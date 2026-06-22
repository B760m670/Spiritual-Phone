package com.spiritualphone.app.map

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.Style

/**
 * Schematic street map centred on the user, showing the player's own
 * position via MapLibre's built-in location component (GPS dot +
 * compass heading + camera tracking).
 *
 * Uses OpenStreetMap raster tiles — real streets, no API key required.
 * A dark "spiritual" themed style is planned for the design milestone.
 */
private const val OSM_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "background", "type": "background", "paint": { "background-color": "#0B0E14" } },
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
"""

// Shown until the first GPS fix arrives, so the screen is never blank.
private const val DEFAULT_ZOOM = 13.0
private val DEFAULT_CENTER = LatLng(20.0, 0.0)

@SuppressLint("MissingPermission")
@Composable
fun SpiritRadarMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapLibre must be initialised before any MapView is created.
    val mapView = remember {
        MapLibre.getInstance(context)
        org.maplibre.android.maps.MapView(context)
    }

    // Forward Android lifecycle events into the MapView. Adding the observer
    // while the host is already RESUMED replays ON_CREATE/START/RESUME, so the
    // MapView is correctly initialised even when this screen appears late
    // (e.g. right after the location permission is granted).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
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

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(DEFAULT_CENTER)
                    .zoom(DEFAULT_ZOOM)
                    .build()
                map.setStyle(Style.Builder().fromJson(OSM_STYLE)) { style ->
                    val location = map.locationComponent
                    location.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style).build()
                    )
                    location.isLocationComponentEnabled = true
                    location.cameraMode = CameraMode.TRACKING
                    location.renderMode = RenderMode.COMPASS
                    // Zoom in to street level while the camera follows the GPS fix.
                    location.zoomWhileTracking(16.0)
                }
            }
            mapView
        }
    )
}
