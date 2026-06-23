package com.spiritualphone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Real Liquid-Glass: samples the [hazeState] backdrop (the live map), blurs and
 * tints it, then draws a glass bevel — the native equivalent of iOS
 * `.glassEffect()`, built on Haze (RenderEffect/AGSL under the hood).
 *
 * On surfaces/devices where the backdrop can't be blurred (API < 31, or a
 * backdrop that isn't captured), Haze falls back to [fallbackTint] as a solid
 * frost, so the control still reads as dark glass.
 *
 * The backdrop must be marked with `Modifier.haze(hazeState)` (see the map in
 * SpiritRadarMap) and this child must be drawn on top of it.
 */
fun Modifier.glass(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = Color.Black.copy(alpha = 0.28f),
): Modifier = this
    .clip(shape)
    .hazeChild(
        state = hazeState,
        style = HazeStyle(
            tints = listOf(HazeTint(tint)),
            blurRadius = 24.dp,
            noiseFactor = 0.04f,
            fallbackTint = HazeTint(tint.copy(alpha = (tint.alpha + 0.28f).coerceAtMost(1f))),
        ),
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.06f)),
        ),
        shape = shape,
    )

/**
 * Approximates Apple's "Liquid Glass" material with built-in Compose only
 * (no extra dependency): a translucent tint, a diagonal specular sheen, and a
 * bright top-left edge that reads as a glass bevel.
 *
 * True Liquid Glass on iOS samples and refracts the pixels *behind* the
 * surface in real time. Compose has no built-in backdrop filter, so a faithful
 * version needs a blur library (e.g. Haze, chrisbanes/haze) or a custom
 * RenderNode. This modifier is the dependency-free look; the structure (clip →
 * tint → sheen → edge) is the same place where a real backdrop blur would slot
 * in later.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = Color.White.copy(alpha = 0.06f),
): Modifier = this
    .clip(shape)
    .background(tint, shape)
    .background(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f)),
        ),
        shape = shape,
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.06f)),
        ),
        shape = shape,
    )
