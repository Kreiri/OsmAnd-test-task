package com.example.osmandtesttask.domain

import com.example.osmandtesttask.domain.downloader.IMapDownloader
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.repository.IRegionRepository

class MapsManager(
    private val regionsRepo: IRegionRepository,
    private val mapsDownloader: IMapDownloader
) {
    private var regions: RegionsList? = null

    suspend fun loadRegions(): Result<RegionsList> {
        val fetched = regionsRepo.getRegionsList()
        fetched.onFailure {
            regions = null
        }.onSuccess {
            regions = it
        }
        return fetched
    }

    val downloadsState = mapsDownloader.state

    fun getRegions(): RegionsList?{
        return regions
    }

    fun requestDownloadMap(region: Region) {
        mapsDownloader.enqueueDownload(region)
    }
}