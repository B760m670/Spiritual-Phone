package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.detection.ReiatsuState

private val CALM = Color(0xFF8A90A0)
private val ALERT = Color(0xFFE53935)
private val PANEL = Color(0xCC11151F)

/**
 * Compact reiatsu read-out shown over the map (radar-style). Neutral while
 * calibrating/calm; turns red and names the Hollow class on a detection.
 *
 * Visual is intentionally minimal — the real Bleach-styled UI is designed later.
 */
@Composable
fun ReiatsuIndicator(state: ReiatsuState, modifier: Modifier = Modifier) {
    val text: String
    val color: Color
    when (state) {
        ReiatsuState.Calibrating -> {
            text = "Калибровка реацу…"
            color = CALM
        }
        is ReiatsuState.Reading -> {
            val hollow = state.hollow
            if (hollow == null) {
                text = "Реацу: %.1f µT · спокойно".format(state.deltaUt)
                color = CALM
            } else {
                text = "⚠ ОБНАРУЖЕН: %s · %.1f µT".format(hollow.title, state.deltaUt)
                color = ALERT
            }
        }
    }

    Surface(color = PANEL, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
