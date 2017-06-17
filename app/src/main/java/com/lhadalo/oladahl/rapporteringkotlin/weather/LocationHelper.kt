package com.lhadalo.oladahl.rapporteringkotlin.weather

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.lhadalo.oladahl.rapporteringkotlin.weather.LocationConstant.FASTEST_INTERVAL
import com.lhadalo.oladahl.rapporteringkotlin.weather.LocationConstant.LOCATION_REQUEST
import com.lhadalo.oladahl.rapporteringkotlin.weather.LocationConstant.UPDATE_INTERVAL

object LocationConstant {
    val LOCATION_REQUEST = 100
    val REQUEST_CHECK_SETTINGS = 102
    var UPDATE_INTERVAL = (10 * 1000).toLong()
    var FASTEST_INTERVAL: Long = 2000
}

class LocationHelper(val context: Context) : GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    val TAG = "LocationHelper"


    val mGoogleApiClient: GoogleApiClient by lazy {
        GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    fun doConnect(connect: Boolean): Unit {
        if (connect) mGoogleApiClient.connect()
        else mGoogleApiClient.disconnect()
    }


    override fun onConnected(p0: Bundle?) {
        if (checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    context as AppCompatActivity,
                    arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_REQUEST
            )
            doConnect(false)
        } else {
            val mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
            if (mLastLocation != null) {
                WeatherFetcher.newInstance(context).fetchWeather(mLastLocation)
                Log.d(TAG, mLastLocation.toString())
            } else {
                val locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setInterval(UPDATE_INTERVAL)
                        .setFastestInterval(FASTEST_INTERVAL)

                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleApiClient, locationRequest, this)

                doConnect(true)
            }
        }
    }

    override fun onConnectionSuspended(p0: Int) {}

    override fun onConnectionFailed(connResults: ConnectionResult) {}

    override fun onLocationChanged(location: Location?) {
        Log.d(TAG, "Location Changed")
    }

}
