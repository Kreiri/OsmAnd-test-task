package com.example.osmandtesttask.data.storage

import android.os.Environment
import android.os.StatFs
import com.example.osmandtesttask.common.Logger
import com.example.osmandtesttask.domain.storage.StorageInfo
import com.example.osmandtesttask.domain.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class StorageManagerImpl(
    private val scope: CoroutineScope
) : StorageManager {
    private val _storageInfo = MutableStateFlow(StorageInfo.unknown())
    override val storageInfo = _storageInfo.asStateFlow()

    override fun update() {
        scope.launch(Dispatchers.IO) {
            val directory = Environment.getDataDirectory()
            val stats = StatFs(directory.absolutePath)
            val totalBytes = stats.totalBytes
            val availableBytes = stats.availableBytes
            val info = StorageInfo(totalBytes, availableBytes)
            Logger.d(
                "storage",
                "on ${directory.path} filesystem: total ~${totalBytes / (1024 * 1024)} MB, available ~${availableBytes / (1024 * 1024)} MB"
            )
            _storageInfo.value = info
        }
    }

    override fun hasAvailable(bytes: Long): Boolean {
        if (bytes <= 0) return true
        val info = _storageInfo.value
        if (info.availableBytes == -1L) return true
        return info.availableBytes >= bytes
    }

    override fun availableBytes(): Long {
        return _storageInfo.value.availableBytes
    }

    override fun totalBytes(): Long {
        return _storageInfo.value.totalBytes
    }
}