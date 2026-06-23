package com.spiritualphone.app.map

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PointF
import android.graphics.RectF
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
import androidx.compose.runtime.collectAsState
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
import com.spiritualphone.app.audio.SoundManager
import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.location.LocationProvider
import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.notify.HollowNotifier
import com.spiritualphone.app.ui.HollowDetailsSheet
import com.spiritualphone.app.ui.RadarControls
import com.spiritualphone.app.world.AlertConfig
import com.spiritualphone.app.world.GeoMath
import com.spiritualphone.app.world.HollowSpawner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

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

private const val DEFAULT_ZOOM = 13.0
private const val FOLLOW_ZOOM = 16.0
private val DEFAULT_CENTER = LatLng(20.0, 0.0)

/**
 * The map screen: user location, the close-range Hollow dots (within 1 km) and
 * the radar search (up to 25 km). Hollows exist across the whole spawn radius
 * but only those within the alert radius are shown normally; the radar reveals
 * the rest as danger triangles while searching.
 */
@SuppressLint("MissingPermission")
@Composable
fun SpiritRadarMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = context.resources.displayMetrics.density
    val margin = (8 * density).toInt()

    val locationProvider = remember { LocationProvider(context) }
    val spawner = remember { HollowSpawner() }
    val notifier = remember { HollowNotifier(context) }
    val hollows by spawner.hollows.collectAsState()

    val mapView = remember {
        MapLibre.getInstance(context)
        org.maplibre.android.maps.MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var hollowLayer by remember { mutableStateOf<HollowLayer?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var locationEnabled by remember { mutableStateOf(locationProvider.isLocationEnabled()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var radarActive by remember { mutableStateOf(false) }
    var radarRadiusM by remember { mutableStateOf(AlertConfig.RADAR_RADII_M.first()) }
    var alarmed by remember { mutableStateOf(false) }
    val alerted = remember { mutableSetOf<String>() }

    fun distanceTo(h: Hollow): Double? = lastLocation?.let {
        GeoMath.distanceM(it.latitude, it.longitude, h.lat, h.lon)
    }

    // Hollows within the close alert radius (shown normally + notified).
    val alertList = hollows.filter { (distanceTo(it) ?: Double.MAX_VALUE) <= AlertConfig.ALERT_RADIUS_M }
    // Hollows caught by the radar while searching.
    val radarList = if (radarActive) {
        hollows.filter { (distanceTo(it) ?: Double.MAX_VALUE) <= radarRadiusM }
    } else {
        emptyList()
    }

    fun openLocationSettings() {
        DebugLog.log("Opening system location settings")
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

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

    LaunchedEffect(map) {
        val mlMap = map ?: return@LaunchedEffect
        locationProvider.locationUpdates().collect { loc ->
            lastLocation = loc
            locationEnabled = true
            mlMap.locationComponent.forceLocationUpdate(loc)
        }
    }

    LaunchedEffect(spawner) {
        spawner.onSpawn = { hollow -> DebugLog.log("Hollow spawned at ${hollow.lat},${hollow.lon}") }
        spawner.simulate { lastLocation?.let { it.latitude to it.longitude } }
    }

    // Show only close-range dots (none while the radar is drawing triangles).
    LaunchedEffect(alertList, radarActive, hollowLayer) {
        hollowLayer?.update(if (radarActive) emptyList() else alertList)
    }

    // Notify once per Hollow that enters the 1 km alert radius (sound via channel).
    LaunchedEffect(alertList) {
        alertList.forEach { h -> if (alerted.add(h.id)) notifier.notifySpawn(h) }
        alerted.retainAll(hollows.map { it.id }.toSet())
    }

    // Radar alarm when more than the threshold of objects are caught.
    LaunchedEffect(radarList.size, radarActive) {
        if (radarActive && radarList.size > AlertConfig.DANGER_COUNT_THRESHOLD) {
            if (!alarmed) {
                SoundManager.playAlarm(context)
                alarmed = true
            }
        } else {
            alarmed = false
        }
    }

    DisposableEffect(hollowLayer) {
        onDispose { hollowLayer?.release() }
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

                        hollowLayer = HollowLayer(style)

                        mlMap.addOnMapClickListener { latLng ->
                            val p: PointF = mlMap.projection.toScreenLocation(latLng)
                            val r = 30f
                            val rect = RectF(p.x - r, p.y - r, p.x + r, p.y + r)
                            val hit = mlMap.queryRenderedFeatures(rect, HollowLayer.CORE)
                                .firstOrNull()?.getStringProperty(HollowLayer.PROP_ID)
                            if (hit != null) {
                                selectedId = hit
                                true
                            } else {
                                false
                            }
                        }

                        DebugLog.log("Map: style loaded, location + hollow layers ready")
                        map = mlMap
                    }
                }
                mapView
            }
        )

        // Radar sweep overlay (drawn over the map, locked to the user).
        val currentMap = map
        val loc = lastLocation
        if (radarActive && currentMap != null && loc != null) {
            RadarOverlay(
                map = currentMap,
                userLat = loc.latitude,
                userLon = loc.longitude,
                radiusM = radarRadiusM,
                objects = radarList,
                modifier = Modifier.fillMaxSize(),
            )
        }

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

        RadarControls(
            active = radarActive,
            radiusM = radarRadiusM,
            onStart = { r ->
                radarRadiusM = r
                radarActive = true
                map?.let { recenterOnUser(it, lastLocation) }
            },
            onStop = { radarActive = false },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )

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

    val selected = hollows.find { it.id == selectedId }
    if (selectedId != null && selected != null) {
        HollowDetailsSheet(
            hollow = selected,
            distanceM = distanceTo(selected),
            onDismiss = { selectedId = null },
        )
    }
}

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
