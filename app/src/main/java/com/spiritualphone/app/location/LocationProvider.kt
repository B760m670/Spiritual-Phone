package com.spiritualphone.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Real device location, sourced from the platform [LocationManager] (no Google
 * Play Services dependency).
 *
 * We deliberately drive the map's location component ourselves instead of
 * relying on MapLibre's internal engine, because that engine often never
 * delivers a fix (its `lastKnownLocation` stays null), which made the camera
 * recenter on the map centre instead of the user. Feeding real fixes here fixes
 * the "my location" button.
 *
 * Emits the last known fix immediately (if any) for a fast first paint, then
 * live updates from GPS and network providers. Caller must hold location
 * permission before collecting.
 */
class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Fast first value: best of the cached fixes.
        bestLastKnown(lm)?.let { trySend(it) }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            // Required no-op overrides for older API levels.
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var registered = false
        for (provider in providers) {
            if (lm.isProviderEnabled(provider)) {
                lm.requestLocationUpdates(provider, 1000L, 0f, listener)
                registered = true
            }
        }
        if (!registered) close()

        awaitClose { lm.removeUpdates(listener) }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnown(lm: LocationManager): Location? {
        val candidates = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        return candidates.maxByOrNull { it.time }
    }
}
