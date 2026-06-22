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
 * Heavily logged via [DebugLog] so location issues can be diagnosed on-device.
 */
class LocationProvider(private val context: Context) {

    private fun providerOrder(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val all = lm.allProviders
        DebugLog.log("Location: allProviders=$all")
        for (p in providerOrder()) {
            val present = all.contains(p)
            val enabled = present && runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)
            DebugLog.log("Location: provider '$p' present=$present enabled=$enabled")
        }

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
                DebugLog.log("Location: provider '$provider' enabled")
            }
            override fun onProviderDisabled(provider: String) {
                DebugLog.log("Location: provider '$provider' disabled")
            }
        }

        var registered = 0
        for (p in providerOrder()) {
            if (lm.allProviders.contains(p) && runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) {
                runCatching {
                    lm.requestLocationUpdates(p, 1000L, 0f, listener, Looper.getMainLooper())
                }.onSuccess {
                    registered++
                    DebugLog.log("Location: requesting updates from '$p'")
                }.onFailure {
                    DebugLog.log("Location: requestUpdates '$p' failed: ${it.message}")
                }
            }
        }
        if (registered == 0) {
            DebugLog.log("Location: NO enabled providers — is location turned on?")
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
