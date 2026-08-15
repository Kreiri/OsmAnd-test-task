package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.domain.downloader.DownloadTask
import com.example.osmandtesttask.domain.downloader.MapDownloadError
import com.example.osmandtesttask.domain.errors.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

fun Throwable.toAppError(): AppError {
    return when (this) {
        is UnknownHostException, is ConnectException -> AppError.NoInternet
        is IOException -> {
            if (this.message?.contains("ENOSPC", ignoreCase = true) == true) {
                AppError.InsufficientStorage
            } else {
                AppError.Unknown(this)
            }
        }

        is HttpException -> AppError.ServerError(this.code())
        else -> AppError.Unknown(this)
    }
}

fun Throwable.toDownloadError(downloadTask: DownloadTask): MapDownloadError {
    return when (val appError = this.toAppError()) {
        is AppError.ServerError -> MapDownloadError.ServerError(downloadTask, appError.code)
        AppError.NoInternet -> MapDownloadError.NoInternet(downloadTask)
        AppError.InsufficientStorage -> MapDownloadError.InsufficientStorage(downloadTask)
        else -> MapDownloadError.Unknown(downloadTask, this)
    }
}