package com.example.osmandtesttask.di

import com.example.osmandtesttask.data.downloader.MapDownloaderImpl
import com.example.osmandtesttask.domain.AssetProvider
import com.example.osmandtesttask.domain.LocaleProvider
import com.example.osmandtesttask.domain.downloader.IMapDownloader
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

    single<CoroutineScope> { CoroutineScope(Dispatchers.Main + SupervisorJob()) }

    single<File>(named(FolderQualifier.MAPS_DOWNLOAD)) {
        File(androidContext().filesDir, "maps")
    }

    single<NotificationUtil> { NotificationUtil(androidContext()) }

    single<LocaleProvider>(named(ProviderQualifier.LOCALE)) {
        { androidContext().getCurrentLocale() }
    }

    single<AssetProvider> {
        { fileName ->
            val context = androidContext()
            context.assets.open(fileName)
        }
    }

    single<IMapDownloader> {
        MapDownloaderImpl(
            service = get(),
            outputDir = get(named(FolderQualifier.MAPS_DOWNLOAD)),
            externalScope = get(),
            localeProvider = get(named(ProviderQualifier.LOCALE)),
            onEnqueue = {
                MapDownloadService.start(androidContext())
            }
        )
    }
}