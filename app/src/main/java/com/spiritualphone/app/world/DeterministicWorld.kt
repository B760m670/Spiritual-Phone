package com.spiritualphone.app.world

import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.model.HollowInfo
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

/**
 * The shared Hollow world as a pure function of place and time:
 *
 *     Hollow = f(geo-cell, time)
 *
 * Every device runs this identical algorithm with an identical seed derived
 * from the world grid + UTC time, so two people in the same place at the same
 * moment compute the SAME Hollows. That makes the world "shared" with **no
 * server and no storage** — nothing is persisted, the world is recomputed
 * rather than remembered. Because the future is computable, upcoming events can
 * also be pre-scheduled as notifications while the app is closed.
 *
 * All parameters live here (not in SpawnConfig) because they define the world
 * itself and must be identical on every device for the shared illusion to hold.
 */
object DeterministicWorld {

    /** Grid cell size in degrees of latitude (~1.4 km). */
    private const val CELL_DEG = 0.0125
    private const val METERS_PER_DEG_LAT = 111_320.0

    /** A spawn "epoch": within each, a cell may birth a Hollow. */
    private const val EPOCH_MS = 300_000L          // 5 min

    /** Chance a given cell births a Hollow in a given epoch (tunable density). */
    private const val SPAWN_CHANCE = 0.18

    private const val LIFETIME_MIN_MS = 120_000L
    private const val LIFETIME_MAX_MS = 360_000L

    private const val SPEED_MPS = 8.0
    private const val LEG_SEC = 20.0               // heading is held this long
    private const val TURN_DEG = 45.0              // max heading change per leg

    /** All Hollows alive within [radiusM] of the point at wall-clock [nowMs]. */
    fun hollowsNear(lat: Double, lon: Double, radiusM: Double, nowMs: Long): List<Hollow> {
        val result = ArrayList<Hollow>()
        val cellSpanM = CELL_DEG * METERS_PER_DEG_LAT
        val cellReach = ceil(radiusM / cellSpanM).toInt() + 1

        val baseX = floor(lat / CELL_DEG).toLong()
        val baseY = floor(lon / CELL_DEG).toLong()

        // A Hollow born in an earlier epoch may still be alive now, so look back
        // far enough to cover the longest possible lifetime.
        val firstEpoch = floor((nowMs - LIFETIME_MAX_MS).toDouble() / EPOCH_MS).toLong()
        val lastEpoch = floor(nowMs.toDouble() / EPOCH_MS).toLong()

        for (dx in -cellReach..cellReach) {
            for (dy in -cellReach..cellReach) {
                val cx = baseX + dx
                val cy = baseY + dy
                for (epoch in firstEpoch..lastEpoch) {
                    val h = cellEpochHollow(cx, cy, epoch, nowMs) ?: continue
                    if (GeoMath.distanceM(lat, lon, h.lat, h.lon) <= radiusM) result.add(h)
                }
            }
        }
        return result
    }

    /** The Hollow (if any) born by cell [cx],[cy] during [epoch], at time [nowMs]. */
    private fun cellEpochHollow(cx: Long, cy: Long, epoch: Long, nowMs: Long): Hollow? {
        val seed = seedFor(cx, cy, epoch)
        val rnd = Random(seed)
        if (rnd.nextDouble() >= SPAWN_CHANCE) return null

        val spawnLat = (cx + rnd.nextDouble()) * CELL_DEG
        val spawnLon = (cy + rnd.nextDouble()) * CELL_DEG
        val bornAt = epoch * EPOCH_MS + (rnd.nextDouble() * EPOCH_MS).toLong()
        val lifetime = LIFETIME_MIN_MS +
            (rnd.nextDouble() * (LIFETIME_MAX_MS - LIFETIME_MIN_MS)).toLong()

        if (nowMs < bornAt || nowMs >= bornAt + lifetime) return null

        val (lat, lon, heading) = positionAt(spawnLat, spawnLon, seed, nowMs - bornAt)
        return Hollow(
            id = "h_${cx}_${cy}_$epoch",
            lat = lat,
            lon = lon,
            headingDeg = heading,
            bornAtMs = bornAt,
            lifetimeMs = lifetime,
            info = HollowInfo(),
        )
    }

    /** Deterministic smooth drift from the spawn point over [elapsedMs]. */
    private fun positionAt(
        startLat: Double,
        startLon: Double,
        seed: Long,
        elapsedMs: Long,
    ): Triple<Double, Double, Double> {
        val walk = Random(seed xor 0x5DEECE66DL)
        var lat = startLat
        var lon = startLon
        var heading = walk.nextDouble() * 360.0
        var remainingSec = elapsedMs / 1000.0
        while (remainingSec > 0) {
            val t = minOf(LEG_SEC, remainingSec)
            val (nlat, nlon) = GeoMath.destination(lat, lon, heading, SPEED_MPS * t)
            lat = nlat
            lon = nlon
            heading = (heading + (walk.nextDouble() - 0.5) * 2 * TURN_DEG + 360.0) % 360.0
            remainingSec -= t
        }
        return Triple(lat, lon, heading)
    }

    /** Stable 64-bit seed for a (cell, epoch) triple — SplitMix64 mixing. */
    private fun seedFor(a: Long, b: Long, c: Long): Long {
        var h = -0x61c8864680b583ebL                       // golden ratio
        h = splitmix(h xor a)
        h = splitmix(h xor (b * 0x9E3779B97F4A7C15uL.toLong()))
        h = splitmix(h xor c)
        return h
    }

    private fun splitmix(x: Long): Long {
        var z = x + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}
