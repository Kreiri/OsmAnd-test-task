package com.example.osmandtesttask.domain

import com.example.osmandtesttask.domain.downloader.MapDownloader
import com.example.osmandtesttask.domain.models.AppResult
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.models.onFailure
import com.example.osmandtesttask.domain.models.onSuccess
import com.example.osmandtesttask.domain.repository.RegionRepository
import com.example.osmandtesttask.domain.storage.StorageManager

class MapsManager(
    private val regionsRepo: RegionRepository,
    private val mapsDownloader: MapDownloader,
    private val storageManager: StorageManager
) {
    private var regions: RegionsList? = null

    init {
        refreshStorageInfo()
    }

    suspend fun loadRegions(): AppResult<RegionsList> {
        val fetched = regionsRepo.getRegionsList()
        fetched.onFailure {
            regions = null
        }.onSuccess {
            regions = it
        }
        return fetched
    }
    val downloadsState = mapsDownloader.throttledState
    val downloadErrors = mapsDownloader.errors
    val downloadsIndex = mapsDownloader.downloadsIndex
    val storageInfo = storageManager.storageInfo

    fun refreshStorageInfo() {
        storageManager.update()
    }

    fun getRegions(): RegionsList?{
        return regions
    }

    fun requestDownloadMap(region: Region) {
        mapsDownloader.enqueueDownload(region)
    }

    fun cancelDownload(region: Region) {
        mapsDownloader.cancelDownload(region)
    }
}