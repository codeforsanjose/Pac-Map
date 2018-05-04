package com.codeforsanjose.maps.pacmap.core

import android.app.Application
import com.codeforsanjose.maps.pacmap.BuildConfig
import timber.log.Timber

class PacMapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}