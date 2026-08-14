package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.domain.downloader.DownloadError
import com.example.osmandtesttask.domain.downloader.DownloadTask
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

fun Throwable.toDownloadError(downloadTask: DownloadTask): DownloadError {
    return when(this) {
        is UnknownHostException, is ConnectException -> DownloadError.NoInternet(downloadTask)
        is IOException -> {
            if (this.message?.contains("ENOSPC", ignoreCase = true) == true) {
                DownloadError.InsufficientStorage(downloadTask)
            } else {
                DownloadError.Unknown(downloadTask, cause = this)
            }
        }
        is HttpException -> DownloadError.ServerError(downloadTask)
        else -> DownloadError.Unknown(downloadTask, cause = this)
    }
}