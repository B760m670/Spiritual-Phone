package com.spiritualphone.app.world

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pure geodesic helpers (no Android deps) for the Hollow "world": offsetting a
 * coordinate by a bearing/distance, picking a uniform random point inside a
 * radius, and great-circle distance. Used to place and move Hollow events
 * relative to the user's coordinates.
 */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Destination point from [lat]/[lon] going [bearingDeg] for [distanceM]. */
    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceM: Double): Pair<Double, Double> {
        val angular = distanceM / EARTH_RADIUS_M
        val brng = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(lat)
        val lon1 = Math.toRadians(lon)

        val lat2 = asin(sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(brng))
        val lon2 = lon1 + atan2(
            sin(brng) * sin(angular) * cos(lat1),
            cos(angular) - sin(lat1) * sin(lat2),
        )
        return Math.toDegrees(lat2) to Math.toDegrees(lon2)
    }

    /** Uniformly distributed random point within [radiusM] of the centre. */
    fun randomPointInRadius(lat: Double, lon: Double, radiusM: Double, rnd: Random): Pair<Double, Double> {
        // sqrt keeps the distribution uniform over the disc area.
        val distance = radiusM * sqrt(rnd.nextDouble())
        val bearing = rnd.nextDouble() * 360.0
        return destination(lat, lon, bearing, distance)
    }

    /** Initial bearing (degrees, 0..360, 0 = north) from point 1 to point 2. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Great-circle distance in metres between two coordinates. */
    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
