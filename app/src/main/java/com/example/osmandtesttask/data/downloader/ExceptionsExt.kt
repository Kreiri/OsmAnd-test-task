package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.data.util.toAppError
import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.MapDownloadError
import com.example.osmandtesttask.domain.errors.AppError
import kotlin.coroutines.cancellation.CancellationException

class ActiveDownloadUserCancellationException : CancellationException("Cancelled by request")

fun Throwable.toDownloadError(downloadTask: DownloadTask): MapDownloadError {
    return when (val appError = this.toAppError()) {
        is AppError.ServerError -> MapDownloadError.ServerError(downloadTask, appError.code)
        AppError.NoInternet -> MapDownloadError.NoInternet(downloadTask)
        AppError.InsufficientStorage -> MapDownloadError.InsufficientStorage(downloadTask)
        else -> {
            if (appError is AppError.Unknown && appError.cause is ActiveDownloadUserCancellationException) {
                MapDownloadError.CancelledByUser(downloadTask)
            } else MapDownloadError.Unknown(downloadTask, this)
        }
    }
}