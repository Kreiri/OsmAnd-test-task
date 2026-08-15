package com.example.osmandtesttask.di

import com.example.osmandtesttask.BuildConfig
import com.example.osmandtesttask.common.MapDownloadQualifier
import com.example.osmandtesttask.common.ProviderQualifiers
import com.example.osmandtesttask.common.ScopeQualifier
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.data.repository.LocalRegionsRepository
import com.example.osmandtesttask.data.storage.StorageManagerImpl
import com.example.osmandtesttask.data.util.retrofit.RemoteRegionsListConverter
import com.example.osmandtesttask.domain.repository.RegionRepository
import com.example.osmandtesttask.domain.storage.StorageManager
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val mapDownloadsModule = module {
    single<OkHttpClient>(named(MapDownloadQualifier.HTTP_CLIENT)) {
        OkHttpClient.Builder()
            .build()
    }
    single(named(MapDownloadQualifier.BASE_URL)) {
        BuildConfig.MAPS_DOWNLOAD_BASE_URL
    }
    single<Retrofit>(named(MapDownloadQualifier.RETROFIT)) {
        Retrofit.Builder()
            .client(get(named(MapDownloadQualifier.HTTP_CLIENT)))
            .baseUrl(get<String>(named(MapDownloadQualifier.BASE_URL)))
            .addConverterFactory(RemoteRegionsListConverter.Factory.create())
            .build()
    }
    single<MapDownloadApiService> {
        val retrofit = get<Retrofit>(named(MapDownloadQualifier.RETROFIT))
        retrofit.create(MapDownloadApiService::class.java)
    }
}

val dataModule = module {
    includes(mapDownloadsModule)
    single<RegionRepository> {
        LocalRegionsRepository(
            assetProvider = get(named(ProviderQualifiers.ASSET)),
            regionsFilePath = "regions.xml"
        )
    }
    single<StorageManager> {
        StorageManagerImpl(scope = get(named(ScopeQualifier.APP_SCOPE)))
    }
}
