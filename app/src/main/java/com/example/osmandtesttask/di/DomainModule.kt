package com.example.osmandtesttask.di

import com.example.osmandtesttask.domain.MapsManager
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val domainModule = module {
    single<MapsManager>()
}