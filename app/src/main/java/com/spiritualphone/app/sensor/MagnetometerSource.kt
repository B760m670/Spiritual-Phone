package com.spiritualphone.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Wraps the device magnetometer (TYPE_MAGNETIC_FIELD) and emits the magnitude
 * of the geomagnetic field in microtesla (µT) as a cold [Flow].
 *
 * Magnitude = sqrt(x² + y² + z²) — orientation-independent, so it reflects the
 * real field strength regardless of how the phone is held. This is the raw
 * signal the detection core turns into "reiatsu".
 */
class MagnetometerSource(private val context: Context) {

    /** True if this device actually has a magnetometer. */
    fun isAvailable(): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    fun magnitudes(): Flow<Float> = callbackFlow {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                trySend(sqrt(x * x + y * y + z * z))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }
}
