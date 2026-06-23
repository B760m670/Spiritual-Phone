package com.spiritualphone.app.world

import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.model.HollowInfo
import com.spiritualphone.app.model.TerrainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Hollow "world" engine. Spawns Hollow events at random coordinates within
 * [SpawnConfig.RADIUS_M] of the user, drifts them over time, and despawns them
 * when their lifetime ends.
 *
 * Spawn distribution is bimodal: [SpawnConfig.NEAR_SPAWN_CHANCE] of events
 * land inside [SpawnConfig.NEAR_RADIUS_M] (so the 1.2 km alert ring always
 * has candidates), the rest are spread over the full radius.
 *
 * Every [SpawnConfig.TERRAIN_QUERY_INTERVAL_MS] each Hollow's coordinates are
 * sent to the Overpass API to learn what OSM feature is underneath. The result
 * changes how the Hollow moves: water → flee toward the soul, residential →
 * hunt the soul, forest → disoriented wandering.
 */
class HollowSpawner {

    private val _hollows = MutableStateFlow<List<Hollow>>(emptyList())
    val hollows: StateFlow<List<Hollow>> = _hollows.asStateFlow()

    var onSpawn: ((Hollow) -> Unit)? = null

    private val rnd = Random.Default
    private var lastSpawnMs = 0L

    // Tracks when each Hollow last had its terrain queried (keyed by id).
    // Lives only for the duration of the simulation — not persisted.
    private val lastTerrainQueryMs = HashMap<String, Long>()

    /** Restore a previously saved set of Hollows before the simulation starts. */
    fun seed(initial: List<Hollow>) {
        if (initial.isNotEmpty()) _hollows.value = initial
        lastSpawnMs = System.currentTimeMillis()
    }

    /**
     * Drives the simulation; [center] returns the user's current lat/lon.
     * Terrain queries are launched as child coroutines within a supervisorScope
     * so individual network failures never crash the simulation loop.
     */
    suspend fun simulate(center: () -> Pair<Double, Double>?) = supervisorScope {
        while (true) {
            center()?.let { (lat, lon) -> tick(lat, lon, this) }
            delay(SpawnConfig.TICK_MS)
        }
    }

    private fun tick(centerLat: Double, centerLon: Double, scope: CoroutineScope) {
        val now = System.currentTimeMillis()
        val moved = _hollows.value.mapNotNull { advance(it, centerLat, centerLon, now) }

        // Remove terrain tracking for hollows that just despawned.
        val activeIds = moved.map { it.id }.toHashSet()
        lastTerrainQueryMs.keys.retainAll(activeIds)

        // Queue terrain queries for each hollow due for a refresh.
        moved.forEach { h ->
            val lastQuery = lastTerrainQueryMs.getOrDefault(h.id, 0L)
            if (now - lastQuery >= SpawnConfig.TERRAIN_QUERY_INTERVAL_MS) {
                lastTerrainQueryMs[h.id] = now
                scope.launch { queryAndApplyTerrain(h.id, h.lat, h.lon) }
            }
        }

        val canSpawn = moved.size < SpawnConfig.MAX_ACTIVE &&
            now - lastSpawnMs >= SpawnConfig.SPAWN_INTERVAL_MS
        _hollows.value = if (canSpawn) {
            lastSpawnMs = now
            val hollow = spawn(centerLat, centerLon, now)
            // Trigger immediate terrain query for the fresh hollow.
            lastTerrainQueryMs[hollow.id] = now
            scope.launch { queryAndApplyTerrain(hollow.id, hollow.lat, hollow.lon) }
            onSpawn?.invoke(hollow)
            moved + hollow
        } else {
            moved
        }
    }

    /** Fetch terrain from OSM and patch the matching Hollow in the live list. */
    private suspend fun queryAndApplyTerrain(id: String, lat: Double, lon: Double) {
        val terrain = TerrainQuery.queryAt(lat, lon)
        _hollows.value = _hollows.value.map { h ->
            if (h.id == id) h.copy(terrainType = terrain) else h
        }
    }

    private fun spawn(centerLat: Double, centerLon: Double, now: Long): Hollow {
        // Bimodal distribution: NEAR_SPAWN_CHANCE of hollows appear inside
        // NEAR_RADIUS_M so the alert zone always has candidates; the remainder
        // are scattered over the full radius.
        val radius = if (rnd.nextDouble() < SpawnConfig.NEAR_SPAWN_CHANCE) {
            SpawnConfig.NEAR_RADIUS_M
        } else {
            SpawnConfig.RADIUS_M
        }
        val (lat, lon) = GeoMath.randomPointInRadius(centerLat, centerLon, radius, rnd)
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

        // Terrain-aware steering:
        when (h.terrainType) {
            TerrainType.WATER -> {
                // Hollows cannot cross water — steer toward the soul (user) immediately.
                heading = bearing(h.lat, h.lon, centerLat, centerLon)
                DebugLog.log("Hollow ${h.id.take(6)} fleeing water → soul")
            }
            TerrainType.RESIDENTIAL -> {
                // Hunting mode: frequently steer toward the soul, rarely wander.
                heading = when {
                    rnd.nextDouble() < 0.40 -> bearing(h.lat, h.lon, centerLat, centerLon)
                    rnd.nextDouble() < SpawnConfig.HEADING_CHANGE_CHANCE -> rnd.nextDouble() * 360.0
                    else -> heading
                }
            }
            TerrainType.FOREST -> {
                // Disoriented in forest — direction changes much more often.
                if (rnd.nextDouble() < SpawnConfig.HEADING_CHANGE_CHANCE * 2.5) {
                    heading = rnd.nextDouble() * 360.0
                }
            }
            TerrainType.INDUSTRIAL -> {
                // Few souls here — drifts toward user at low probability.
                heading = when {
                    rnd.nextDouble() < 0.15 -> bearing(h.lat, h.lon, centerLat, centerLon)
                    rnd.nextDouble() < SpawnConfig.HEADING_CHANGE_CHANCE -> rnd.nextDouble() * 360.0
                    else -> heading
                }
            }
            TerrainType.OPEN -> {
                if (rnd.nextDouble() < SpawnConfig.HEADING_CHANGE_CHANCE) {
                    heading = rnd.nextDouble() * 360.0
                }
            }
        }

        val step = SpawnConfig.SPEED_MPS * (SpawnConfig.TICK_MS / 1000.0)
        var (lat, lon) = GeoMath.destination(h.lat, h.lon, heading, step)

        // Keep the Hollow inside the spawn radius: if it would leave, steer back.
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
