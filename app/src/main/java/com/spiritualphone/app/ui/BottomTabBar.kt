package com.spiritualphone.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The three top-level sections. Map is the default (centre). */
enum class AppTab(val label: String, val icon: ImageVector) {
    Profile("Профиль", Icons.Filled.Person),
    Map("Карта", Icons.Filled.Map),
    Ar("AR", Icons.Filled.ViewInAr),
}

private val BAR_TINT = Color(0xCC000000)    // black @ 0.80
private val PILL = Color(0x24FFFFFF)        // translucent white "glass" pill
private val SELECTED = Color(0xFFFFFFFF)
private val UNSELECTED = Color(0xFF8E8E93)

/**
 * Custom bottom tab bar with a draggable "Liquid-Glass-like" pill: the pill
 * follows the finger and the section switches only when released (snapping to
 * the nearest tab); tapping switches immediately.
 *
 * The pill's position is a single continuous [Animatable] (in px) — during a
 * drag we snapTo it, on release we animateTo the target centre from wherever
 * the finger left it. There is no second "rest" position, so it never jerks
 * back to the old tab before settling.
 */
@Composable
fun BottomTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val tabs = AppTab.entries
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    Box(modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .glass(shape = RoundedCornerShape(32.dp), tint = BAR_TINT)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val slotPx = widthPx / tabs.size
            val edgePx = with(density) { 8.dp.toPx() }
            val pillWidthPx = slotPx - with(density) { 16.dp.toPx() }
            val index = tabs.indexOf(selected)

            fun centerFor(i: Int) = slotPx * i + slotPx / 2f
            val minCenter = pillWidthPx / 2f + edgePx
            val maxCenter = widthPx - pillWidthPx / 2f - edgePx

            // Single source of truth for the pill's horizontal centre (px).
            val pillCenter = remember { Animatable(centerFor(index)) }
            // Glide to the selected tab on tap / external change / resize.
            LaunchedEffect(index, slotPx) { pillCenter.animateTo(centerFor(index)) }

            val leftPx = (pillCenter.value - pillWidthPx / 2f)
                .coerceIn(edgePx, widthPx - pillWidthPx - edgePx)

            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .offset { IntOffset(leftPx.roundToInt(), 0) }
                    .width(with(density) { pillWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PILL)
            )

            Row(
                Modifier
                    .fillMaxSize()
                    .pointerInput(tabs.size, widthPx, index) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                val target = (pillCenter.value + drag.x).coerceIn(minCenter, maxCenter)
                                scope.launch { pillCenter.snapTo(target) }
                            },
                            onDragEnd = {
                                val idx = (pillCenter.value / slotPx).toInt().coerceIn(0, tabs.size - 1)
                                if (idx == index) {
                                    scope.launch { pillCenter.animateTo(centerFor(idx)) }
                                } else {
                                    onSelect(tabs[idx])  // LaunchedEffect(index) animates the rest
                                }
                            },
                            onDragCancel = {
                                scope.launch { pillCenter.animateTo(centerFor(index)) }
                            },
                        )
                    },
            ) {
                tabs.forEach { tab ->
                    val tint = if (tab == selected) SELECTED else UNSELECTED
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
                        if (showLabels) {
                            Spacer(Modifier.height(3.dp))
                            Text(tab.label, color = tint, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
