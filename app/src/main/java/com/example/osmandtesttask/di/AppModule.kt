package com.example.osmandtesttask.di

import com.example.osmandtesttask.common.MapDownloadQualifier
import com.example.osmandtesttask.common.ProviderQualifiers
import com.example.osmandtesttask.common.ScopeQualifier
import com.example.osmandtesttask.data.downloader.MapDownloaderImpl
import com.example.osmandtesttask.domain.AssetProvider
import com.example.osmandtesttask.domain.LocaleProvider
import com.example.osmandtesttask.domain.downloader.MapDownloader
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale
import com.example.osmandtesttask.ui.common.navigation.NavigationViewModel
import com.example.osmandtesttask.ui.common.notifications.NotificationUtil
import com.example.osmandtesttask.ui.screens.maps.download.list.MapsListViewModel
import com.example.osmandtesttask.ui.screens.maps.download.overview.MapsOverviewViewModel
import com.example.osmandtesttask.ui.services.MapDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel
import java.io.File

val appModule = module {
    viewModel<NavigationViewModel>()
    viewModel<MapsListViewModel>()
    viewModel<MapsOverviewViewModel>()

    single<CoroutineScope>(named(ScopeQualifier.APP_SCOPE)) { CoroutineScope(Dispatchers.Main + SupervisorJob()) }

    single<File>(named(MapDownloadQualifier.FOLDER)) {
        File(androidContext().filesDir, "maps")
    }

    single<NotificationUtil> { NotificationUtil(androidContext()) }

    single<LocaleProvider>(named(ProviderQualifiers.LOCALE)) {
        { androidContext().getCurrentLocale() }
    }

    single<AssetProvider>(named(ProviderQualifiers.ASSET)) {
        { fileName ->
            val context = androidContext()
            context.assets.open(fileName)
        }
    }

    single<MapDownloader> {
        MapDownloaderImpl(
            service = get(),
            outputDir = get(named(MapDownloadQualifier.FOLDER)),
            externalScope = get(named(ScopeQualifier.APP_SCOPE)),
            storageManager = get(),
            localeProvider = get(named(ProviderQualifiers.LOCALE)),
            onEnqueue = {
                MapDownloadService.start(androidContext())
            }
        )
    }
}