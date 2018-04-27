package com.codeforsanjose.maps.pacmap.zone

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber


class ZoneManager {
    companion object {
        const val SERVICE_URL = "https://obscure-river-21166.herokuapp.com/"

        fun fetchZones(): Observable<FeatureCollection> {
            val loggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message ->
                Timber.d(message)
            })

            loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
            val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build()

            val retrofit = Retrofit.Builder()
                    .client(client)
                    .baseUrl(SERVICE_URL)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            val polyService: PolygonService = retrofit.create(PolygonService::class.java)

            return polyService.getFeatureCollection()
        }
    }
}