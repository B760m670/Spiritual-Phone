package com.spiritualphone.app.world

import android.content.Context
import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.model.HollowInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the live Hollow set to a JSON file so they survive app restarts.
 * Saved on background; on load, Hollows whose lifetime already elapsed (while
 * the app was closed) are dropped.
 */
class HollowStore(context: Context) {

    private val file = File(context.filesDir, "hollows.json")

    fun save(hollows: List<Hollow>) {
        val array = JSONArray()
        hollows.forEach { h ->
            array.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("lat", h.lat)
                    put("lon", h.lon)
                    put("heading", h.headingDeg)
                    put("born", h.bornAtMs)
                    put("life", h.lifetimeMs)
                    put("race", h.info.race)
                    put("count", h.info.count)
                    put("power", h.info.spiritualPower)
                }
            )
        }
        runCatching { file.writeText(array.toString()) }
    }

    fun load(): List<Hollow> {
        if (!file.exists()) return emptyList()
        val now = System.currentTimeMillis()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val born = o.getLong("born")
                val life = o.getLong("life")
                if (now - born >= life) return@mapNotNull null
                Hollow(
                    id = o.getString("id"),
                    lat = o.getDouble("lat"),
                    lon = o.getDouble("lon"),
                    headingDeg = o.getDouble("heading"),
                    bornAtMs = born,
                    lifetimeMs = life,
                    info = HollowInfo(
                        race = o.optString("race", "Пустой"),
                        count = o.optInt("count", 1),
                        spiritualPower = o.optString("power", "Неизвестно"),
                    ),
                )
            }
        }.getOrDefault(emptyList())
    }
}
