package com.example.osmandtesttask.domain.downloader

import com.example.osmandtesttask.domain.errors.AppError
import com.example.osmandtesttask.domain.errors.NetworkError
import com.example.osmandtesttask.domain.errors.StorageError
import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IMapDownloader {
    fun enqueueDownload(region: Region)
    fun cancelDownload(region: Region)
    fun cancelAll()

    val state: StateFlow<DownloaderState>
    val throttledState: StateFlow<DownloaderState>

    val downloadsIndex: StateFlow<DownloadsIndex>

    val errors: Flow<MapDownloadError> }

sealed interface DownloaderState {
    object Empty : DownloaderState
    data class Waiting(val enqueuedDownloads: List<DownloadTask>): DownloaderState
    data class Processing(
        val activeDownload: DownloadTask,
        val downloadState: DownloadState,
        val enqueuedDownloads: List<DownloadTask>
    ) : DownloaderState
}

data class DownloadTask(
    val displayName: String, val downloadName: String
)

sealed interface DownloadState {
    object Finished : DownloadState
    object Cancelled: DownloadState
    data class Error(val cause: Throwable): DownloadState
    data class Progress(val bytesRead: Long, val totalBytes: Long) : DownloadState {
        /**
         * progress as expressed with float values from 0 to 1. 1 is considered 100%
         */
        val progress: Float =
            if (bytesRead <= 0L) 0f
            else if (bytesRead == totalBytes) 1f
            else (bytesRead.toFloat() / totalBytes)
    }
}

sealed interface MapDownloadError: AppError {
    val download: DownloadTask
    data class NoInternet(override val download: DownloadTask): MapDownloadError, NetworkError
    data class ServerError(override val download: DownloadTask, val code: Int): MapDownloadError, NetworkError
    data class InsufficientStorage(override val download: DownloadTask): MapDownloadError, StorageError
    data class Unknown(override val download: DownloadTask, val cause: Throwable): MapDownloadError
}

data class DownloadsIndex(val downloadedFiles: Set<DownloadedFileInfo>) {
    companion object {
        fun blank() = DownloadsIndex(emptySet())
    }
}
data class DownloadedFileInfo(val filename: String, val downloadName: String)