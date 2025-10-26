package com.example.baytro

import android.app.Application
import com.example.baytro.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class BayTroApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@BayTroApp)
            modules(
                coreModule,
                dataModule,
                cloudFunctionsModule,
                externalServicesModule,
                authViewModelModule,
                splashViewModelModule,
                buildingViewModelModule,
                roomViewModelModule,
                contractViewModelModule,
                serviceViewModelModule,
                requestViewModelModule,
                billingViewModelModule,
                meterReadingViewModelModule,
                dashboardViewModelModule,
                tenantViewModelModule,
                utilityViewModelModule
            )
        }
    }
}