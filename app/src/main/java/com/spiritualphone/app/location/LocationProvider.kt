package com.spiritualphone.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.spiritualphone.app.debug.DebugLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Real device location from the platform [LocationManager] (no Google Play
 * Services). We drive the map's location component ourselves because MapLibre's
 * internal engine frequently never delivers a fix.
 *
 * Listeners are registered on all present providers even if location is
 * currently OFF, so fixes start flowing automatically the moment the user
 * enables location (onProviderEnabled). Heavily logged via [DebugLog].
 */
class LocationProvider(private val context: Context) {

    private fun manager() =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private fun providerOrder(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }

    /** Whether the system location master switch is on. */
    fun isLocationEnabled(): Boolean {
        val lm = manager()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            providerOrder().any { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val lm = manager()

        DebugLog.log("Location: allProviders=${lm.allProviders} locationEnabled=${isLocationEnabled()}")

        bestLastKnown(lm)?.let {
            DebugLog.log("Location: lastKnown ${it.provider} ${it.latitude},${it.longitude} acc=${it.accuracy}m")
            trySend(it)
        } ?: DebugLog.log("Location: no cached lastKnown")

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                DebugLog.log("Location: FIX ${location.provider} ${location.latitude},${location.longitude} acc=${location.accuracy}m")
                trySend(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {
                DebugLog.log("Location: provider '$provider' turned ON")
            }
            override fun onProviderDisabled(provider: String) {
                DebugLog.log("Location: provider '$provider' turned OFF")
            }
        }

        // Register on every present provider regardless of current enabled state,
        // so enabling location later starts delivering fixes without re-subscribing.
        var registered = 0
        for (p in providerOrder()) {
            if (!lm.allProviders.contains(p)) continue
            val enabled = runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)
            runCatching {
                lm.requestLocationUpdates(p, 1000L, 0f, listener, Looper.getMainLooper())
            }.onSuccess {
                registered++
                DebugLog.log("Location: listening on '$p' (enabled=$enabled)")
            }.onFailure {
                DebugLog.log("Location: requestUpdates '$p' failed: ${it.message}")
            }
        }
        when {
            registered == 0 -> DebugLog.log("Location: could not register any provider")
            !isLocationEnabled() -> DebugLog.log("Location: registered, but system location is OFF — waiting for user to enable it")
        }

        awaitClose {
            lm.removeUpdates(listener)
            DebugLog.log("Location: updates stopped")
        }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnown(lm: LocationManager): Location? =
        providerOrder()
            .filter { lm.allProviders.contains(it) }
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
}
