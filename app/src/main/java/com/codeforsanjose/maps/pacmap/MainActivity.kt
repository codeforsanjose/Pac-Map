package com.codeforsanjose.maps.pacmap

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.codeforsanjose.maps.pacmap.zone.FeatureCollection
import com.codeforsanjose.maps.pacmap.zone.ZoneManager.Companion.dlZones
import com.codeforsanjose.maps.pacmap.zone.ZoneManager.Companion.fetchZones
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.telemetry.location.LocationEngine
import com.mapbox.services.android.telemetry.location.LocationEngineListener
import com.mapbox.services.android.telemetry.location.LocationEnginePriority
import com.mapbox.services.android.telemetry.location.LostLocationEngine
import com.mapbox.services.android.telemetry.permissions.PermissionsListener
import com.mapbox.services.android.telemetry.permissions.PermissionsManager
import com.mapbox.services.commons.geojson.Feature
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.URL


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, MapboxMap.OnMapClickListener,
        PermissionsListener {


    companion object {
        const val GEO_SOURCE_ID = "1"
        const val GEO_LAYER_ID = "Layer1"
    }

    var permissionsManager: PermissionsManager? = null
    var mapboxMap: MapboxMap? = null
    var locationPlugin: LocationLayerPlugin? = null
    var locationEngine: LocationEngine? = null
    var originLocation: Location? = null

    var zones: FeatureCollection? = null
    val polygonList = ArrayList<ArrayList<LatLng>>()

    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main)

        dlZones()

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    public override fun onStart() {
        super.onStart()
        locationEngine?.requestLocationUpdates()
        locationPlugin?.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationPlugin?.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("low memory")
        mapView.onLowMemory()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: MapboxMap?) {
        if (map != null) {
            Timber.d("Map is ready")
            mapboxMap = map
            mapboxMap?.let {
                it.addOnMapClickListener(this)
                it.addSource(GeoJsonSource(GEO_SOURCE_ID, URL("https://pac-map.herokuapp.com/geo/")))
            }

        }

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // With LocationLayer 0.5.0+ it should be:
            // locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
            locationEngine = LostLocationEngine(this)

            locationEngine?.let {
                it.priority = LocationEnginePriority.HIGH_ACCURACY
                it.fastestInterval = 1000
                it.activate()
                val lastLocation = it.lastLocation
                if (lastLocation != null) {
                    originLocation = lastLocation
                    setCameraPosition(lastLocation)
                } else {
                    it.addLocationEngineListener(this)
                }
            }

            mapboxMap?.let {
                locationPlugin = LocationLayerPlugin(mapView, it, locationEngine)
                locationPlugin?.setLocationLayerEnabled(LocationLayerMode.TRACKING)
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }

//        updateZones()
        addExtrusionsLayerToMap()
    }

    private fun addExtrusionsLayerToMap(){
        // Add FillExtrusion layer to map using GeoJSON data
        val courseExtrusionLayer = FillExtrusionLayer(GEO_LAYER_ID, GEO_SOURCE_ID);
        courseExtrusionLayer.setProperties(
                fillExtrusionColor(Color.YELLOW),
                fillExtrusionOpacity(0.7f))

        mapboxMap?.addLayer(courseExtrusionLayer);
    }

    override fun onMapClick(point: LatLng) {
        Timber.v("Map was touched at point lat: %s,  lon: %s", point.latitude, point.longitude)
        mapboxMap?.let { map ->
            val pointf = map.projection.toScreenLocation(point)
            val rectF = RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10)

            val featureList = map.queryRenderedFeatures(rectF, GEO_LAYER_ID) as List<Feature>

            for (feature in featureList) {
                Timber.d("Feature found with %s", feature.toJson())

                Toast.makeText(this, "polycon clicked?",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateZones() {
        if (zones != null) {
            Timber.d("prevented a duplicate server call")
            return
        }

        val fetchOperation = fetchZones()

        fetchOperation.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            Timber.v("Result : %s", result)
                            zones = result

//                            val moshi =  Moshi.Builder().build()
//                            val jsonAdapter = moshi.adapter(FeatureCollection::class.java)
//                            val json = jsonAdapter.toJson(zones);
//                            val courseRouteGeoJson = GeoJsonSource(GEO_SOURCE_ID, json);
//                            mapboxMap?.addSource(courseRouteGeoJson)

                            //drawPolygons()
                        },
                        { error ->
                            Timber.e(error)
                        })
    }


    private fun drawPolygons() {
        if (polygonList.size > 0) {
            Timber.d("prevented a duplicate parsing of polygons")
            return
        }

        zones?.let {
            var poly: ArrayList<LatLng>
            for (feat in it.features) {
                if (feat.geometry.type == "Polygon") {
                    for (ca in feat.geometry.coordinates) {
                        poly = arrayListOf()
                        for (cb in ca) {
                            poly.add(LatLng(cb[1] as Double, cb[0] as Double))
                        }
                        polygonList.add(poly)
                    }
                } else if (feat.geometry.type == "MultiPolygon") {
                    Timber.w("Found a MultiPolygon in the dataset. Not yet supportted.")
                }
            }

        }
        Timber.w("About to draw %d polygons to the map", polygonList.size)
        for (p in polygonList) {
            mapboxMap?.addPolygon(PolygonOptions()
                    .addAll(p)
                    .fillColor(Color.parseColor("#3bb2d0")));
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationEngine?.deactivate()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState!!)
    }

    private fun setCameraPosition(location: Location) {
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 12.0))
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            originLocation = it
            setCameraPosition(it)
            locationEngine?.removeLocationEngineListener(this)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine?.requestLocationUpdates();
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Timber.d("not implemented")
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            onMapReady(null);
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            Timber.e("permissions not granted")
            finish();
        }
    }
}
