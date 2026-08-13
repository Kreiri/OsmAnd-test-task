package com.example.osmandtesttask.di

import com.example.osmandtesttask.data.repository.DummyRepository
import com.example.osmandtesttask.domain.repository.IRegionRepository
import org.koin.dsl.module

val dataModule = module {
    single<IRegionRepository> {
        DummyRepository()
    }
}