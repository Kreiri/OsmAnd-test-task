package com.example.osmandtesttask.domain.errors

sealed class AppError(cause: Exception): Exception(cause) {
    class General(cause: Exception): AppError(cause)
}