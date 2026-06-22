package com.spiritualphone.app.detection

import android.content.Context
import com.spiritualphone.app.sensor.MagnetometerSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Glue between the raw magnetometer stream and the detection core. Exposes a
 * single [Flow] of [ReiatsuState] for the UI to collect.
 */
class ReiatsuEngine(context: Context) {
    private val source = MagnetometerSource(context)
    private val detector = ReiatsuDetector()

    val available: Boolean = source.isAvailable()

    fun states(): Flow<ReiatsuState> = source.magnitudes().map { detector.onSample(it) }
}
