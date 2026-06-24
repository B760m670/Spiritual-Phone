package com.spiritualphone.app.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Physics-grounded Garganta as a spacetime rupture: it doesn't draw over the
 * camera, it *bends* it. The AGSL shader takes the camera frame as an input
 * (`uniform shader content`) and, near the rupture, deflects the sampled
 * coordinates — real gravitational lensing of the sky/clouds — with a smooth
 * elliptical event-horizon shadow (absolute black) and a thin white-blue photon
 * ring at the boundary. No teeth, no reiatsu.
 *
 * Applied as a [RenderEffect] on the layer wrapping the camera, so the camera
 * pixels are the shader input. The camera must render into a TextureView
 * (PreviewView COMPATIBLE) for that capture to work. Android 13+; on older or
 * if the shader fails to compile, [content] is shown unaffected.
 */
@Composable
fun GargantaLens(
    open: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Lensed(open, modifier, content)
    } else {
        Box(modifier, content = content)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Lensed(
    open: () -> Float,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shader = remember { runCatching { RuntimeShader(AGSL_GARGANTA) }.getOrNull() }
    val transition = rememberInfiniteTransition(label = "garganta")
    val time by transition.animateFloat(
        0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "time",
    )

    val mod = if (shader != null) {
        modifier.graphicsLayer {
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uOpen", open().coerceIn(0f, 1f))
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    } else {
        modifier
    }
    Box(mod, content = content)
}

// language=AGSL
private const val AGSL_GARGANTA = """
uniform shader content;
uniform float2 uResolution;
uniform float uTime;
uniform float uOpen;

half4 main(float2 fragCoord) {
    float2 res = uResolution;
    float2 ctr = res * 0.5;
    float ax = res.x * 0.30;                 // horizontal semi-axis (~20 m, dev)
    float ay = ax * 0.5 * uOpen;             // vertical grows with opening (~10 m)

    // Not open yet -> pure camera.
    if (ay < 1.0) {
        return content.eval(fragCoord);
    }

    float2 p = fragCoord - ctr;
    float2 q = float2(p.x / ax, p.y / ay);
    float r = length(q);                     // 1 == on the horizon ellipse
    float plen = length(p);
    float2 dir = plen > 0.001 ? p / plen : float2(1.0, 0.0);

    // Gravitational lensing: deflect inward, strongest just outside the horizon,
    // fading to nothing far away (so the rest of the frame is untouched).
    float lens = 0.0;
    if (r > 1.0) {
        float t = 1.0 / r;                   // 1 at boundary -> 0 far
        lens = pow(t, 3.0) * ax * 0.6;       // px of inward displacement
    }
    float ca = lens * 0.06;                  // chromatic spread, scales with lens

    float2 b = fragCoord - dir * lens;
    half3 bg = half3(0.0);
    bg.r = content.eval(b - dir * ca).r;
    bg.g = content.eval(b).g;
    bg.b = content.eval(b + dir * ca).b;

    // "Thick glass" smear near the rim (extra taps, weighted by lens strength).
    half3 sm = content.eval(b - dir * (lens * 0.25)).rgb +
               content.eval(b + dir * (lens * 0.25)).rgb;
    float smk = clamp(lens / (ax * 0.30), 0.0, 0.6);
    bg = mix(bg, sm * 0.5, smk);

    // Thin white-blue photon ring just outside the boundary.
    float ring = smoothstep(1.10, 1.0, r) - smoothstep(1.0, 0.97, r);
    ring = clamp(ring, 0.0, 1.0);
    half3 ringCol = half3(0.65, 0.82, 1.0);

    // Absolute-black event-horizon shadow inside.
    float shadow = smoothstep(1.0, 0.97, r);

    half3 col = bg + ringCol * ring * 1.6;
    col = mix(col, half3(0.0), shadow);
    return half4(col, 1.0);
}
"""
