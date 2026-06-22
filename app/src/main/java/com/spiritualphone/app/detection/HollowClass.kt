package com.spiritualphone.app.detection

/**
 * Canonical Bleach Hollow classes, ordered weakest → strongest.
 *
 * Each class is reached when the magnetic anomaly (Δ from the calm baseline,
 * in µT) exceeds [minDeltaUt]. Bigger, rarer spikes ⇒ stronger, rarer Hollows —
 * so Vasto Lorde appears only on extreme anomalies, exactly as in the anime.
 *
 * Thresholds are first estimates and meant to be tuned on a real device.
 */
enum class HollowClass(val title: String, val minDeltaUt: Float) {
    ORDINARY("Пустой", 8f),
    GILLIAN("Гиллиан", 20f),
    ADJUCHAS("Адъюкас", 40f),
    VASTO_LORDE("Васто Лорде", 80f);

    companion object {
        /** Highest class whose threshold the anomaly reaches, or null if calm. */
        fun fromDelta(deltaUt: Float): HollowClass? =
            entries.lastOrNull { deltaUt >= it.minDeltaUt }
    }
}
