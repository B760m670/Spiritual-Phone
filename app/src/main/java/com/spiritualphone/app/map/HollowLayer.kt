package com.spiritualphone.app.map

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.animation.LinearInterpolator
import com.spiritualphone.app.model.Hollow
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Renders Hollow events on the map: a small red core dot plus an expanding
 * ripple of the same colour. Markers are bound to the GeoJSON source (real
 * coordinates), so they stay put as the camera moves. Tap hit-testing is done
 * against the [CORE] layer.
 */
class HollowLayer(style: Style) {

    private val source = GeoJsonSource(SOURCE)
    private val ripple = CircleLayer(RIPPLE, SOURCE)
    private val core = CircleLayer(CORE, SOURCE)
    private var animator: ValueAnimator? = null

    init {
        style.addSource(source)

        ripple.setProperties(
            circleColor(RED),
            circleRadius(6f),
            circleOpacity(0.4f),
        )
        core.setProperties(
            circleColor(RED),
            circleRadius(6f),
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(1.5f),
        )
        // Ripple first so the solid core paints on top.
        style.addLayer(ripple)
        style.addLayer(core)

        startRipple()
    }

    fun update(hollows: List<Hollow>) {
        val features = hollows.map { h ->
            Feature.fromGeometry(Point.fromLngLat(h.lon, h.lat)).apply {
                addStringProperty(PROP_ID, h.id)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun startRipple() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                // Expanding wave.
                ripple.setProperties(
                    circleRadius(6f + t * 22f),
                    circleOpacity((1f - t) * 0.45f),
                )
                // Blinking core (мерцание): opacity pulses 0.5 → 1 → 0.5.
                val blink = 0.5f + 0.5f * Math.sin(t * Math.PI).toFloat()
                core.setProperties(circleOpacity(blink))
            }
            start()
        }
    }

    fun release() {
        animator?.cancel()
        animator = null
    }

    companion object {
        const val SOURCE = "hollows"
        const val CORE = "hollow-core"
        const val RIPPLE = "hollow-ripple"
        const val PROP_ID = "id"
        private val RED = Color.parseColor("#E53935")
    }
}
