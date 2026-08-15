package com.example.osmandtesttask.data.util

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