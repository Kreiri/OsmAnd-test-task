package com.example.osmandtesttask.domain.models

import com.example.osmandtesttask.domain.errors.AppError

class AppResult<T>
private constructor(val value: Any?) {

    data class Failure(val error: AppError)

    val isSuccess = value !is Failure
    val isFailure = value is Failure

    override fun toString(): String =
        when (value) {
            is Failure -> value.toString()
            else -> "Success($value)"
        }

    companion object {
        fun <T> success(value: T) = AppResult<T>(value)
        fun <T> failure(error: AppError) = AppResult<T>(Failure(error))
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> AppResult<T>.errorOrNull(): AppError? =
    if (isSuccess) null else (value as AppResult.Failure).error

@Suppress("UNCHECKED_CAST")
fun <T> AppResult<T>.getOrNull(): T? = if (isSuccess) value as T else null

@Suppress("UNCHECKED_CAST")
fun <T> AppResult<T>.getOrThrow(): T =
    if (isSuccess) value as T
    else throw IllegalStateException("Expected Success but got Failure: cause ${this.errorOrNull()}")

fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (isFailure) {
        errorOrNull()?.let { block.invoke(it) }
    }
    return this
}

fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (isSuccess) {
        getOrNull()?.let { block.invoke(it) }
    }
    return this
}