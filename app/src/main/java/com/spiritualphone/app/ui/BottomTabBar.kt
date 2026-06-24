package com.spiritualphone.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The three top-level sections. Map is the default (centre). */
enum class AppTab(val label: String, val icon: ImageVector) {
    Profile("Профиль", Icons.Filled.Person),
    Map("Карта", Icons.Filled.Map),
    Ar("AR", Icons.Filled.ViewInAr),
}

// The bar's dark base is fairly opaque (0.80) so the glass stays legible over
// the bright map instead of washing out; over the dark Profile/AR backgrounds
// the difference is negligible (dark on near-black).
private val BAR_TINT = Color(0xCC000000)    // black @ 0.80
private val PILL = Color(0x24FFFFFF)        // translucent white "glass" pill
private val SELECTED = Color(0xFFFFFFFF)
private val UNSELECTED = Color(0xFF8E8E93)

/**
 * Custom bottom tab bar. Same dependency-free glass look as the QR/Изм. pills
 * (tint + sheen + edge — no Haze/RenderEffect, can't crash like a backdrop blur
 * would), with a translucent pill that animates (slides) under the selected tab.
 */
@Composable
fun BottomTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = AppTab.entries
    Box(modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .glass(shape = RoundedCornerShape(32.dp), tint = BAR_TINT)
        ) {
            val slot = maxWidth / tabs.size
            val index = tabs.indexOf(selected)
            val pillOffset by animateDpAsState(targetValue = slot * index + 8.dp, label = "pill")

            // The sliding translucent pill behind the selected tab.
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .offset(x = pillOffset)
                    .width(slot - 16.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PILL)
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSel = tab == selected
                    val tint = if (isSel) SELECTED else UNSELECTED
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(tab.label, color = tint, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
