package com.example.osmandtesttask.domain.downloader

import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.flow.Flow

interface IMapDownloader {
    fun enqueueDownload(region: Region)
    fun cancelDownload(region: Region)
    fun cancelAll()
    val state: Flow<DownloaderState>
}

sealed interface DownloaderState {
    object Empty : DownloaderState
//    object Initial: DownloaderState
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
    data class Progress(val bytesRead: Long, val totalBytes: Long) : DownloadState
}