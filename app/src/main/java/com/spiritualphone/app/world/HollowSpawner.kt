package com.spiritualphone.app.world

import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.model.HollowInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Hollow "world" engine. Spawns Hollow events at random coordinates within
 * [SpawnConfig.RADIUS_M] of the user, drifts them over time, and despawns them
 * when their lifetime ends (retreated / slain by a Shinigami).
 *
 * The radius is always anchored to the user's current coordinates supplied to
 * [run]; everything stays relative to wherever the user is. Emits the live set
 * of Hollows via [hollows]; [onSpawn] fires once per new Hollow (notification +
 * sound hook).
 */
class HollowSpawner {

    private val _hollows = MutableStateFlow<List<Hollow>>(emptyList())
    val hollows: StateFlow<List<Hollow>> = _hollows.asStateFlow()

    var onSpawn: ((Hollow) -> Unit)? = null

    private val rnd = Random.Default
    private var lastSpawnMs = 0L

    /** Restore a previously saved set of Hollows before the simulation starts. */
    fun seed(initial: List<Hollow>) {
        if (initial.isNotEmpty()) _hollows.value = initial
        lastSpawnMs = System.currentTimeMillis()
    }

    /** Drives the simulation; [center] returns the user's current lat/lon. */
    suspend fun simulate(center: () -> Pair<Double, Double>?) {
        while (true) {
            center()?.let { (lat, lon) -> tick(lat, lon) }
            delay(SpawnConfig.TICK_MS)
        }
    }

    private fun tick(centerLat: Double, centerLon: Double) {
        val now = System.currentTimeMillis()
        val moved = _hollows.value.mapNotNull { advance(it, centerLat, centerLon, now) }

        val canSpawn = moved.size < SpawnConfig.MAX_ACTIVE &&
            now - lastSpawnMs >= SpawnConfig.SPAWN_INTERVAL_MS
        _hollows.value = if (canSpawn) {
            lastSpawnMs = now
            val hollow = spawn(centerLat, centerLon, now)
            onSpawn?.invoke(hollow)
            moved + hollow
        } else {
            moved
        }
    }

    private fun spawn(centerLat: Double, centerLon: Double, now: Long): Hollow {
        val (lat, lon) = GeoMath.randomPointInRadius(centerLat, centerLon, SpawnConfig.RADIUS_M, rnd)
        return Hollow(
            id = UUID.randomUUID().toString(),
            lat = lat,
            lon = lon,
            headingDeg = rnd.nextDouble() * 360.0,
            bornAtMs = now,
            lifetimeMs = rnd.nextLong(SpawnConfig.LIFETIME_MIN_MS, SpawnConfig.LIFETIME_MAX_MS),
            info = HollowInfo(),
        )
    }

    /** Move one Hollow a tick forward; return null to despawn it. */
    private fun advance(h: Hollow, centerLat: Double, centerLon: Double, now: Long): Hollow? {
        if (now - h.bornAtMs >= h.lifetimeMs) return null

        var heading = h.headingDeg
        if (rnd.nextDouble() < SpawnConfig.HEADING_CHANGE_CHANCE) {
            heading = rnd.nextDouble() * 360.0
        }
        val step = SpawnConfig.SPEED_MPS * (SpawnConfig.TICK_MS / 1000.0)
        var (lat, lon) = GeoMath.destination(h.lat, h.lon, heading, step)

        // Keep the Hollow inside the radius: if it would leave, steer back.
        if (GeoMath.distanceM(centerLat, centerLon, lat, lon) > SpawnConfig.RADIUS_M) {
            heading = bearing(h.lat, h.lon, centerLat, centerLon)
            val back = GeoMath.destination(h.lat, h.lon, heading, step)
            lat = back.first
            lon = back.second
        }
        return h.copy(lat = lat, lon = lon, headingDeg = heading)
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
