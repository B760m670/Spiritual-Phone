package com.spiritualphone.app.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.spiritualphone.app.model.Hollow
import com.spiritualphone.app.world.GeoMath
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.hypot

private val RADAR_GREEN = Color(0xFF39FF14)
private val DANGER_RED = Color(0xFFE53935)

/**
 * Real-time radar sweep drawn over the map and locked to the user's coordinates
 * — the range scales with zoom (computed via the map projection), so it reads
 * as part of the map. Objects caught by the radar are drawn as red danger
 * triangles instead of plain dots.
 *
 * Note: the sweep angle animates every frame; the Canvas re-reads the live map
 * projection on each frame, so markers stay glued to their coordinates even as
 * the camera moves.
 */
@Composable
fun RadarOverlay(
    map: MapLibreMap,
    userLat: Double,
    userLon: Double,
    radiusM: Double,
    objects: List<Hollow>,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    Canvas(modifier) {
        val proj = map.projection
        val centerPf = proj.toScreenLocation(LatLng(userLat, userLon))
        val c = Offset(centerPf.x, centerPf.y)

        val edge = GeoMath.destination(userLat, userLon, 0.0, radiusM)
        val edgePf = proj.toScreenLocation(LatLng(edge.first, edge.second))
        val radiusPx = hypot((edgePf.x - centerPf.x).toDouble(), (edgePf.y - centerPf.y).toDouble()).toFloat()
        if (radiusPx <= 1f) return@Canvas

        // Range rings.
        listOf(0.33f, 0.66f, 1f).forEach { f ->
            drawCircle(RADAR_GREEN.copy(alpha = 0.25f), radiusPx * f, c, style = Stroke(2f))
        }
        // Cross-hairs.
        drawLine(RADAR_GREEN.copy(alpha = 0.12f), Offset(c.x - radiusPx, c.y), Offset(c.x + radiusPx, c.y), 1f)
        drawLine(RADAR_GREEN.copy(alpha = 0.12f), Offset(c.x, c.y - radiusPx), Offset(c.x, c.y + radiusPx), 1f)

        // Rotating sweep wedge + leading edge.
        rotate(degrees = sweepDeg, pivot = c) {
            drawArc(
                color = RADAR_GREEN.copy(alpha = 0.18f),
                startAngle = -42f,
                sweepAngle = 42f,
                useCenter = true,
                topLeft = Offset(c.x - radiusPx, c.y - radiusPx),
                size = Size(radiusPx * 2, radiusPx * 2),
            )
            drawLine(RADAR_GREEN, c, Offset(c.x + radiusPx, c.y), 3f)
        }

        // Detected objects as danger triangles.
        objects.forEach { h ->
            val pf = proj.toScreenLocation(LatLng(h.lat, h.lon))
            drawDanger(Offset(pf.x, pf.y))
        }
    }
}

private fun DrawScope.drawDanger(p: Offset) {
    val s = 28f
    val path = Path().apply {
        moveTo(p.x, p.y - s * 0.62f)
        lineTo(p.x - s * 0.55f, p.y + s * 0.45f)
        lineTo(p.x + s * 0.55f, p.y + s * 0.45f)
        close()
    }
    drawPath(path, DANGER_RED)
    drawPath(path, Color.White, style = Stroke(2f))
    // Exclamation mark inside.
    drawLine(Color.White, Offset(p.x, p.y - s * 0.18f), Offset(p.x, p.y + s * 0.1f), 3f)
    drawCircle(Color.White, 2f, Offset(p.x, p.y + s * 0.26f))
}
