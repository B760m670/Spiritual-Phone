package com.spiritualphone.app.world

/**
 * Tunable rules of the Hollow "world". Spawns now cover the full radar range
 * (25 km); only a few are within the close alert radius at any time.
 */
object SpawnConfig {
    /** Hollows may appear anywhere within this radius of the user (metres). */
    const val RADIUS_M = 10_000.0

    /** Simulation tick: how often positions are recomputed (ms). */
    const val TICK_MS = 1_000L

    /** Soonest gap between spawns (ms) while below [MAX_ACTIVE]. */
    const val SPAWN_INTERVAL_MS = 6_000L

    /** Maximum Hollows alive at once (enough for the radar to find > 5). */
    const val MAX_ACTIVE = 14

    /** Hollow drift speed (m/s) — supernatural, wanders seeking souls. */
    const val SPEED_MPS = 8.0

    /** Per-tick chance a Hollow changes heading (0..1). */
    const val HEADING_CHANGE_CHANCE = 0.15

    /** Hollow lifetime range before it despawns (retreats / destroyed), ms. */
    const val LIFETIME_MIN_MS = 120_000L
    const val LIFETIME_MAX_MS = 360_000L
}
