package com.codeforsanjose.maps.pacmap.demo

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine
import com.mapbox.turf.TurfMeasurement
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DebugLocationEngine() :
        MockLocationEngine(200, 45, false) {

    // private var lastLocation: Location? = Location(DebugLocationEngine::class.java.simpleName)
    private var location: Location? = null
    private var tracking: Boolean = false

    override fun deactivate() {
        tracking = false
        super.deactivate()
    }

    fun setSource(lll: LatLngList?) {
        tracking = true
        lll?.let {
            Observable.fromArray(it.coordinates)
                    .subscribeOn(Schedulers.io())
                    .concatMapIterable({ coords -> coords.asIterable() })
                    .concatMap({ i -> Observable.just(i).delay(700, TimeUnit.MILLISECONDS) })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { ll ->
                                if (tracking) {
                                    Timber.d("Moving to Location [%s, %s]", ll[0], ll[1])
                                    location = mockLocation(Point.fromLngLat(ll[0], ll[1]))
                                    location?.let { l ->
                                        for (listener in locationListeners) {
                                            listener.onLocationChanged(l)
                                        }
                                    }
                                }
                            },
                            { error ->
                                Timber.e(error)
                            })
        }
    }

    private fun mockLocation(point: Point): Location? {
        location = Location(DebugLocationEngine::class.java.name)
        location?.let { loc ->
            loc.latitude = point.latitude()
            loc.longitude = point.longitude()

            // Need to convert speed to meters/second as specified in Android's Location object documentation.
            val speedInMeterPerSec = (speed * 1.609344 * 1000 / (60 * 60)).toFloat()
            loc.speed = speedInMeterPerSec

            loc.accuracy = 3f
            loc.time = System.currentTimeMillis()

            return loc
        }
        return null
    }

}