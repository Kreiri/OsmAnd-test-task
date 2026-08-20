package com.example.osmandtesttask.data.util

import android.system.ErrnoException
import android.system.OsConstants
import com.example.osmandtesttask.data.downloader.InsufficientStorageException
import com.example.osmandtesttask.domain.errors.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.Locale

fun Throwable.toAppError(): AppError {
    return when (this) {
        is InsufficientStorageException -> AppError.InsufficientStorage(requiredBytes, availableBytes)
        is UnknownHostException, is ConnectException -> AppError.NoInternet
        is HttpException -> AppError.ServerError(this.code())
        else -> AppError.Unknown(this)
    }
}

fun Throwable.isInsufficientStorage(): Boolean {
    if (this !is IOException) return false
    val cause = this.cause
    if (cause != null && cause is ErrnoException) {
        return cause.errno == OsConstants.ENOSPC
    }
    val msg = this.message?.lowercase(Locale.ENGLISH) ?: return false
    if (msg.isEmpty()) return false
    return msg.contains("enospc")
            || msg.contains("disk full")
            || msg.contains("no space left")
}