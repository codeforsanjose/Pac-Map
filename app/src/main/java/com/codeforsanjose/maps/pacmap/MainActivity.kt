package com.codeforsanjose.maps.pacmap

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.codeforsanjose.maps.pacmap.zone.FeatureCollection
import com.codeforsanjose.maps.pacmap.zone.ZoneManager.Companion.fetchZones
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.Style.MAPBOX_STREETS
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapClickListener
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.URL


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, OnMapClickListener,
        PermissionsListener {


    companion object {
        const val GEO_SOURCE_ID = "Source_1"
        const val GEO_LAYER_ID = "Layer_1"
        const val GEO_FILL_LAYER_ID = "FillLayer1"
        const val GEO_LINE_LAYER_ID = "LineLayer1"
    }

    var permissionsManager: PermissionsManager? = null
    var mapboxMap: MapboxMap? = null
    var locationPlugin: LocationLayerPlugin? = null
    var locationEngine: LocationEngine? = null
    var originLocation: Location? = null

    var menuFabIsOpen = false
    lateinit var menuFab: FloatingActionButton
    lateinit var settingsFab: FloatingActionButton
    lateinit var localModeFab: FloatingActionButton
    lateinit var liveModeFab: FloatingActionButton

    var zones: FeatureCollection? = null
    val polygonList = ArrayList<ArrayList<LatLng>>()

    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main)

        //dlZones()

        getFABulous()

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl(MAPBOX_STREETS)
        mapView.getMapAsync(this)
    }

    fun getFABulous() {
        menuFab = findViewById(R.id.menuFab)
        settingsFab = findViewById(R.id.settingsFab)
        localModeFab = findViewById(R.id.localModeFab)
        liveModeFab = findViewById(R.id.liveModeFab)
        rotateFabsOut()

        menuFab.setOnClickListener({ v ->
            if (menuFabIsOpen) {
                Timber.d("fabs are open; closing the fabs")
                rotateFabsOut()
                menuFabIsOpen = false
            } else {
                Timber.d("fabs are closed; opening the fabs")
                rotateFabsIn()
                menuFabIsOpen = true
            }
        })
    }

    fun rotateFabsIn() {
        val center = IntArray(2)
        menuFab.getLocationInWindow(center)
        //   center[0] = center[0] - (menuFab.width / 2)
        center[1] = center[1] - (menuFab.height / 2)
        val rectf = RectF()
        val radius = 250f
        rectf.set(-radius, -radius, radius, radius)
        rectf.offset(center[0].toFloat(), center[1].toFloat())

        val fabs = arrayOf(settingsFab, localModeFab, liveModeFab)
        var angle = -90f
        for (fab in fabs) {
            val path = Path()
            path.arcTo(rectf, angle, -180f, true)
            val animator = ObjectAnimator.ofFloat(fab, View.X, View.Y, path)
            animator.duration = 800
            animator.start()
            angle -= 45f
        }
    }

    fun rotateFabsOut() {
        val center = IntArray(2)
        menuFab.getLocationInWindow(center)
        //   center[0] = center[0] - (menuFab.width / 2)
        center[1] = center[1] - (menuFab.height / 2)
        val rectf = RectF()
        val radius = 250f
        rectf.set(-radius, -radius, radius, radius)
        rectf.offset(center[0].toFloat(), center[1].toFloat())

        val fabs = arrayOf(settingsFab, localModeFab, liveModeFab)
        var angle = -90f - 180f
        for (fab in fabs) {
            val path = Path()
            path.arcTo(rectf, angle, 180f, true)
            val animator = ObjectAnimator.ofFloat(fab, View.X, View.Y, path)
            animator.duration = 800
            animator.start()
            angle -= 45f
        }
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
            locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
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
                val options = LocationLayerOptions.builder(this).build();
                locationPlugin = LocationLayerPlugin(mapView, it, locationEngine, options);
                locationPlugin?.cameraMode = CameraMode.TRACKING
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }

        //updateZones()
        addExtrusionsLayerToMap()
    }

    private fun addExtrusionsLayerToMap() {
        // Add FillExtrusion layer to map using GeoJSON data
        val fillLayer = FillLayer(GEO_FILL_LAYER_ID, GEO_SOURCE_ID)
        fillLayer.setProperties(
                visibility(VISIBLE),
                fillColor(Color.parseColor("#0000ff")),
                fillOutlineColor(Color.parseColor("#000000ff")),
                fillOpacity(0.25f))
        mapboxMap?.addLayer(fillLayer)

        val lineLayer = LineLayer(GEO_LINE_LAYER_ID, GEO_SOURCE_ID)
        lineLayer.setProperties(
                visibility(VISIBLE),
                lineWidth(2f),
                lineOpacity(0.4f),
                lineColor(Color.parseColor("#ff0000")))
        mapboxMap?.addLayer(lineLayer)
    }

    override fun onMapClick(point: LatLng) {
        Timber.v("Map was touched at point lat: %s,  lon: %s", point.latitude, point.longitude)
        mapboxMap?.let { map ->
            val pointf = map.projection.toScreenLocation(point)
            val rectF = RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10)

            val featureList = map.queryRenderedFeatures(rectF, GEO_FILL_LAYER_ID) as List<Feature>

            for (feature in featureList) {
                Timber.d("Feature found with %s", feature.toJson())

                Toast.makeText(this, "region selected",
                        Toast.LENGTH_SHORT).show()
                zoomToFeature(feature)
            }
        }
    }

    private fun zoomToFeature(feature: Feature) {
        val bounds = featureToCoords(feature)
        if (bounds == null) {
            Timber.e("Cannot zoom to feature; no LatLngs received:\n %s", feature)
            Toast.makeText(this, "MultiPolygons not yet supported", Toast.LENGTH_LONG).show()
            return
        }

        mapboxMap?.moveCamera(CameraUpdateFactory
                .newLatLngBounds(bounds, 20))
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
//                            val json = jsonAdapter.toJson(zones)
//                            mapboxMap?.addSource(GeoJsonSource(GEO_SOURCE_ID, json.toString()))
//                            addExtrusionsLayerToMap()

                            drawPolygons()
                        },
                        { error ->
                            Timber.e(error)
                        })
    }

    private fun featureToCoords(feature: com.codeforsanjose.maps.pacmap.zone.Feature): List<LatLng> {
        val lls = ArrayList<LatLng>()
        if (feature.geometry.type == "Polygon") {
            for (ca in feature.geometry.coordinates) {
                for (cb in ca) {
                    lls.add(LatLng(cb[1] as Double, cb[0] as Double))
                }
            }
        } else if (feature.geometry.type == "MultiPolygon") {
            Timber.w("Found a MultiPolygon in the dataset. Not yet supportted.")
        }
        return lls
    }

    private fun featureToCoords(feature: Feature): LatLngBounds? {
        val bounds = LatLngBounds.Builder()

        feature.bbox()?.let { bb ->
            bounds.include(LatLng(bb.southwest().latitude(), bb.southwest().longitude()))
            bounds.include(LatLng(bb.northeast().latitude(), bb.northeast().longitude()))
            return bounds.build()
        }

        feature.geometry()?.let { geometry ->
            if (geometry.type() == "Polygon") {
                Timber.d("Found a Polygon in the dataset.")
                val poly = geometry as Polygon
                poly.coordinates()?.let { coordinates ->
                    for (coord in coordinates) {
                        for (point in coord) {
                            bounds.include(LatLng(point.latitude(), point.longitude()))
                        }
                    }
                }
            } else {
                Timber.d("Found a Feature of type %s in the dataset, which is currently not supported.", geometry.type())
                return null
            }
        }

        return bounds.build()
    }

    private fun drawPolygons() {
        if (polygonList.size > 0) {
            Timber.d("prevented a duplicate parsing of polygons")
            return
        }

        zones?.let {
            var poly: ArrayList<LatLng>
            for (feat in it.features) {
                val lls = featureToCoords(feat)
                poly = arrayListOf()
                for (ll in lls) {
                    poly.add(ll)
                }
                polygonList.add(poly)
            }

        }
        Timber.w("About to draw %d polygons to the map", polygonList.size)
        for (p in polygonList) {
            mapboxMap?.addPolygon(PolygonOptions()
                    .addAll(p)
                    .alpha(0.3f)
                    .strokeColor(Color.parseColor("#ff0000"))
                    .fillColor(Color.parseColor("#5e64e5")))
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
