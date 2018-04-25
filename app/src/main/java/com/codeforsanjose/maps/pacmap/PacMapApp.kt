package com.codeforsanjose.maps.pacmap

import android.app.Application
import timber.log.Timber

class PacMapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}