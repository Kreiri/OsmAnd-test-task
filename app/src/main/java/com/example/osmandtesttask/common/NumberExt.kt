package com.example.osmandtesttask.common

fun Long.asFractionOf(another: Long): Float {
    if (this == 0L) return 0f
    if (this == another) return 1f
    return this.toFloat() / another
}