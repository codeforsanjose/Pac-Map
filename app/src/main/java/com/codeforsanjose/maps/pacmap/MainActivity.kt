package com.codeforsanjose.maps.pacmap

import android.annotation.SuppressLint
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.telemetry.location.LocationEngineListener
import com.mapbox.services.android.telemetry.permissions.PermissionsListener

import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.telemetry.permissions.PermissionsManager
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode
import com.mapbox.services.android.telemetry.location.LocationEngine
import com.mapbox.services.android.telemetry.location.LocationEnginePriority
import com.mapbox.services.android.telemetry.location.LostLocationEngine
import timber.log.Timber


class MainActivity : AppCompatActivity(), LocationEngineListener, PermissionsListener {

    var permissionsManager : PermissionsManager? = null
    var mapboxMap: MapboxMap? = null
    var locationPlugin: LocationLayerPlugin? = null
    var locationEngine: LocationEngine? = null
    var originLocation: Location? = null

    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this,  getString(R.string.access_token))

        setContentView(R.layout.activity_main)

        mapView = findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync({ mBoxMap ->
            mapboxMap = mBoxMap
            enableLocationPlugin()
        })
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
        location?.let{
            originLocation = it
            setCameraPosition(it)
            locationEngine?.removeLocationEngineListener(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationPlugin() {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine()

            mapboxMap?.let {
                locationPlugin = LocationLayerPlugin(mapView, mapboxMap!!, locationEngine)
                locationPlugin?.setLocationLayerEnabled(LocationLayerMode.TRACKING)
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LostLocationEngine(this)

        locationEngine?.let {
            it.setPriority(LocationEnginePriority.HIGH_ACCURACY)
            it.activate()
            val lastLocation = it.getLastLocation()
            if (lastLocation != null) {
                originLocation = lastLocation
                setCameraPosition(lastLocation)
            } else {
                it.addLocationEngineListener(this)
            }
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
            enableLocationPlugin();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            Timber.e("permissions not granted")
            finish();
        }
    }
}
