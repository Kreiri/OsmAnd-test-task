package com.example.osmandtesttask.data.downloader

import android.os.SystemClock
import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.common.capitalizeFirstChar
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.domain.LocaleProvider
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.DownloadsIndex
import com.example.osmandtesttask.domain.downloader.IMapDownloader
import com.example.osmandtesttask.domain.downloader.MapDownloadError
import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

class MapDownloaderImpl(
    private val service: MapDownloadApiService,
    private val outputDir: File,
    private val externalScope: CoroutineScope,
    private val localeProvider: LocaleProvider,
    private val onEnqueue: () -> Unit
) : IMapDownloader {

    private var downloadJob: Job? = null

    private var queueChannel = createChannel()
    private var queueJob: Job? = null

    private val enqueuedList = Collections.synchronizedList(mutableListOf<DownloadTask>())

    private val _state = MutableStateFlow<DownloaderState>(DownloaderState.Empty)
    override val state: StateFlow<DownloaderState> = _state.asStateFlow()

    private val _downloadsIndex = MutableStateFlow(DownloadsIndex.blank())
    override val downloadsIndex = _downloadsIndex.asStateFlow()

    private val lastThrottleFlowEmitTime = AtomicLong(0L)
    private val throttleInterval = 500L
    override val throttledState = _state.filter { state ->
        if (state == DownloaderState.Empty) {
            true
        } else {
            val time = SystemClock.elapsedRealtime()
            val accept = time - lastThrottleFlowEmitTime.get() >= throttleInterval
            if (accept) lastThrottleFlowEmitTime.set(time)
            accept
        }
    }.stateIn(
        scope = externalScope,
        started = SharingStarted.Eagerly,
        initialValue = _state.value
    )

    private val _errors = MutableSharedFlow<MapDownloadError>()
    override val errors = _errors.asSharedFlow()

    init {
        collectDownloadedFilesInfo()
        launchQueue()
    }

    private fun createChannel() = Channel<DownloadTask>(Channel.UNLIMITED)

    private fun launchQueue() {

        Logs.d("downloader", "launch queue")
        queueChannel = createChannel()
        queueJob = externalScope.launch {
            try {
                for (downloadTask in queueChannel) {
                    Logs.d("Downloader", "getting ${downloadTask.downloadName} from queue")
                    enqueuedList.remove(downloadTask)
                    startDownloadJob(downloadTask)
                }
            } catch (e: CancellationException) {
                Logs.d("Downloader", "queue job : CancellationException")
            }
        }
    }

    private fun cancelQueue() {
        Logs.d("downloader", "cancel queue")
        queueChannel.close()
        queueJob?.cancel()
        queueJob = null
    }

    private suspend fun startDownloadJob(download: DownloadTask) {
        Logs.d("Downloader", "starting job for ${download.downloadName}")
        updateState(activeDownload = download)
        try {
            withContext(Dispatchers.IO) {
                downloadJob = currentCoroutineContext()[Job]
                processDownload(download)
            }
        } catch (e: Exception) {
            Logs.d("Downloader", "download error: ${e.message}")
            if (e is ActiveDownloadCancelledException) {
                // todo: custom handling of user-initiated cancel?
            } else if (e is CancellationException) {
                throw e
            } else {
                val error = e.toDownloadError(download)
                _errors.tryEmit(error)
            }
        } finally {
            downloadJob = null
            updateState(activeDownload = null)
        }
    }

    private suspend fun processDownload(download: DownloadTask) {
        Logs.d("Downloader", "launching download ${download.downloadName}")
        withContext(Dispatchers.IO) {
            val filename = composeFileName(download.downloadName)
            val destination = File(outputDir, filename)
            if (!validateFileLocation(destination, outputDir)) {
                throw SecurityException("Path traversal check failed: $filename")
            }
            val downloadDestination =
                File(outputDir, composeDownloadFileName(download.downloadName))

            val response = service.downloadMap(filename)
            if (response.isSuccessful) {
                Logs.d("Downloader", "received success response for ${download.downloadName}")
                val body = response.body() ?: throw HttpException(response)
                saveFileWithProgress(download, body, destination, downloadDestination)
            } else {
                throw HttpException(response)
            }
        }
    }

    private fun validateFileLocation(file: File, expectedDir: File): Boolean {
        val canonicalFile = file.canonicalPath
        val canonicalFolder = File(expectedDir.canonicalPath + File.separator).canonicalPath
        return canonicalFile.startsWith(canonicalFolder)
    }

    private suspend fun saveFileWithProgress(
        download: DownloadTask,
        body: ResponseBody,
        destination: File,
        downloadDestination: File
    ) {
        body.toFileWithProgress(downloadDestination).collect { downloadState ->
            if (downloadState == DownloadState.Finished) {
                if (destination.exists()) destination.delete()
                destination.parentFile?.mkdirs()
                downloadDestination.renameTo(destination)
                val info = DownloadedFileInfo(destination.name, download.downloadName)
                val oldIndex = _downloadsIndex.value
                val newIndex = oldIndex.copy(downloadedFiles = oldIndex.downloadedFiles + info)
                _downloadsIndex.value = newIndex
                writeIndexToFile(newIndex)
            }
            updateState(download, downloadState)
        }
    }

    private fun getActiveDownload(): DownloadTask? {
        return (_state.value as? DownloaderState.Processing)?.activeDownload
    }

    private fun updateState(activeDownload: DownloadTask?, downloadState: DownloadState? = null) {
        val currentEnqueued = enqueuedList.toList()
        val newState = if (activeDownload == null) {
            if (currentEnqueued.isEmpty()) DownloaderState.Empty
            else DownloaderState.Waiting(currentEnqueued)
        } else if (downloadState != null) {
            DownloaderState.Processing(
                activeDownload, downloadState, currentEnqueued
            )
        } else _state.value
        _state.value = newState
    }

    override fun enqueueDownload(region: Region) {
        val locale = localeProvider.invoke()
        val displayName = region.getLocalizedName(locale.language)
        val download = DownloadTask(displayName, region.downloadName)
        if (enqueuedList.contains(download)) return

        val queue = queueChannel ?: return

        val sent = queue.trySend(download)
        if (sent.isSuccess) {
            Logs.d("Downloader", "enqueued ${download.downloadName}")
            enqueuedList.add(download)
            updateState(activeDownload = getActiveDownload())
            onEnqueue.invoke()
        } else {
            Logs.d("Downloader", "enqueuing ${download.downloadName} failed")
        }
    }

    override fun cancelDownload(region: Region) {
        val downloadName = region.downloadName
        if (getActiveDownload()?.downloadName == downloadName) {
            downloadJob?.cancel(ActiveDownloadCancelledException())
            downloadJob = null
        }
    }

    override fun cancelAll() {
        cancelQueue()
        downloadJob?.cancel()
        enqueuedList.clear()
        _state.value = DownloaderState.Empty
        launchQueue()
    }

    private fun composeFileName(downloadName: String): String =
        downloadName.capitalizeFirstChar(Locale.ENGLISH) + FILE_SUFFIX

    private fun composeDownloadFileName(downloadName: String): String =
        composeFileName(downloadName) + DOWNLOAD_FILE_SUFFIX


    private fun collectDownloadedFilesInfo() {
        externalScope.launch(Dispatchers.IO) {
            val downloadedFileNames = outputDir.listFiles { file ->
                file.name.endsWith(FILE_SUFFIX)
            }?.map {
                it.name
            }?.toSet() ?: emptySet()
            val savedIndex = readIndexFromFile()
            val checkedIndex = checkIndexAgainstRealFiles(savedIndex, downloadedFileNames)
            _downloadsIndex.value = checkedIndex
        }
    }

    private fun checkIndexAgainstRealFiles(
        index: DownloadsIndex,
        existingDownloaded: Set<String>
    ): DownloadsIndex {
        val files = index.downloadedFiles
        val missing = mutableSetOf<DownloadedFileInfo>()
        for (fileInfo in files) {
            if (fileInfo.filename !in existingDownloaded) {
                missing.add(fileInfo)
            }
        }
        val actualFiles = files - missing
        return DownloadsIndex(actualFiles)

    }

    private fun readIndexFromFile(): DownloadsIndex {
        return try {
            val file = File(outputDir, INDEX_FILE)
            val json = file.readText()
            val index = DownloadsIndexSerializer.deserialize(json)
            index ?: DownloadsIndex.blank()
        } catch (e: Exception) {
            DownloadsIndex.blank()
        }
    }

    private fun writeIndexToFile(index: DownloadsIndex) {
        try {
            val file = File(outputDir, INDEX_FILE)
            file.parentFile?.mkdirs()
            val json = DownloadsIndexSerializer.serialize(index)
            file.writeText(json)
        } catch (e: Exception) {
            Logs.d("downloader", "error while saving index to file: ${e.message}")
        }
    }

    companion object {
        private const val FILE_SUFFIX = "_2.obf.zip"
        private const val DOWNLOAD_FILE_SUFFIX = ".part"
        private const val INDEX_FILE = "downloads.index"
    }
}

private class ActiveDownloadCancelledException : CancellationException("Cancelled by request")