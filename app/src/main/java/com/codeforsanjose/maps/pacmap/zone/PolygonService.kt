package com.codeforsanjose.maps.pacmap.zone

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path


interface PolygonService {
    @GET("geo/")
    fun getFeatureCollection(): Observable<FeatureCollection>

    @GET("geo/{id}")
    fun getFeature(@Path("id") id: String): Observable<Feature>
}