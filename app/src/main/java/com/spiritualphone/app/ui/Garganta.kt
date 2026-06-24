package com.spiritualphone.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.sin

/**
 * A Garganta — a tear in the sky revealing the void. Pure Compose Canvas (no
 * runtime shader, works on every device): a jagged black rip with glowing
 * white/purple reiatsu edges, slow swirling dark matter inside, crackling
 * discharges along the rim, and an opening animation. Stylised but reads as a
 * "crack in the sky" over the live camera.
 */
@Composable
fun Garganta(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "garganta")
    val twoPi = (2f * PI).toFloat()
    val phase by t.animateFloat(
        0f, twoPi, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "phase",
    )
    val flicker by t.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse), label = "flicker",
    )
    val swirl by t.animateFloat(
        0f, twoPi, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "swirl",
    )

    // One-shot opening.
    val open = remember { Animatable(0f) }
    LaunchedEffect(Unit) { open.animateTo(1f, tween(1500)) }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val top = h * 0.14f
        val bot = h * 0.86f
        val span = bot - top
        val cyMid = (top + bot) / 2f
        val maxHalf = w * 0.24f * open.value
        val jagAmp = w * 0.045f
        val steps = 46

        fun noise(tt: Float): Float =
            sin(tt * 9f + phase) * 0.5f +
                sin(tt * 23f - phase * 1.7f) * 0.3f +
                sin(tt * 47f + phase * 0.6f) * 0.2f

        // Half-width of the rip at height fraction tt (0..1); tapers to sharp tips.
        fun halfAt(tt: Float): Float {
            val taper = sin(PI.toFloat() * tt)
            return taper * (maxHalf + jagAmp * noise(tt))
        }

        val path = Path().apply {
            moveTo(cx, top)
            for (i in 0..steps) {
                val tt = i / steps.toFloat()
                lineTo(cx + halfAt(tt), top + span * tt)
            }
            for (i in steps downTo 0) {
                val tt = i / steps.toFloat()
                lineTo(cx - halfAt(tt), top + span * tt)
            }
            close()
        }

        // The void: absolute black with a faint dark-matter tint near the rim.
        drawPath(
            path,
            brush = Brush.radialGradient(
                colors = listOf(Color.Black, Color.Black, Color(0xFF14001F)),
                center = Offset(cx, cyMid),
                radius = (maxHalf * 4f).coerceAtLeast(1f),
            ),
        )

        // Slow swirling dark matter, clipped inside the rip.
        clipPath(path) {
            repeat(2) { k ->
                val a = swirl + k * PI.toFloat()
                val ox = cx + sin(a) * maxHalf * 0.45f
                val oy = cyMid + sin(a * 1.3f) * span * 0.18f
                val r = (maxHalf * 1.1f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x55351055), Color.Transparent),
                        center = Offset(ox, oy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(ox, oy),
                )
            }
        }

        // Glowing reiatsu edges (layered strokes = soft glow), flickering.
        drawPath(path, color = Color.White.copy(alpha = 0.10f * flicker), style = Stroke(width = 16f))
        drawPath(path, color = Color(0xFFB388FF).copy(alpha = 0.30f * flicker), style = Stroke(width = 7f))
        drawPath(path, color = Color.White.copy(alpha = 0.90f * flicker), style = Stroke(width = 2.5f))

        // Crackling discharges along the rim.
        val bolts = 5
        for (b in 0 until bolts) {
            val tt = 0.2f + 0.6f * (b / (bolts - 1f))
            val side = if (b % 2 == 0) 1f else -1f
            val len = w * (0.05f + 0.05f * (0.5f + 0.5f * sin(phase * 2f + b)))
            var px = cx + side * halfAt(tt)
            var py = top + span * tt
            val bolt = Path().apply {
                moveTo(px, py)
                for (s in 1..4) {
                    px += side * (len / 4f)
                    py += noise(tt + s * 0.1f + b) * h * 0.02f
                    lineTo(px, py)
                }
            }
            val a = 0.5f * flicker * (0.5f + 0.5f * sin(phase * 3f + b))
            drawPath(bolt, color = Color(0xFFB388FF).copy(alpha = a), style = Stroke(width = 2f))
        }
    }
}
