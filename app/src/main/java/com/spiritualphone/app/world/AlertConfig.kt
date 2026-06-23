package com.spiritualphone.app.world

/**
 * Rules for proximity alerts and the radar — kept separate from spawn rules.
 */
object AlertConfig {
    /** Hollows within this distance trigger a notification + sound. All Hollows
     *  are shown on the map; this only gates the close-range alerts. (metres) */
    const val ALERT_RADIUS_M = 700.0

    /** Radar search radii the user can choose (metres). */
    val RADAR_RADII_M = listOf(5_000.0, 10_000.0)

    /** Alarm sounds when more than this many objects are caught by the radar. */
    const val DANGER_COUNT_THRESHOLD = 5
}
