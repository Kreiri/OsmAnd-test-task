package com.example.osmandtesttask.domain.errors


interface AppError {
    class Unknown(val cause: Throwable): AppError
    object NoInternet: AppError, NetworkError
    class ServerError(val code: Int): AppError, NetworkError
    object InsufficientStorage: AppError, StorageError
}
interface NetworkError : AppError
interface StorageError : AppError