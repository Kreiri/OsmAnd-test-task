package com.example.osmandtesttask.domain.storage

import kotlinx.coroutines.flow.StateFlow

interface StorageManager {
    val storageInfo: StateFlow<StorageInfo>

    fun update()
}

data class StorageInfo(val totalBytes: Long, val availableBytes: Long) {
    companion object {
        fun unknown() = StorageInfo(-1, -1)
    }
}