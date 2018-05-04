package com.codeforsanjose.maps.pacmap.zone

import com.codeforsanjose.maps.pacmap.core.StreamUtils.Companion.writeResponseBodyToAssets
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber


class ZoneManager {
    companion object {
        const val SERVICE_URL = "https://pac-map.herokuapp.com/"
        const val GEOJSON_FILENAME = "zones.geojson"

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

        fun MarkZoneComplete(id: String): Completable {
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
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            val polyService: PolygonService = retrofit.create(PolygonService::class.java)

            return polyService.markComplete(id, System.currentTimeMillis())
        }

        fun dlZones() {
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
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            val polyService: PolygonService = retrofit.create(PolygonService::class.java)

            Observable.create<ResponseBody> { subscriber ->
                val response = polyService.downloadFeatureCollection().execute()
                if (response.isSuccessful) {
                    Timber.d("server contacted and has file")

                    val writtenToDisk = writeResponseBodyToAssets(response.body())

                    Timber.d("file download was a success? $writtenToDisk")
                } else {
                    Timber.d("server contact failed")
                    subscriber.onError(Throwable(response.message()))
                }
            }.subscribeOn(Schedulers.io()).subscribe({ result ->
                Timber.d("Finished downloading the file!")
            }, { error ->
                Timber.e(error)
            })
        }
    }
}