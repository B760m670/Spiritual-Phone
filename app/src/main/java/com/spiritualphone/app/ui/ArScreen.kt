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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

/** Horizontal field of view (deg) we map markers across — roughly a phone cam. */
private const val FOV_DEG = 60f

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

    // Camera preview.
    val previewView = remember { PreviewView(context) }
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
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    // Device azimuth (where the camera points) from the rotation-vector sensor.
    var azimuth by remember { mutableStateOf(0f) }
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
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
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
