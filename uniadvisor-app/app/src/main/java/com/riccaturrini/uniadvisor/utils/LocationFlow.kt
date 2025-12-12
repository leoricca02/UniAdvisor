package com.riccaturrini.uniadvisor.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@SuppressLint("MissingPermission") // I permessi sono già controllati nella UI prima di chiamare questo
fun getLocationFlow(context: Context): Flow<Location> = callbackFlow {
    val client = LocationServices.getFusedLocationProviderClient(context)

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000) // Aggiorna ogni 2 secondi
        .setMinUpdateIntervalMillis(1000) // Minimo 1 secondo tra aggiornamenti
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                trySend(location) // Invia la nuova posizione al Dialog
            }
        }
    }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())

    awaitClose {
        client.removeLocationUpdates(callback) // Ferma il GPS quando il dialog si chiude
    }
}