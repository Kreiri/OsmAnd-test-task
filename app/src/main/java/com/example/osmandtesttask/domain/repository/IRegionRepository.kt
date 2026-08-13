package com.example.osmandtesttask.domain.repository

import com.example.osmandtesttask.domain.models.RegionsList

interface IRegionRepository {
    suspend fun getRegionsList(): Result<RegionsList>
}