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
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView

/**
 * Schematic (vector) map centred on the user, showing the player's own
 * position via MapLibre's built-in location component (GPS blue dot +
 * compass heading + camera tracking).
 *
 * Uses the free MapLibre demo style — no API key required.
 */
private const val SCHEMATIC_STYLE = "https://demotiles.maplibre.org/style.json"

@SuppressLint("MissingPermission")
@Composable
fun SpiritRadarMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapLibre must be initialised before any MapView is created.
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // Forward Android lifecycle events into the MapView.
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
                map.setStyle(SCHEMATIC_STYLE) { style ->
                    val location = map.locationComponent
                    location.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style).build()
                    )
                    location.isLocationComponentEnabled = true
                    location.cameraMode = CameraMode.TRACKING
                    location.renderMode = RenderMode.COMPASS
                    map.cameraPosition = CameraPosition.Builder().zoom(15.0).build()
                }
            }
            mapView
        }
    )
}
