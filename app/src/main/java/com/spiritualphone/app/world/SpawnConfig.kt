package com.spiritualphone.app.world

/**
 * Tunable rules of the Hollow "world" — kept in one place so spawn feel can be
 * adjusted without touching the engine.
 */
object SpawnConfig {
    /** Hollows may appear anywhere within this radius of the user (metres). */
    const val RADIUS_M = 20_000.0

    /** Simulation tick: how often positions are recomputed (ms). */
    const val TICK_MS = 1_000L

    /** Soonest gap between spawns (ms) while below [MAX_ACTIVE]. */
    const val SPAWN_INTERVAL_MS = 25_000L

    /** Maximum Hollows alive at once. */
    const val MAX_ACTIVE = 3

    /** Hollow drift speed (m/s) — supernatural, wanders seeking souls. */
    const val SPEED_MPS = 8.0

    /** Per-tick chance a Hollow changes heading (0..1). */
    const val HEADING_CHANGE_CHANCE = 0.15

    /** Hollow lifetime range before it despawns (retreats / destroyed), ms. */
    const val LIFETIME_MIN_MS = 90_000L
    const val LIFETIME_MAX_MS = 240_000L
}
