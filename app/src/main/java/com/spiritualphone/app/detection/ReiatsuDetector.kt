package com.spiritualphone.app.detection

import kotlin.math.abs

/**
 * Core detection logic: turns a stream of magnetic field magnitudes (µT) into
 * [ReiatsuState].
 *
 * 1. Calibration — averages the first [DetectionConfig.CALIBRATION_SAMPLES]
 *    samples into a calm baseline.
 * 2. After that, Δ = |magnitude − baseline| is the "reiatsu". The class is
 *    derived from Δ via [HollowClass.fromDelta].
 * 3. The baseline keeps drifting slowly (EMA) ONLY while the field is calm, so
 *    real anomalies are not silently absorbed into the baseline.
 *
 * Pure and stateful — no Android dependencies, so it is easy to test/tune.
 */
class ReiatsuDetector(
    private val calibrationSamples: Int = DetectionConfig.CALIBRATION_SAMPLES,
    private val baselineAlpha: Float = DetectionConfig.BASELINE_ALPHA,
    private val noiseFloorUt: Float = DetectionConfig.NOISE_FLOOR_UT,
) {
    private var baseline = 0f
    private var count = 0

    fun reset() {
        baseline = 0f
        count = 0
    }

    fun onSample(magnitude: Float): ReiatsuState {
        count++
        if (count <= calibrationSamples) {
            // Incremental mean of the calm field.
            baseline += (magnitude - baseline) / count
            return ReiatsuState.Calibrating
        }

        val delta = abs(magnitude - baseline)

        // Let the baseline track slow environmental drift, but freeze it during
        // anomalies so a real spike stays a spike.
        if (delta < noiseFloorUt) {
            baseline += baselineAlpha * (magnitude - baseline)
        }

        return ReiatsuState.Reading(deltaUt = delta, hollow = HollowClass.fromDelta(delta))
    }
}
