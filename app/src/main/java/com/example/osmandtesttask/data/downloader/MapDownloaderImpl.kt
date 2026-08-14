package com.example.osmandtesttask.data.downloader

import android.os.SystemClock
import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.common.capitalizeFirstChar
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.domain.downloader.DownloadError
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.IMapDownloader
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
import kotlinx.coroutines.supervisorScope
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
    private val localeProvider: () -> Locale,
    private val onEnqueue: () -> Unit
) : IMapDownloader {

    private var downloadJob: Job? = null

    private var queueChannel = createChannel()
    private var queueJob: Job? = null

    private val enqueuedList = Collections.synchronizedList(mutableListOf<DownloadTask>())

    private val _state = MutableStateFlow<DownloaderState>(DownloaderState.Empty)
    override val state: StateFlow<DownloaderState> = _state.asStateFlow()

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

    private val _errors = MutableSharedFlow<DownloadError>()
    override val errors = _errors.asSharedFlow()

    init {
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
            val downloadFilename = composeDownloadFileName(download.downloadName)
            val destination = File(outputDir, downloadFilename)
            if (!validateFileLocation(destination, outputDir)) {
                throw SecurityException("Path traversal check failed: $downloadFilename")
            }

            val response = service.downloadMap(downloadFilename)
            if (response.isSuccessful) {

                Logs.d("Downloader", "received success response for ${download.downloadName}")
                val body = response.body() ?: throw HttpException(response)
                saveFileWithProgress(download, body, destination)
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
        destination: File
    ) {
        body.toFileWithProgress(destination).collect { downloadState ->
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

    private fun composeDownloadFileName(downloadName: String): String =
        (downloadName + "_2.obf.zip").capitalizeFirstChar(Locale.ENGLISH)
}

private class ActiveDownloadCancelledException : CancellationException("Cancelled by request")