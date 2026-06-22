package com.spiritualphone.app.model

/**
 * A Hollow event in the world. The event is anchored to coordinates
 * ([lat]/[lon]) — the red dot on the map is only its indicator. Hollows drift
 * over time ([headingDeg]) and eventually despawn after [lifetimeMs].
 */
data class Hollow(
    val id: String,
    val lat: Double,
    val lon: Double,
    val headingDeg: Double,
    val bornAtMs: Long,
    val lifetimeMs: Long,
    val info: HollowInfo,
)
