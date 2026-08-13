package com.example.osmandtesttask.domain

import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.repository.IRegionRepository

class MapsManager(
    private val regionsRepo: IRegionRepository
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

    fun getRegions(): RegionsList?{
        return regions
    }
}