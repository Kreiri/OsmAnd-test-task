package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.common.capitalizeFirstChar
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.IMapDownloader
import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.util.Collections
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class MapDownloaderImpl(
    private val service: MapDownloadApiService,
    private val outputDir: File,
    private val externalScope: CoroutineScope,
    private val localeProvider: () -> Locale,
    private val onEnqueue: () -> Unit
) : IMapDownloader {

    private val queue = Channel<DownloadTask>(Channel.UNLIMITED)
    private var currentJob: Job? = null

    private val enqueuedList = Collections.synchronizedList(mutableListOf<DownloadTask>())

    private val _state = MutableStateFlow<DownloaderState>(DownloaderState.Empty)
    override val state: StateFlow<DownloaderState> = _state.asStateFlow()

    init {
        externalScope.launch {
            for (downloadName in queue) {
                enqueuedList.remove(downloadName)
                startDownloadJob(downloadName)
            }
        }
    }

    private suspend fun startDownloadJob(download: DownloadTask) {
        updateState(activeDownload = download)
        try {
            withContext(Dispatchers.IO) {
                currentJob = currentCoroutineContext()[Job]
                processDownload(download)
            }
        } catch (e: Exception) {
            Logs.d("download error: ${e.message}")
            if (e is ActiveDownloadCancelledException) {
                // todo: custom handling of user-initiated cancel?
            }
            if (e is CancellationException) throw e
        } finally {
            currentJob = null
            updateState(activeDownload = null)
        }
    }

    private suspend fun processDownload(download: DownloadTask) {
        withContext(Dispatchers.IO) {
            val downloadFilename = composeDownloadFileName(download.downloadName)
            val destination = File(outputDir, downloadFilename)
            if (!validateFileLocation(destination, outputDir)) {
                // todo: implement individual download failure notification
                return@withContext
            }

            val response = service.downloadMap(downloadFilename)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    saveFileWithProgress(download, body, destination)
                }
            } else {
                // todo: implement individual download failure notification
            }
        }
    }

    private fun validateFileLocation(file: File, expectedDir: File): Boolean {
        val canonicalFile = file.canonicalPath
        val canonicalFolder = expectedDir.canonicalPath
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
        enqueuedList.add(download)
        queue.trySend(download)
        updateState(activeDownload = getActiveDownload())
        onEnqueue.invoke()
    }

    override fun cancelDownload(region: Region) {
        val downloadName = region.downloadName
        if (getActiveDownload()?.downloadName == downloadName) {
            currentJob?.cancel(ActiveDownloadCancelledException())
        }
    }

    override fun cancelAll() {
        queue.close()
        currentJob?.cancel()
        enqueuedList.clear()
        _state.value = DownloaderState.Empty
    }

    private fun composeDownloadFileName(downloadName: String): String =
        (downloadName + "_2.obf.zip").capitalizeFirstChar(Locale.ENGLISH)
}

private class ActiveDownloadCancelledException : CancellationException("Cancelled by request")