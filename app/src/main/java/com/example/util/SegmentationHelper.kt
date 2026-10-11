package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object SegmentationHelper {

    private val segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(options)
    }

    /**
     * Isolates the person in the bitmap and replaces the background with pure white #FFFFFF
     * (or the requested targetBackgroundColor), preserving smooth edges.
     */
    suspend fun removeBackground(
        sourceBitmap: Bitmap,
        targetBackgroundColor: Int = Color.WHITE
    ): Bitmap = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(sourceBitmap, 0)

        val mask = suspendCancellableCoroutine { continuation ->
            segmenter.process(inputImage)
                .addOnSuccessListener { segmentationMask ->
                    continuation.resume(segmentationMask)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val maskWidth = mask.width
        val maskHeight = mask.height
        val maskBuffer = mask.buffer
        maskBuffer.rewind()

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgR = Color.red(targetBackgroundColor)
        val bgG = Color.green(targetBackgroundColor)
        val bgB = Color.blue(targetBackgroundColor)

        val hasSameDimensions = (maskWidth == width && maskHeight == height)

        if (hasSameDimensions) {
            for (i in 0 until width * height) {
                val confidence = maskBuffer.float
                val originalPixel = pixels[i]

                if (confidence >= 0.75f) {
                    // Full foreground
                    pixels[i] = originalPixel
                } else if (confidence <= 0.25f) {
                    // Full background replacement
                    pixels[i] = targetBackgroundColor
                } else {
                    // Smooth transition / anti-aliasing on hair and silhouettes
                    val blend = (confidence - 0.25f) / 0.5f
                    val origR = Color.red(originalPixel)
                    val origG = Color.green(originalPixel)
                    val origB = Color.blue(originalPixel)

                    val r = (origR * blend + bgR * (1f - blend)).toInt().coerceIn(0, 255)
                    val g = (origG * blend + bgG * (1f - blend)).toInt().coerceIn(0, 255)
                    val b = (origB * blend + bgB * (1f - blend)).toInt().coerceIn(0, 255)
                    pixels[i] = Color.rgb(r, g, b)
                }
            }
        } else {
            // Raw mask has different dimension, sample per row/col
            val confidences = FloatArray(maskWidth * maskHeight)
            for (i in 0 until maskWidth * maskHeight) {
                confidences[i] = maskBuffer.float
            }

            for (y in 0 until height) {
                val maskY = ((y.toFloat() / height) * maskHeight).toInt().coerceIn(0, maskHeight - 1)
                for (x in 0 until width) {
                    val maskX = ((x.toFloat() / width) * maskWidth).toInt().coerceIn(0, maskWidth - 1)
                    val confidence = confidences[maskY * maskWidth + maskX]
                    val idx = y * width + x
                    val originalPixel = pixels[idx]

                    if (confidence >= 0.75f) {
                        pixels[idx] = originalPixel
                    } else if (confidence <= 0.25f) {
                        pixels[idx] = targetBackgroundColor
                    } else {
                        val blend = (confidence - 0.25f) / 0.5f
                        val origR = Color.red(originalPixel)
                        val origG = Color.green(originalPixel)
                        val origB = Color.blue(originalPixel)

                        val r = (origR * blend + bgR * (1f - blend)).toInt().coerceIn(0, 255)
                        val g = (origG * blend + bgG * (1f - blend)).toInt().coerceIn(0, 255)
                        val b = (origB * blend + bgB * (1f - blend)).toInt().coerceIn(0, 255)
                        pixels[idx] = Color.rgb(r, g, b)
                    }
                }
            }
        }

        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        outputBitmap
    }
}
