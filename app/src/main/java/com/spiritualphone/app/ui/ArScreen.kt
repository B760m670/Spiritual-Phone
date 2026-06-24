package com.spiritualphone.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.spiritualphone.app.location.LocationProvider
import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.world.AlertConfig
import com.spiritualphone.app.world.DeterministicWorld
import com.spiritualphone.app.world.GeoMath
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Horizontal field of view (deg) we map markers across — roughly a phone cam. */
private const val FOV_DEG = 60f

/** Fixed elevation of the sky-anchored Garganta, degrees above the horizon. */
private const val SKY_ELEVATION_DEG = 45f

/**
 * AR section: a live camera preview with nearby Hollows anchored by real-world
 * compass bearing (device azimuth from the rotation-vector sensor + GPS bearing
 * to each Hollow). No ARCore — a lightweight geo-AR overlay that works on any
 * device with a camera + compass.
 */
@Composable
fun ArScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (!granted) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("AR-режим", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Нужен доступ к камере, чтобы видеть Пустых поверх реального мира.",
                    color = Color(0xFF8E8E93), fontSize = 14.sp,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Разрешить камеру")
                }
            }
        } else {
            ArView()
        }
    }
}

@Composable
private fun ArView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera preview. COMPATIBLE = TextureView, so the lensing RenderEffect can
    // sample the camera pixels.
    val previewView = remember {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Device orientation from the rotation-vector sensor: azimuth (compass) where
    // the camera points, and pitch (how far up/down the camera is aimed).
    var azimuth by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            val rotation = FloatArray(9)
            val remapped = FloatArray(9)
            val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                // Remap so the result is referenced to the camera direction when
                // the phone is held upright.
                SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped,
                )
                SensorManager.getOrientation(remapped, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                // Camera elevation: 0° at the horizon, +90° straight up.
                pitch = -Math.toDegrees(orientation[1].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    // Anchor the rupture to a fixed point in the sky: its azimuth is captured the
    // first time we get a real heading (the way you face on entry), its elevation
    // is fixed high above the horizon. Turn / look up to find it.
    var targetAz by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(azimuth) {
        if (targetAz == null && azimuth != 0f) targetAz = azimuth
    }
    val targetElevation = SKY_ELEVATION_DEG

    // Vertical FOV from the horizontal one and the screen aspect.
    val config = LocalConfiguration.current
    val vFov = FOV_DEG * (config.screenHeightDp.toFloat() / config.screenWidthDp.toFloat())

    // Screen-space centre of the rupture, as a fraction of the view (0.5,0.5 = mid).
    val center = {
        val tz = targetAz
        if (tz == null) {
            Offset(0.5f, 0.5f)
        } else {
            val dAz = ((tz - azimuth + 540f) % 360f) - 180f          // -180..180, +right
            val dEl = targetElevation - pitch                         // + = target above aim
            Offset(0.5f + dAz / FOV_DEG, 0.5f - dEl / vFov)
        }
    }

    // Garganta (dev): auto-cycle — cut → open → hold → collapse → repeat.
    val gargantaOpen = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            gargantaOpen.snapTo(0f)
            gargantaOpen.animateTo(1f, tween(2600, easing = FastOutSlowInEasing))
            delay(1600)
            gargantaOpen.animateTo(0f, tween(1800, easing = FastOutSlowInEasing))
            delay(1400)
        }
    }

    // The camera, wrapped by the lensing effect anchored to the sky.
    GargantaLens(
        open = { gargantaOpen.value },
        center = center,
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }

    // Guide arrow toward the rupture while it's off-screen.
    Canvas(Modifier.fillMaxSize()) {
        val c = center()
        val onScreen = c.x in 0.10f..0.90f && c.y in 0.12f..0.88f
        if (!onScreen) {
            val ang = atan2(c.y - 0.5f, c.x - 0.5f)
            val px = size.width * 0.5f + cos(ang) * size.width * 0.34f
            val py = size.height * 0.5f + sin(ang) * size.height * 0.34f
            val s = 44f
            val tip = Offset(px + cos(ang) * s, py + sin(ang) * s)
            val left = Offset(px + cos(ang + 2.5f) * s, py + sin(ang + 2.5f) * s)
            val right = Offset(px + cos(ang - 2.5f) * s, py + sin(ang - 2.5f) * s)
            val path = Path().apply {
                moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(right.x, right.y); close()
            }
            drawPath(path, Color(0xDD66B3FF))
        }
    }

    // Location + nearby Hollows (deterministic world, same as the map).
    val locationProvider = remember { LocationProvider(context) }
    var loc by remember { mutableStateOf<Location?>(null) }
    var hollows by remember { mutableStateOf<List<Hollow>>(emptyList()) }
    LaunchedEffect(Unit) { locationProvider.locationUpdates().collect { loc = it } }
    LaunchedEffect(Unit) {
        while (true) {
            loc?.let { l ->
                hollows = DeterministicWorld.hollowsNear(
                    l.latitude, l.longitude, AlertConfig.ALERT_RADIUS_M, System.currentTimeMillis(),
                )
            }
            delay(1000)
        }
    }

    // Marker overlay: place each Hollow horizontally by (bearing − azimuth).
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val current = loc
        if (current != null) {
            hollows.forEach { h ->
                val bearing = GeoMath.bearing(current.latitude, current.longitude, h.lat, h.lon).toFloat()
                val diff = ((bearing - azimuth + 540f) % 360f) - 180f   // -180..180
                if (abs(diff) <= FOV_DEG / 2f) {
                    val frac = 0.5f + diff / FOV_DEG                     // 0..1 across width
                    val distM = GeoMath.distanceM(current.latitude, current.longitude, h.lat, h.lon)
                    Column(
                        Modifier
                            .offset(x = maxWidth * frac - 28.dp, y = maxHeight / 2 - 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color(0xCCE53935)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xB3000000))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text("${distM.roundToInt()} м", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
