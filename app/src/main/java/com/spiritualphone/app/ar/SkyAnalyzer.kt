package com.spiritualphone.app.ar

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * CameraX analyzer that produces a sky mask for each (throttled) frame:
 * orient the frame upright, centre-crop it to the on-screen aspect so the mask
 * lines up 1:1 with the preview, then run [SkySegmenter]. Results are delivered
 * via [onResult] on the analyzer thread.
 *
 * [aspect] returns the preview's width/height; segmentation is rate-limited to
 * [minIntervalMs] (sky moves slowly — no need for 60 fps).
 */
class SkyAnalyzer(
    private val segmenter: SkySegmenter,
    private val aspect: () -> Float,
    private val minIntervalMs: Long = 220L,
    private val onResult: (mask: Bitmap?, skyCoverage: Float) -> Unit,
) : ImageAnalysis.Analyzer {

    private var last = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - last < minIntervalMs) {
            image.close()
            return
        }
        last = now
        try {
            val rgba = image.toBitmap()
            val upright = orientAndCrop(rgba, image.imageInfo.rotationDegrees, aspect())
            val mask = segmenter.segment(upright)
            onResult(mask, mask?.let(::coverage) ?: 0f)
            if (upright !== rgba) upright.recycle()
            rgba.recycle()
        } catch (t: Throwable) {
            onResult(null, 0f)
        } finally {
            image.close()
        }
    }

    private fun orientAndCrop(src: Bitmap, rotationDeg: Int, viewAspect: Float): Bitmap {
        val up = if (rotationDeg % 360 != 0) {
            Bitmap.createBitmap(
                src, 0, 0, src.width, src.height,
                Matrix().apply { postRotate(rotationDeg.toFloat()) }, true,
            )
        } else {
            src
        }
        val a = if (viewAspect <= 0f) up.width.toFloat() / up.height else viewAspect
        val upAspect = up.width.toFloat() / up.height
        val crop = if (upAspect > a) {
            val w = (up.height * a).toInt().coerceIn(1, up.width)
            Bitmap.createBitmap(up, (up.width - w) / 2, 0, w, up.height)
        } else {
            val h = (up.width / a).toInt().coerceIn(1, up.height)
            Bitmap.createBitmap(up, 0, (up.height - h) / 2, up.width, h)
        }
        if (up !== src) up.recycle()
        return crop
    }

    private fun coverage(mask: Bitmap): Float {
        val w = mask.width
        val h = mask.height
        val px = IntArray(w * h)
        mask.getPixels(px, 0, w, 0, 0, w, h)
        var sky = 0
        for (p in px) if ((p shr 16 and 0xFF) > 127) sky++
        return sky.toFloat() / px.size
    }
}
