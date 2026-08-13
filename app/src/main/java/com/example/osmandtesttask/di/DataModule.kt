package com.example.osmandtesttask.di

import com.example.osmandtesttask.BuildConfig
import com.example.osmandtesttask.data.api.MapDownloadApiService
import com.example.osmandtesttask.data.repository.DummyRepository
import com.example.osmandtesttask.domain.repository.IRegionRepository
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val mapDownloadsModule = module {
    single<OkHttpClient>(named(OkHttpQualifier.MAPS_DOWNLOAD)) {
        OkHttpClient.Builder()
            .build()
    }
    single(named(BaseUrlQualifier.MAPS_DOWNLOAD)) {
        BuildConfig.MAPS_DOWNLOAD_BASE_URL
    }
    single<Retrofit>(named(RetrofitQualifier.MAPS_DOWNLOAD)) {
        Retrofit.Builder()
            .client(get(named(OkHttpQualifier.MAPS_DOWNLOAD)))
            .baseUrl(get<String>(named(BaseUrlQualifier.MAPS_DOWNLOAD)))
            .build()
    }
    single<MapDownloadApiService> {
        val retrofit = get<Retrofit>(named(RetrofitQualifier.MAPS_DOWNLOAD))
        retrofit.create(MapDownloadApiService::class.java)
    }
}

val networkModule = module {
    includes(mapDownloadsModule)
}

val dataModule = module {
    includes(networkModule)
    single<IRegionRepository> {
        DummyRepository()
    }
}
