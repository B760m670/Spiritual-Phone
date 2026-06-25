package com.spiritualphone.app.world

import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.model.TerrainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Queries the public Overpass API (OSM) for the terrain type at a point.
 * Used by HollowSpawner to steer Hollows according to the real world:
 * avoid water, hunt souls in residential areas, wander in forests.
 *
 * All network I/O runs on Dispatchers.IO; callers may be on any thread.
 */
object TerrainQuery {

    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    private const val RADIUS = 80      // metres around the sample point
    private const val TIMEOUT_MS = 7_000

    suspend fun queryAt(lat: Double, lon: Double): TerrainType = withContext(Dispatchers.IO) {
        runCatching {
            val raw = post(buildQuery(lat, lon))
            parse(raw).also { DebugLog.log("TerrainQuery (${lat.fmt()},${lon.fmt()}) → ${it.ruName}") }
        }.getOrElse { e ->
            DebugLog.log("TerrainQuery failed: ${e.message}")
            TerrainType.OPEN
        }
    }

    private fun buildQuery(lat: Double, lon: Double) = """
        [out:json][timeout:5];
        (
          way(around:$RADIUS,$lat,$lon)["landuse"];
          way(around:$RADIUS,$lat,$lon)["natural"];
          way(around:$RADIUS,$lat,$lon)["waterway"];
          way(around:$RADIUS,$lat,$lon)["building"];
          relation(around:$RADIUS,$lat,$lon)["natural"];
          relation(around:$RADIUS,$lat,$lon)["landuse"];
        );
        out tags;
    """.trimIndent()

    private fun post(query: String): String {
        val url = URL(ENDPOINT)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val body = "data=${URLEncoder.encode(query, "UTF-8")}".toByteArray()
        conn.outputStream.use { it.write(body) }
        return conn.inputStream.use { it.bufferedReader().readText() }
    }

    private fun parse(json: String): TerrainType {
        val elements = JSONObject(json).getJSONArray("elements")

        var hasWater = false
        var hasResidential = false
        var hasForest = false
        var hasIndustrial = false

        for (i in 0 until elements.length()) {
            val tags = elements.getJSONObject(i).optJSONObject("tags") ?: continue
            val natural  = tags.optString("natural")
            val landuse  = tags.optString("landuse")
            val waterway = tags.optString("waterway")
            val building = tags.optString("building")

            if (natural  in WATER_NATURAL || waterway.isNotEmpty()) hasWater = true
            if (natural  in FOREST_NATURAL || landuse in FOREST_LANDUSE) hasForest = true
            if (landuse  in RESIDENTIAL_LANDUSE || building.isNotEmpty()) hasResidential = true
            if (landuse  in INDUSTRIAL_LANDUSE) hasIndustrial = true
        }

        return when {
            hasWater       -> TerrainType.WATER
            hasResidential -> TerrainType.RESIDENTIAL
            hasForest      -> TerrainType.FOREST
            hasIndustrial  -> TerrainType.INDUSTRIAL
            else           -> TerrainType.OPEN
        }
    }

    private val WATER_NATURAL       = setOf("water", "wetland", "bay", "glacier", "coastline")
    private val FOREST_NATURAL      = setOf("wood", "forest", "scrub", "heath", "grassland")
    private val FOREST_LANDUSE      = setOf("forest", "meadow", "farmland", "grass", "recreation_ground")
    private val RESIDENTIAL_LANDUSE = setOf("residential", "commercial", "retail", "mixed", "civic")
    private val INDUSTRIAL_LANDUSE  = setOf("industrial", "military", "quarry")

    private fun Double.fmt() = "%.4f".format(this)
}
