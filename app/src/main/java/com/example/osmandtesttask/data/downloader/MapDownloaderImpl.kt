package com.example.osmandtesttask.data.downloader

import android.os.SystemClock
import com.example.osmandtesttask.common.Logger
import com.example.osmandtesttask.common.capitalizeFirstChar
import com.example.osmandtesttask.common.removeAndReturnIf
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.data.util.isInsufficientStorage
import com.example.osmandtesttask.domain.LocaleProvider
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.DownloadsIndex
import com.example.osmandtesttask.domain.downloader.MapDownloadError
import com.example.osmandtesttask.domain.downloader.MapDownloader
import com.example.osmandtesttask.domain.downloader.matches
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
    private val storageManager: StorageManager,
    private val localeProvider: LocaleProvider,
    private val onEnqueue: () -> Unit
) : MapDownloader {

    private var downloadJob: Job? = null

    private val queueLock = Any()
    private val queue = Collections.synchronizedList(mutableListOf<DownloadTask>())
    private var signalChannel = createSignalChannel()
    private var queueJob: Job? = null


    private val _state = MutableStateFlow<DownloaderState>(DownloaderState.Empty)
    override val state: StateFlow<DownloaderState> = _state.asStateFlow()

    private val _downloadsIndex = MutableStateFlow(DownloadsIndex.blank())
    override val downloadsIndex = _downloadsIndex.asStateFlow()

    private val lastThrottleFlowEmitTime = AtomicLong(0L)
    private val throttleInterval = 500L
    override val throttledState = _state.filter { state ->
        val time = SystemClock.elapsedRealtime()
        val accept = when (state) {
            DownloaderState.Empty -> {
                true
            }

            is DownloaderState.Waiting -> {
                true
            }

            is DownloaderState.Processing -> {
                state.downloadState !is DownloadState.Progress
                        || time - lastThrottleFlowEmitTime.get() >= throttleInterval
            }
        }
        if (accept) lastThrottleFlowEmitTime.set(time)
        accept
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

    override fun enqueueDownload(region: Region) {
        doEnqueueDownload(region)
    }

    override fun cancelDownload(region: Region) {
        doCancelDownload(region)
    }

    override fun cancelAll() {
        cancelQueue()
        downloadJob?.cancel()
        queue.clear()
        _state.value = DownloaderState.Empty
        launchQueue()
    }


    private fun createSignalChannel() = Channel<Unit>(Channel.UNLIMITED)

    private fun launchQueue() {
        queueJob = externalScope.launch {
            try {
                for (signal: Unit in signalChannel) {
                    val downloadTask = synchronized(queueLock) {
                        queue.removeFirstOrNull()
                    }
                    if (downloadTask == null) continue
                    startDownloadJob(downloadTask)
                }
            } catch (e: CancellationException) {
                Logger.d("Downloader", "queue job : CancellationException")
            }
        }
    }

    private fun cancelQueue() {
        signalChannel.close()
        queueJob?.cancel()
        queueJob = null
        signalChannel = createSignalChannel()
    }

    private suspend fun startDownloadJob(download: DownloadTask) {
        Logger.d("Downloader", "starting job for ${download.downloadName}")
        updateState(activeDownload = download)
        try {
            withContext(Dispatchers.IO) {
                downloadJob = currentCoroutineContext()[Job]
                processDownload(download)
            }
        } catch (e: Throwable) {
            Logger.d("Downloader", "download error: ${e.message}")
            if (e is ActiveDownloadUserCancellationException) {
                val error = e.toDownloadError(download)
                _errors.emit(error)
            } else if (e is CancellationException) {
                throw e
            } else {
                val error = e.toDownloadError(download)
                _errors.emit(error)
            }
        } finally {
            downloadJob = null
            updateState(activeDownload = null)
            signalChannel.trySend(Unit)
            Logger.d("downloader", "download job ended: ${download.downloadName}")
        }
    }

    private suspend fun processDownload(download: DownloadTask) {
        Logger.d("Downloader", "launching download ${download.downloadName}")
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
                Logger.d("Downloader", "received success response for ${download.downloadName}")
                val body = response.body() ?: throw HttpException(response)
                val contentLength = body.contentLength()
                if (!storageManager.hasAvailable(contentLength))
                    throw InsufficientStorageException(
                        contentLength,
                        storageManager.availableBytes()
                    )
                try {
                    saveFileWithProgress(download, body, destination, downloadDestination)
                } catch (e: Throwable) {
                    if (e.isInsufficientStorage()) {
                        throw InsufficientStorageException(
                            contentLength,
                            storageManager.availableBytes()
                        )
                    } else {
                        throw e
                    }
                }
                storageManager.update()
            } else {
                storageManager.update()
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
        downloadDestination: File,
    ) {
        try {
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
        } catch (e: ActiveDownloadUserCancellationException) {
            withContext(NonCancellable) {
                updateState(download, DownloadState.Cancelled)
            }
            throw e
        }
    }

    private fun getActiveDownload(): DownloadTask? {
        return (_state.value as? DownloaderState.Processing)?.activeDownload
    }

    private fun updateState(activeDownload: DownloadTask?, downloadState: DownloadState? = null) {
        val currentEnqueued = synchronized(queueLock) { queue.toList() }
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

    private fun doEnqueueDownload(region: Region) {
        val locale = localeProvider.invoke()
        val displayName = region.getLocalizedName(locale.language)

        val addedDownload = synchronized(queueLock) {
            if (queue.any { it.matches(region) }) null
            else {
                val download = DownloadTask(displayName, region.downloadName)
                queue.add(download)
                download
            }
        }
        if (addedDownload != null) {
            updateState(activeDownload = getActiveDownload())
            signalChannel.trySend(Unit)
            onEnqueue.invoke()
        }
    }

    private fun doCancelDownload(region: Region) {
        if (getActiveDownload()?.matches(region) == true) {
            downloadJob?.cancel(ActiveDownloadUserCancellationException())
            downloadJob = null
            Logger.d("downloader", "canceled active download for ${region.downloadName}")
        }
        val removed = synchronized(queueLock) {
            queue.removeAndReturnIf { it.matches(region) }
        }
        if (removed.isNotEmpty()) {
            val task = removed.first()
            _errors.tryEmit(MapDownloadError.CancelledByUser(task))
            Logger.d("downloader", "canceled enqueued download for ${task.downloadName}")
        }
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
        } catch (e: Throwable) {
            DownloadsIndex.blank()
        }
    }

    private fun writeIndexToFile(index: DownloadsIndex) {
        try {
            val file = File(outputDir, INDEX_FILE)
            file.parentFile?.mkdirs()
            val json = DownloadsIndexSerializer.serialize(index)
            file.writeText(json)
        } catch (e: Throwable) {
            Logger.d("downloader", "error while saving index to file: ${e.message}")
        }
    }

    companion object {
        private const val FILE_SUFFIX = "_2.obf.zip"
        private const val DOWNLOAD_FILE_SUFFIX = ".part"
        private const val INDEX_FILE = "downloads.index"
    }
}

