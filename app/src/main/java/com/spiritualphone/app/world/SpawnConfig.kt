package com.spiritualphone.app.world

/**
 * View-side tuning for sampling the shared world. The world's own rules
 * (density, lifetime, movement) live in [DeterministicWorld]; these only
 * control how the app *observes* it.
 */
object SpawnConfig {
    /** How far around the user we compute and show Hollows (metres). */
    const val RADIUS_M = 10_000.0

    /** How often the nearby world is recomputed (ms). */
    const val TICK_MS = 1_000L

    /** How often to re-query OSM terrain for each Hollow's label (ms). */
    const val TERRAIN_QUERY_INTERVAL_MS = 45_000L
}
