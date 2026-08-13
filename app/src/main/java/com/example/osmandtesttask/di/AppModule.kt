package com.example.osmandtesttask.di

import com.example.osmandtesttask.ui.common.navigation.NavigationViewModel
import com.example.osmandtesttask.ui.screens.maps.download.list.MapsListViewModel
import com.example.osmandtesttask.ui.screens.maps.download.overview.MapsOverviewViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    viewModel<NavigationViewModel>()
    viewModel<MapsListViewModel>()
    viewModel<MapsOverviewViewModel>()
}