package com.example.osmandtesttask.data.storage

import android.os.Environment
import android.os.StatFs
import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.domain.storage.StorageManager
import com.example.osmandtesttask.domain.storage.StorageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class StorageManagerImpl(
    private val scope: CoroutineScope
): StorageManager {
    private val _storageInfo = MutableStateFlow(StorageInfo.unknown())
    override val storageInfo = _storageInfo.asStateFlow()

    override fun update() {
        scope.launch(Dispatchers.IO) {
            val directory = Environment.getDataDirectory()
            val stats = StatFs(directory.absolutePath)
            val totalBytes = stats.totalBytes
            val availableBytes = stats.availableBytes
            val info = StorageInfo(totalBytes, availableBytes)
            Logs.d("storage", "on ${directory.path} filesystem: total ~${totalBytes/1000_000} MB, available ~${availableBytes/1000_000} MB")
            _storageInfo.value = info
        }
    }
}