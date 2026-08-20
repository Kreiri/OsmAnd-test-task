package com.example.osmandtesttask.domain.storage

import kotlinx.coroutines.flow.StateFlow

interface StorageManager {
    val storageInfo: StateFlow<StorageInfo>

    fun update()
    fun hasAvailable(bytes: Long): Boolean
    fun availableBytes(): Long
    fun totalBytes(): Long
}

data class StorageInfo(val totalBytes: Long, val availableBytes: Long) {
    companion object {
        fun unknown() = StorageInfo(-1, -1)
    }
}