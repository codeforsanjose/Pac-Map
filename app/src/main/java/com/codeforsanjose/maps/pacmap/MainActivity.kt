package com.codeforsanjose.maps.pacmap

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.codeforsanjose.maps.pacmap.core.StreamUtils.Companion.rawResourceToString
import com.codeforsanjose.maps.pacmap.demo.DebugLocationEngine
import com.codeforsanjose.maps.pacmap.demo.LatLngList
import com.codeforsanjose.maps.pacmap.zone.FeatureCollection
import com.codeforsanjose.maps.pacmap.zone.ZoneManager.Companion.fetchZones
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.PolygonOptions
import com.mapbox.mapboxsdk.annotations.Polyline
import com.mapbox.mapboxsdk.annotations.PolylineOptions
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
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.squareup.moshi.Moshi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.net.URL


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener, OnMapClickListener,
        PermissionsListener {


    companion object {
        const val GEO_SOURCE_ID = "Source_1"
        const val GEO_FILL_LAYER_ID = "FillLayer1"
        const val GEO_LINE_LAYER_ID = "LineLayer1"
    }

    var permissionsManager: PermissionsManager? = null
    var mapboxMap: MapboxMap? = null
    var locationPlugin: LocationLayerPlugin? = null
    var locationEngine: LocationEngine? = null
    var originLocation: Location? = null
    var currentRoute: DirectionsRoute? = null
    var navigationMapRoute: NavigationMapRoute? = null

    var previousLocations : ArrayList<LatLng> = arrayListOf()

    var menuFabIsOpen = false
    lateinit var progressBar: ProgressBar
    lateinit var menuFab: FloatingActionButton
    lateinit var settingsFab: FloatingActionButton
    lateinit var showInfoFab: FloatingActionButton
    lateinit var liveModeFab: FloatingActionButton
    lateinit var demoFab: FloatingActionButton
    lateinit var navigateHereButton: Button

    var zones: FeatureCollection? = null
    val polygonList = ArrayList<ArrayList<LatLng>>()

    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_main)

        //dlZones()

        getFABulous()

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        navigateHereButton = findViewById(R.id.navButton)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl(MAPBOX_STREETS)
        mapView.getMapAsync(this)
    }

    private fun getFABulous() {
        menuFab = findViewById(R.id.menuFab)
        settingsFab = findViewById(R.id.settingsFab)
        showInfoFab = findViewById(R.id.showInfoFab)
        liveModeFab = findViewById(R.id.liveModeFab)
        demoFab = findViewById(R.id.demoFab)
        rotateFabsOut()

        if (BuildConfig.DEBUG) {
            demoFab.visibility = View.VISIBLE
            demoFab.setOnClickListener { _ ->
                locationEngine?.let { loc ->
                    toggleDemoLocationEngine()
                }
            }
        }

        menuFab.setOnClickListener { _ ->
            if (menuFabIsOpen) {
                Timber.d("fabs are open; closing the fabs")
                rotateFabsOut()
                menuFabIsOpen = false
            } else {
                Timber.d("fabs are closed; opening the fabs")
                rotateFabsIn()
                menuFabIsOpen = true
            }
        }

        settingsFab.setOnClickListener { _ ->
            Timber.d("Settings FAB was touched")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        showInfoFab.setOnClickListener { _ ->
            Timber.d("Info FAB was touched")
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }

        liveModeFab.setOnClickListener { _ ->
            Timber.d("Settings FAB was touched")
        }
    }

    private fun rotateFabsIn() {
        val center = IntArray(2)
        menuFab.getLocationInWindow(center)
        //   center[0] = center[0] - (menuFab.width / 2)
        center[1] = center[1] - (menuFab.height / 2)
        val rectf = RectF()
        val radius = 250f
        rectf.set(-radius, -radius, radius, radius)
        rectf.offset(center[0].toFloat(), center[1].toFloat())

        val fabs = arrayOf(settingsFab, showInfoFab, liveModeFab)
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

    private fun rotateFabsOut() {
        val center = IntArray(2)
        menuFab.getLocationInWindow(center)
        //   center[0] = center[0] - (menuFab.width / 2)
        center[1] = center[1] - (menuFab.height / 2)
        val rectf = RectF()
        val radius = 250f
        rectf.set(-radius, -radius, radius, radius)
        rectf.offset(center[0].toFloat(), center[1].toFloat())

        val fabs = arrayOf(settingsFab, showInfoFab, liveModeFab)
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
            initRealLocationEngine()

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }

        //updateZones()
        addGeoJsonLayersToMap()
    }

    @SuppressLint("MissingPermission")
    private fun initRealLocationEngine() {
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
    }

    @SuppressLint("MissingPermission")
    private fun toggleDemoLocationEngine() {
        locationEngine?.let { loc ->
            loc.deactivate()
            mapboxMap?.removeLayer("mapbox-location-layer")
            mapboxMap?.removeLayer("mapbox-location-bearing-layer")
            mapboxMap?.removeLayer("mapbox-location-accuracy-layer")
            mapboxMap?.removeLayer("mapbox-location-shadow")
            mapboxMap?.removeLayer("mapbox-location-stroke-layer")
            mapboxMap?.removeSource("mapbox-location-source")
            locationPlugin?.setLocationLayerEnabled(false)

            if (locationEngine is DebugLocationEngine) {
                Timber.d("Stopping the demo")
                initRealLocationEngine()
            } else {
                Timber.d("Starting the location demo")
                locationEngine = DebugLocationEngine()

                val moshi = Moshi.Builder().build()
                val jsonAdapter = moshi.adapter(LatLngList::class.java)
                val demoString = rawResourceToString(this, R.raw.demo_trip_1)
                val lll = jsonAdapter.fromJson(demoString)

                (locationEngine as DebugLocationEngine).setSource(lll)
                locationEngine?.activate()
                locationEngine?.addLocationEngineListener(this)

                mapboxMap?.let {
                    val options = LocationLayerOptions.builder(this).build();
                    locationPlugin = LocationLayerPlugin(mapView, it, locationEngine, options);
                    locationPlugin?.cameraMode = CameraMode.TRACKING
                }
            }
        }
    }

    private fun addGeoJsonLayersToMap() {
        // Fill in the polygons
        val fillLayer = FillLayer(GEO_FILL_LAYER_ID, GEO_SOURCE_ID)
        fillLayer.setProperties(
                visibility(VISIBLE),
                fillColor(Color.parseColor("#0000ff")),
                fillOutlineColor(Color.parseColor("#000000ff")),
                fillOpacity(0.25f))
        mapboxMap?.addLayer(fillLayer)

        // Draw the polygon outlines
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
            if (!featureList.isEmpty()) {
                val feat = featureList.first()
                Timber.d("Feature found with %s", feat.toJson())
                zoomToFeature(feat)
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

        originLocation?.let { userLocation ->
            val originPosition = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
            val destinationPosition = Point.fromLngLat(bounds.center.longitude, bounds.center.latitude)
            getRoute(originPosition, destinationPosition)
            progressBar.visibility = View.VISIBLE

            navigateHereButton.text = getString(R.string.navigate_button_route_loading_text)
            navigateHereButton.isEnabled = false
            navigateHereButton.visibility = View.VISIBLE
            navigateHereButton.setOnClickListener {
                Timber.d("Nagivation button was clicked")

                // Pass in your Amazon Polly pool id for speech synthesis using Amazon Polly
                // Set to null to use the default Android speech synthesizer
                //val awsPoolId: String? = null
                val simulateRoute = true
                val options = NavigationLauncherOptions.builder()
                        .origin(originPosition)
                        .destination(destinationPosition)
                        //.awsPoolId(awsPoolId)
                        .shouldSimulateRoute(simulateRoute)
                        .build()
                NavigationLauncher.startNavigation(this, options)
            }
        }
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onFailure(call: Call<DirectionsResponse>?, t: Throwable?) {
                        Timber.e(t)
                        Toast.makeText(applicationContext, "", Toast.LENGTH_LONG).show()
                        progressBar.visibility = View.GONE
                        navigateHereButton.visibility = View.GONE
                        navigateHereButton.isEnabled = true
                    }

                    override fun onResponse(call: Call<DirectionsResponse>?, response: Response<DirectionsResponse>?) {
                        if (response == null) {
                            Timber.e("Null DirectionsResponse")
                            return
                        }
                        Timber.d("Response code: %s", response.code())
                        response.body()?.let { body ->
                            if (response.body() == null) {
                                Timber.e("No routes found, make sure you set the right user and access token.")
                                return
                            } else if (body.routes().size < 1) {
                                Timber.e("No routes found")
                                return
                            }

                            currentRoute = body.routes()[0]

                            // Draw the route on the map
                            navigationMapRoute?.removeRoute()
                            if (navigationMapRoute == null) {
                                navigationMapRoute = mapboxMap?.let {
                                    NavigationMapRoute(null, mapView, it, R.style.NavigationMapRoute)
                                }
                            }
                            navigationMapRoute?.addRoute(currentRoute)
                            navigateHereButton.text = getString(R.string.navigate_button_text)
                            navigateHereButton.isEnabled = true
                            progressBar.visibility = View.GONE
                        }
                    }
                })
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
//                            addGeoJsonLayersToMap()

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
                LatLng(location.latitude, location.longitude), 15.0), 50)
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            originLocation = it
            updateDrivingLine( LatLng(it.latitude, it.longitude))
            setCameraPosition(it)
        }
    }

    private fun updateDrivingLine(ll : LatLng){
        previousLocations.add(ll)
        if (previousLocations.isEmpty()){
            Timber.d("Empty locations list")
            return
        }
        mapboxMap?.addPolyline(PolylineOptions()
                .addAll(previousLocations)
                .color(Color.parseColor("#098e17"))
                .alpha(1f)
                .width(5f))
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
