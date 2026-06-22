package com.spiritualphone.app.detection

/**
 * Tunable parameters for reiatsu detection — kept in one place so the feel can
 * be adjusted without touching the detection logic.
 */
object DetectionConfig {
    /** Samples collected at startup to learn the calm baseline field. */
    const val CALIBRATION_SAMPLES = 50

    /**
     * Smoothing factor for the slow baseline EMA (0..1). Small = the baseline
     * drifts slowly, so sudden spikes stand out as anomalies instead of being
     * absorbed.
     */
    const val BASELINE_ALPHA = 0.02f

    /** Below this Δ (µT) we treat the field as calm — no detection. */
    val NOISE_FLOOR_UT = HollowClass.ORDINARY.minDeltaUt
}
