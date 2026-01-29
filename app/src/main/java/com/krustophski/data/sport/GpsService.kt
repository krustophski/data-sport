package com.krustophski.data.sport

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kvl.cyclotrack.vmix.VmixIntegrationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsService @Inject constructor(context: Application) : LiveData<Location>() {
    private val logTag = "GpsService"
    private val context = context
    var accessGranted = MutableLiveData(false)
    private val locationManager =
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.v(logTag, "New location result")
            val now = System.currentTimeMillis()
            VmixIntegrationManager.hub.setGps(
                lat = location.latitude,
                lon = location.longitude,
                altitudeM = location.altitude,
                speedMps = location.speed,
                nowMs = now
            )
            Log.v(
                logTag,
                "location: ${location.latitude},${location.longitude} +/- ${location.accuracy}m"
            )
            Log.v(
                logTag,
                "bearing: ${location.bearing} +/- ${location.bearingAccuracyDegrees}deg"
            )
            Log.v(
                logTag,
                "speed: ${location.speed} +/- ${location.speedAccuracyMetersPerSecond}m/s"
            )
            Log.v(
                logTag,
                "altitude: ${location.altitude} +/- ${location.verticalAccuracyMeters}m"
            )
            Log.v(
                logTag,
                "timestamp: ${location.elapsedRealtimeNanos}; ${location.time}"
            )
            value = location
        }

        @Deprecated("Uncommented for version 8.1")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(logTag, "GPS status changed: $status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(logTag, "GPS provider enabled")
            accessGranted.value = true
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(logTag, "GPS provider disabled")
            accessGranted.value = false
        }
    }

    init {
        accessGranted.value = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        startListening()
        Log.d(logTag, "GPS service initialized")
    }

    fun stopListening() {
        locationManager.removeUpdates(locationListener)
    }

    fun startListening() {
        accessGranted.value = true
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(logTag, "User has not granted permission to access fine location data")
            accessGranted.value = false
            return
        }

        val minTimeMs = 0L
        val minDistanceMeters = 0f

        if (BuildConfig.BUILD_TYPE == "debug") {
            val fakeProvider = "fakeLocationProvider"
            if (locationManager.allProviders.contains(fakeProvider)) {
                Log.d(logTag, "Registering fake location provider")
                locationManager.requestLocationUpdates(
                    fakeProvider,
                    minTimeMs,
                    minDistanceMeters,
                    locationListener
                )
                return
            }
            Log.w(logTag, "Fake location provider missing; falling back to GPS")
        }

        if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            Log.d(logTag, "Registering GPS location provider")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                locationListener
            )
        }
        if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            Log.d(logTag, "Registering Network location provider")
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                locationListener
            )
        }
        if (locationManager.allProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
            Log.d(logTag, "Registering Passive location provider")
            locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                locationListener
            )
        }
        val fusedProvider = "fused"
        if (locationManager.allProviders.contains(fusedProvider)) {
            Log.d(logTag, "Registering Fused location provider")
            locationManager.requestLocationUpdates(
                fusedProvider,
                minTimeMs,
                minDistanceMeters,
                locationListener
            )
        }
    }
}
