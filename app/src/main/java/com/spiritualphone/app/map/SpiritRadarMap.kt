package com.spiritualphone.app.map

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.provider.Settings
import android.view.Gravity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.location.LocationProvider
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * Ordinary street map centred on the user, with a working "my location" button.
 *
 * Real device location comes from [LocationProvider] (platform LocationManager)
 * and is pushed into MapLibre's location component via forceLocationUpdate. If
 * the system location switch is off, a banner invites the user to enable it.
 */
private const val OSM_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap"
    }
  },
  "layers": [
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
"""

// Shown until the first GPS fix arrives, so the screen is never blank.
private const val DEFAULT_ZOOM = 13.0
private const val FOLLOW_ZOOM = 16.0
private val DEFAULT_CENTER = LatLng(20.0, 0.0)

@SuppressLint("MissingPermission")
@Composable
fun SpiritRadarMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = context.resources.displayMetrics.density
    val margin = (8 * density).toInt()

    val locationProvider = remember { LocationProvider(context) }

    val mapView = remember {
        MapLibre.getInstance(context)
        org.maplibre.android.maps.MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var locationEnabled by remember { mutableStateOf(locationProvider.isLocationEnabled()) }

    fun openLocationSettings() {
        DebugLog.log("Opening system location settings")
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Forward Android lifecycle into the MapView; re-check the location switch
    // whenever we resume (e.g. returning from system settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    locationEnabled = locationProvider.isLocationEnabled()
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Feed real device locations into the map's location component.
    LaunchedEffect(map) {
        val mlMap = map ?: return@LaunchedEffect
        DebugLog.log("Map: collecting location updates")
        locationProvider.locationUpdates().collect { loc ->
            lastLocation = loc
            locationEnabled = true
            mlMap.locationComponent.forceLocationUpdate(loc)
            DebugLog.log("Map: forceLocationUpdate ${loc.latitude},${loc.longitude}")
        }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.getMapAsync { mlMap ->
                    mlMap.uiSettings.isLogoEnabled = false
                    mlMap.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.START
                    mlMap.uiSettings.setAttributionMargins(margin, 0, 0, margin)

                    mlMap.cameraPosition = CameraPosition.Builder()
                        .target(DEFAULT_CENTER)
                        .zoom(DEFAULT_ZOOM)
                        .build()
                    mlMap.setStyle(Style.Builder().fromJson(OSM_STYLE)) { style ->
                        val location = mlMap.locationComponent
                        location.activateLocationComponent(
                            LocationComponentActivationOptions.builder(context, style)
                                .useDefaultLocationEngine(false)
                                .build()
                        )
                        location.isLocationComponentEnabled = true
                        location.cameraMode = CameraMode.TRACKING
                        location.renderMode = RenderMode.COMPASS
                        location.zoomWhileTracking(FOLLOW_ZOOM)
                        DebugLog.log("Map: style loaded, location component activated")
                        map = mlMap
                    }
                }
                mapView
            }
        )

        if (!locationEnabled) {
            Surface(
                color = Color(0xF2E53935),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clickable { openLocationSettings() },
            ) {
                Text(
                    "📍 Геолокация выключена — нажмите, чтобы включить",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (!locationProvider.isLocationEnabled()) {
                    locationEnabled = false
                    openLocationSettings()
                } else {
                    map?.let { recenterOnUser(it, lastLocation) }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Моё местоположение")
        }
    }
}

/**
 * "My location" button behaviour, as in navigation apps: re-engage tracking and
 * fly to the user's real position; tracking then follows until the user pans.
 */
private fun recenterOnUser(map: MapLibreMap, lastLocation: Location?) {
    DebugLog.log("Button: my-location pressed, lastLocation=${lastLocation?.let { "${it.latitude},${it.longitude}" } ?: "null"}")
    val location = map.locationComponent
    location.cameraMode = CameraMode.TRACKING
    location.zoomWhileTracking(FOLLOW_ZOOM)
    lastLocation?.let { loc ->
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), FOLLOW_ZOOM)
        )
    }
}
