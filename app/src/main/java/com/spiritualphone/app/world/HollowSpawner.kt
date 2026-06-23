package com.spiritualphone.app.world

import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.model.TerrainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Drives the live view of the shared world. It does not invent Hollows itself —
 * it samples [DeterministicWorld] around the user every tick, so the set it
 * emits is identical to what any other device at the same place/time computes.
 *
 * On top of the deterministic geometry it layers a real-world terrain label per
 * Hollow (async OSM/Overpass lookup), shown in the details sheet. The terrain
 * is cosmetic — it never changes a Hollow's position, so the world stays
 * perfectly shared even when a device is offline.
 */
class HollowSpawner {

    private val _hollows = MutableStateFlow<List<Hollow>>(emptyList())
    val hollows: StateFlow<List<Hollow>> = _hollows.asStateFlow()

    /** Fires when a Hollow first appears in view (foreground logging hook). */
    var onSpawn: ((Hollow) -> Unit)? = null

    private val terrainCache = HashMap<String, TerrainType>()
    private val lastTerrainQuery = HashMap<String, Long>()
    private var knownIds = emptySet<String>()
    private var started = false

    /** Recomputes the nearby world each tick; [center] is the user's lat/lon. */
    suspend fun simulate(center: () -> Pair<Double, Double>?) = supervisorScope {
        while (true) {
            val c = center()
            if (c != null) {
                val now = System.currentTimeMillis()
                val world = DeterministicWorld
                    .hollowsNear(c.first, c.second, SpawnConfig.RADIUS_M, now)
                    .map { it.copy(terrainType = terrainCache[it.id] ?: TerrainType.OPEN) }

                val ids = world.map { it.id }.toSet()
                if (started) {
                    world.forEach { if (it.id !in knownIds) onSpawn?.invoke(it) }
                }
                knownIds = ids
                started = true

                // Refresh the real terrain label for each Hollow periodically.
                world.forEach { h ->
                    val last = lastTerrainQuery.getOrDefault(h.id, 0L)
                    if (now - last >= SpawnConfig.TERRAIN_QUERY_INTERVAL_MS) {
                        lastTerrainQuery[h.id] = now
                        launch { terrainCache[h.id] = TerrainQuery.queryAt(h.lat, h.lon) }
                    }
                }
                lastTerrainQuery.keys.retainAll(ids)
                terrainCache.keys.retainAll(ids)

                _hollows.value = world
            }
            delay(SpawnConfig.TICK_MS)
        }
    }
}
