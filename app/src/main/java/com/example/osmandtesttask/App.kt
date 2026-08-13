package com.example.osmandtesttask

import android.app.Application
import com.example.osmandtesttask.di.appModule
import com.example.osmandtesttask.di.dataModule
import com.example.osmandtesttask.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(domainModule, dataModule, appModule)
        }
    }
}