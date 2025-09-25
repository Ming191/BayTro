package com.example.baytro

import android.app.Application
import com.example.baytro.di.appModule
import com.example.baytro.di.authModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class BayTroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-wide resources here if needed

        startKoin {
            androidLogger()
            androidContext(this@BayTroApp)
            modules(appModule, authModule)
        }
    }
}