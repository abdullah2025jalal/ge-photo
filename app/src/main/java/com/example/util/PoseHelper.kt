package com.example.util

import android.graphics.Bitmap
import com.example.model.SuitTransform
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.hypot

object PoseHelper {

    private val poseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    /**
     * Analyzes subject's shoulders to auto-position, scale, and rotate the suit.
     */
    suspend fun estimateSuitPlacement(bitmap: Bitmap): SuitTransform = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val pose = suspendCancellableCoroutine { continuation ->
            poseDetector.process(inputImage)
                .addOnSuccessListener { detectedPose ->
                    continuation.resume(detectedPose)
                }
                .addOnFailureListener {
                    // Fall back gracefully to null on failure
                    continuation.resume(null)
                }
        }

        if (pose == null) {
            return@withContext SuitTransform(
                offsetX = 0f,
                offsetY = bitmap.height * 0.40f,
                scale = 1.0f,
                rotation = 0f
            )
        }

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftShoulder != null && rightShoulder != null &&
            leftShoulder.inFrameLikelihood > 0.4f && rightShoulder.inFrameLikelihood > 0.4f
        ) {
            val leftPoint = leftShoulder.position
            val rightPoint = rightShoulder.position

            val dx = rightPoint.x - leftPoint.x
            val dy = rightPoint.y - leftPoint.y
            val shoulderDistance = hypot(dx, dy)

            // Calculate rotation angle in degrees
            val angleRadians = atan2(dy.toDouble(), dx.toDouble())
            val angleDegrees = Math.toDegrees(angleRadians).toFloat()

            // Shoulder midpoint (center of chest/collar)
            val midX = (leftPoint.x + rightPoint.x) / 2f
            val midY = (leftPoint.y + rightPoint.y) / 2f

            // Calculate scale relative to image width. Normal shoulder span is ~55-65% of image width.
            val targetSuitWidth = shoulderDistance * 1.85f
            val baseScale = targetSuitWidth / bitmap.width.toFloat()
            val clampedScale = baseScale.coerceIn(0.75f, 1.45f)

            // Offset from image center
            val offsetX = midX - (bitmap.width / 2f)
            // Suit neck collar sits slightly below midpoint
            val offsetY = midY - (bitmap.height * 0.22f)

            SuitTransform(
                offsetX = offsetX,
                offsetY = offsetY,
                scale = clampedScale,
                rotation = angleDegrees,
                alpha = 1.0f
            )
        } else {
            // Default studio placement
            SuitTransform(
                offsetX = 0f,
                offsetY = bitmap.height * 0.38f,
                scale = 1.05f,
                rotation = 0f
            )
        }
    }
}
