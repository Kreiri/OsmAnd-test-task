package com.example.osmandtesttask.domain.repository

import com.example.osmandtesttask.domain.models.AppResult
import com.example.osmandtesttask.domain.models.RegionsList

interface RegionRepository {
    suspend fun getRegionsList(): AppResult<RegionsList>
}