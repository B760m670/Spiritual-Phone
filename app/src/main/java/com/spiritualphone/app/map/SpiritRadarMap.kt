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
 * Schematic dark "spiritual" map centred on the user, showing the player's own
 * position via MapLibre's built-in location component (GPS dot + compass +
 * camera tracking).
 *
 * The basemap uses OpenFreeMap **vector** tiles (free, no API key) with a
 * fully custom style defined below — so every colour/layer is ours to change,
 * unlike the previous raster basemap which could only be drawn over.
 */
private const val SPIRIT_DARK_STYLE = """
{
  "version": 8,
  "name": "Spiritual Dark",
  "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
  "sources": {
    "openmaptiles": {
      "type": "vector",
      "url": "https://tiles.openfreemap.org/planet"
    }
  },
  "layers": [
    { "id": "background", "type": "background",
      "paint": { "background-color": "#0B0E14" } },
    { "id": "landcover", "type": "fill", "source": "openmaptiles", "source-layer": "landcover",
      "paint": { "fill-color": "#0e1320", "fill-opacity": 0.6 } },
    { "id": "park", "type": "fill", "source": "openmaptiles", "source-layer": "park",
      "paint": { "fill-color": "#0d1a16", "fill-opacity": 0.5 } },
    { "id": "water", "type": "fill", "source": "openmaptiles", "source-layer": "water",
      "paint": { "fill-color": "#08111e" } },
    { "id": "building", "type": "fill", "source": "openmaptiles", "source-layer": "building", "minzoom": 13,
      "paint": { "fill-color": "#161b28", "fill-opacity": 0.7 } },
    { "id": "road-minor", "type": "line", "source": "openmaptiles", "source-layer": "transportation",
      "filter": ["in", "class", "minor", "service", "track"],
      "paint": { "line-color": "#222838",
        "line-width": ["interpolate", ["linear"], ["zoom"], 12, 0.5, 18, 4] } },
    { "id": "road-major", "type": "line", "source": "openmaptiles", "source-layer": "transportation",
      "filter": ["in", "class", "primary", "secondary", "tertiary", "trunk", "motorway"],
      "paint": { "line-color": "#333a50",
        "line-width": ["interpolate", ["linear"], ["zoom"], 8, 0.6, 18, 8] } },
    { "id": "boundary", "type": "line", "source": "openmaptiles", "source-layer": "boundary",
      "filter": ["<=", "admin_level", 4],
      "paint": { "line-color": "#3a2030", "line-dasharray": [2, 2], "line-width": 1 } },
    { "id": "place-label", "type": "symbol", "source": "openmaptiles", "source-layer": "place",
      "filter": ["in", "class", "city", "town", "village"],
      "layout": { "text-field": "{name}", "text-font": ["Noto Sans Regular"],
        "text-size": ["interpolate", ["linear"], ["zoom"], 6, 10, 12, 15] },
      "paint": { "text-color": "#9aa0b4", "text-halo-color": "#05070b", "text-halo-width": 1.4 } }
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
                map.setStyle(Style.Builder().fromJson(SPIRIT_DARK_STYLE)) { style ->
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
