package com.spiritualphone.app.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush

/**
 * AGSL (RuntimeShader) Garganta — a real "tear in the sky". The sky is cut by a
 * razor-thin horizontal rip with a torn, toothy edge; driven by [open] (0 = no
 * tear, 1 = fully open) the rip widens vertically into an almond "mouth"; behind
 * it is absolute darkness with a faint, real smoke drift (fbm noise). No glow,
 * no reiatsu — matching the spec.
 *
 * Android 13+ only (AGSL). Shader creation is guarded, so a failure degrades to
 * "draw nothing" rather than crashing.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun GargantaShader(open: () -> Float, modifier: Modifier = Modifier) {
    val shader = remember { runCatching { RuntimeShader(AGSL_GARGANTA) }.getOrNull() }
    val transition = rememberInfiniteTransition(label = "garganta")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing)),
        label = "time",
    )

    Box(
        modifier.drawBehind {
            val s = shader ?: return@drawBehind
            s.setFloatUniform("uResolution", size.width, size.height)
            s.setFloatUniform("uTime", time)
            s.setFloatUniform("uOpen", open().coerceIn(0f, 1f))
            drawRect(ShaderBrush(s))
        },
    )
}

// language=AGSL
private const val AGSL_GARGANTA = """
uniform float2 uResolution;
uniform float uTime;
uniform float uOpen;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        v += amp * vnoise(p);
        p = p * 2.0;
        amp = amp * 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 res = uResolution;
    float2 ctr = res * 0.5;
    float halfW = res.x * 0.42;   // horizontal half-length (~20 m, dev scale)
    float halfH = res.x * 0.22;   // vertical half-opening (~10 m, dev scale)
    float2 p = fragCoord - ctr;

    float x = p.x / halfW;                          // -1..1 across the rip
    float prof = pow(max(0.0, 1.0 - x * x), 0.7);   // almond profile, sharp tips
    float teeth = fbm(float2(x * 9.0 + 3.0, 7.3)) - 0.5;
    float edgeN = (prof + teeth * prof * 0.8) * uOpen;
    float edgePx = edgeN * halfH;

    float dy = edgePx - abs(p.y);
    float a = clamp(dy / 2.0 + 0.5, 0.0, 1.0);      // 1 inside, AA at the rim
    if (x < -1.0 || x > 1.0) {
        a = 0.0;
    }

    // Inside: absolute darkness with a faint real smoke drift.
    float2 uv = p / halfH;
    float smoke = fbm(uv * 2.5 + float2(0.0, uTime * 0.06));
    float g = 0.05 * smoke;

    return half4(g * 0.6 * a, g * 0.55 * a, g * 0.75 * a, a);
}
"""
