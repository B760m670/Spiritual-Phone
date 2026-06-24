package com.spiritualphone.app.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * On-device sky segmentation (ADE20K DeepLabV3, see assets/sky_mask_513.tflite).
 * Given a camera frame it returns a small soft sky-probability mask — used to
 * clip the Garganta so it only renders over real sky, never ceilings/walls.
 *
 * Not thread-safe: call [segment] from a single background thread (the CameraX
 * analyzer executor). Build with [create]; release with [close].
 */
class SkySegmenter private constructor(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    private val inputSize: Int,
    private val outW: Int,
    private val outH: Int,
) {
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(inputSize * inputSize * 3).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(outW * outH * 4).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(inputSize * inputSize)
    private val maskPixels = IntArray(outW * outH)

    /** Runs the model on [frame], returning a [outW]x[outH] sky-probability mask
     *  (probability in every channel). Returns null on failure. */
    fun segment(frame: Bitmap): Bitmap? {
        return try {
            val sameSize = frame.width == inputSize && frame.height == inputSize
            val src = if (sameSize) frame
            else Bitmap.createScaledBitmap(frame, inputSize, inputSize, true)

            src.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            if (!sameSize) src.recycle()
            inputBuffer.rewind()
            for (p in pixels) {
                inputBuffer.put((p shr 16 and 0xFF).toByte())  // R
                inputBuffer.put((p shr 8 and 0xFF).toByte())   // G
                inputBuffer.put((p and 0xFF).toByte())         // B
            }
            inputBuffer.rewind()
            outputBuffer.rewind()
            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val fb = outputBuffer.asFloatBuffer()
            for (i in maskPixels.indices) {
                val v = (fb.get(i).coerceIn(0f, 1f) * 255f).toInt()
                maskPixels[i] = Color.argb(v, v, v, v)
            }
            Bitmap.createBitmap(maskPixels, outW, outH, Bitmap.Config.ARGB_8888)
        } catch (t: Throwable) {
            null
        }
    }

    /** One blank inference to surface unsupported-op / delegate failures at init
     *  rather than silently producing nothing later. */
    private fun warmup() {
        inputBuffer.rewind()
        repeat(inputSize * inputSize * 3) { inputBuffer.put(0) }
        inputBuffer.rewind()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
    }

    fun close() {
        runCatching { interpreter.close() }
        runCatching { gpuDelegate?.close() }
    }

    companion object {
        private const val ASSET = "sky_mask_513.tflite"

        /** Try the GPU delegate first, fall back to multithreaded CPU. Returns
         *  null only if neither can run the model. */
        fun create(context: Context): SkySegmenter? {
            val model = runCatching { loadModel(context) }.getOrNull() ?: return null
            for (useGpu in booleanArrayOf(true, false)) {
                var gpu: GpuDelegate? = null
                try {
                    val options = Interpreter.Options()
                    if (useGpu) {
                        if (!CompatibilityList().isDelegateSupportedOnThisDevice) continue
                        gpu = GpuDelegate()
                        options.addDelegate(gpu)
                    } else {
                        options.numThreads = 4
                    }
                    val interp = Interpreter(model, options)
                    val inShape = interp.getInputTensor(0).shape()    // [1,513,513,3]
                    val outShape = interp.getOutputTensor(0).shape()  // [1,129,129,1]
                    val seg = SkySegmenter(interp, gpu, inShape[1], outShape[2], outShape[1])
                    seg.warmup()
                    return seg
                } catch (t: Throwable) {
                    runCatching { gpu?.close() }
                }
            }
            return null
        }

        private fun loadModel(context: Context): ByteBuffer {
            context.assets.openFd(ASSET).use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    return input.channel.map(
                        FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength,
                    )
                }
            }
        }
    }
}
