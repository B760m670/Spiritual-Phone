package com.spiritualphone.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** The three top-level sections. Map is the default (centre). */
enum class AppTab(val label: String, val icon: ImageVector) {
    Profile("Профиль", Icons.Filled.Person),
    Map("Карта", Icons.Filled.Map),
    Ar("AR", Icons.Filled.ViewInAr),
}

private val PILL = Color(0x24FFFFFF)        // translucent white "glass" pill
private val SELECTED = Color(0xFFFFFFFF)
private val UNSELECTED = Color(0xFF8E8E93)

/**
 * Custom bottom tab bar. The bar itself uses the same dependency-free glass look
 * as the QR/Изм. pills (tint + sheen + edge — no Haze/RenderEffect, so it can't
 * crash like a backdrop blur would). A translucent pill animates (slides) under
 * the selected tab. Icons only, centred — no labels.
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
                .glass(shape = RoundedCornerShape(32.dp), tint = Color.Black.copy(alpha = 0.42f))
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
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelect(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSel) SELECTED else UNSELECTED,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}
