package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.world.AlertConfig

private val RADAR_GREEN = Color(0xFF1B5E20)

/**
 * Radar controls: a button that expands to pick a search radius (5/15/25 km),
 * and, while searching, shows the active range with a stop button.
 */
@Composable
fun RadarControls(
    active: Boolean,
    radiusM: Double,
    onStart: (Double) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        if (active) {
            Surface(color = Color(0xCC0B0E14), shape = RoundedCornerShape(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "📡 Поиск: ${(radiusM / 1000).toInt()} км",
                        color = RADAR_GREEN.let { Color(0xFF39FF14) },
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                    Button(onClick = onStop, modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Остановить")
                        Text(" Стоп")
                    }
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            if (expanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    AlertConfig.RADAR_RADII_M.forEach { r ->
                        Button(onClick = { onStart(r); expanded = false }) {
                            Text("${(r / 1000).toInt()} км")
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = Color(0xFF1B5E20),
            ) {
                Icon(Icons.Filled.Radar, contentDescription = "Радар", tint = Color.White)
            }
        }
    }
}
