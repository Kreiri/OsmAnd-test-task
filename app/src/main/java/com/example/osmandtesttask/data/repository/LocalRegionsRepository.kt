package com.example.osmandtesttask.data.repository

import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.data.downloader.toAppError
import com.example.osmandtesttask.data.remote.parsers.RemoteRegionsListParser
import com.example.osmandtesttask.domain.AssetProvider
import com.example.osmandtesttask.domain.models.AppResult
import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.repository.IRegionRepository

class LocalRegionsRepository(
    private val assetProvider: AssetProvider,
    private val regionsFilePath: String
): IRegionRepository {
    private val parser = RemoteRegionsListParser()
    private var data: RegionsList? = null

    override suspend fun getRegionsList(): AppResult<RegionsList> {
        val cached = data
        if (cached != null) return AppResult.success(cached)

        return try {
            val stream = assetProvider.invoke(regionsFilePath)
            val data = stream.use { parser.parse(it) }
            AppResult.success(data.toLocal())
        } catch (e: Throwable) {
            Logs.d("regions", "regions list load failed: ${e.message}")
            val appError = e.toAppError()
            AppResult.failure(appError)
        }
    }
}