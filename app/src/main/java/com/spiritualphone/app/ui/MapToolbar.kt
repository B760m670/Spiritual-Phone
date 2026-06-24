package com.spiritualphone.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.world.AlertConfig

// Slightly denser than the profile pills so the controls stay legible over the
// bright map — same dark glass family as the tab bar.
private val TINT = Color(0xB3000000)        // black @ 0.70
private val TOOL = 52.dp

/**
 * Floating map toolbar (top-right). A "Инструменты" button opens a downward
 * drawer with the Radar and My-Location controls. Radar's radius chips open to
 * the left (toward the screen centre) since the button sits on the right edge.
 * All buttons share the tab-bar / profile glass look.
 */
@Composable
fun MapToolbar(
    radarActive: Boolean,
    radarRadiusM: Double,
    onStartRadar: (Double) -> Unit,
    onStopRadar: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var toolsOpen by remember { mutableStateOf(false) }
    var radarExpanded by remember { mutableStateOf(false) }

    Column(modifier, horizontalAlignment = Alignment.End) {
        GlassSquare(Icons.Filled.Build, "Инструменты") {
            toolsOpen = !toolsOpen
            if (!toolsOpen) radarExpanded = false
        }

        AnimatedVisibility(
            visible = toolsOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                // Radar (with radius chips opening to the left).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (radarActive) {
                        GlassPill("Поиск ${(radarRadiusM / 1000).toInt()} км")
                        GlassSquare(Icons.Filled.Close, "Остановить радар") { onStopRadar() }
                    } else {
                        AnimatedVisibility(
                            visible = radarExpanded,
                            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AlertConfig.RADAR_RADII_M.forEach { r ->
                                    GlassChip("${(r / 1000).toInt()} км") {
                                        onStartRadar(r); radarExpanded = false
                                    }
                                }
                            }
                        }
                        GlassSquare(Icons.Filled.Radar, "Радар") { radarExpanded = !radarExpanded }
                    }
                }

                // My location.
                GlassSquare(Icons.Filled.MyLocation, "Моё местоположение") { onRecenter() }
            }
        }
    }
}

@Composable
private fun GlassSquare(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(TOOL)
            .glass(shape = RoundedCornerShape(16.dp), tint = TINT)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun GlassChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .glass(shape = RoundedCornerShape(22.dp), tint = TINT)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassPill(label: String) {
    Box(
        Modifier
            .height(44.dp)
            .glass(shape = RoundedCornerShape(22.dp), tint = TINT)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF39FF14), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
