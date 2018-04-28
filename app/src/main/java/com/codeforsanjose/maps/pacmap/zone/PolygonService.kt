package com.codeforsanjose.maps.pacmap.zone

import io.reactivex.Completable
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


interface PolygonService {
    @GET("geo/")
    fun getFeatureCollection(): Observable<FeatureCollection>

    @GET("geo/")
    fun downloadFeatureCollection(): Call<ResponseBody>

    @GET("geo/{id}")
    fun getFeature(@Path("id") id: String): Observable<Feature>

    @GET("geo/{id}/{timestamp}")
    fun markComplete(@Path("id") id: String, @Path("timestamp") timestamp: Long): Completable
}