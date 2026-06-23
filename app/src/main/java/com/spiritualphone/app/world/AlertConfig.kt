package com.spiritualphone.app.world

/**
 * Rules for proximity alerts and the radar — kept separate from spawn rules.
 */
object AlertConfig {
    /** Hollows within this distance trigger a notification + sound, and are the
     *  only ones shown on the normal map. Others exist but stay hidden until a
     *  radar search reveals them. (metres) */
    const val ALERT_RADIUS_M = 1_000.0

    /** Radar search radii the user can choose (metres). */
    val RADAR_RADII_M = listOf(5_000.0, 15_000.0, 25_000.0)

    /** Alarm sounds when more than this many objects are caught by the radar. */
    const val DANGER_COUNT_THRESHOLD = 5
}
