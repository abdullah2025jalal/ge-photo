package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.A4GridPlan
import com.example.model.PhotoDimension
import com.example.model.SuitTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapProcessor {

    /**
     * Crops the source bitmap to match the requested aspect ratio (width / height),
     * keeping it centered.
     */
    fun cropToAspectRatio(source: Bitmap, targetRatio: Float): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val currentRatio = srcWidth.toFloat() / srcHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int

        if (currentRatio > targetRatio) {
            // Source is wider than target ratio: crop width
            cropHeight = srcHeight
            cropWidth = (srcHeight * targetRatio).toInt().coerceIn(1, srcWidth)
        } else {
            // Source is taller than target ratio: crop height
            cropWidth = srcWidth
            cropHeight = (srcWidth / targetRatio).toInt().coerceIn(1, srcHeight)
        }

        val startX = ((srcWidth - cropWidth) / 2).coerceAtLeast(0)
        val startY = ((srcHeight - cropHeight) / 2).coerceAtLeast(0)

        return Bitmap.createBitmap(source, startX, startY, cropWidth, cropHeight)
    }

    /**
     * Scales a bitmap to exact physical dimensions at 300 DPI.
     */
    fun scaleToDimension(source: Bitmap, dimension: PhotoDimension, dpi: Int = 300): Bitmap {
        val targetWidth = dimension.getWidthPx(dpi)
        val targetHeight = dimension.getHeightPx(dpi)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Loads a suit drawable as a Bitmap.
     */
    fun loadSuitBitmap(context: Context, drawableResId: Int): Bitmap? {
        if (drawableResId == 0) return null
        return BitmapFactory.decodeResource(context.resources, drawableResId)
    }

    /**
     * Composites the person photo with the superimposed suit overlay.
     */
    fun compositeSuit(
        baseBitmap: Bitmap,
        suitBitmap: Bitmap?,
        transform: SuitTransform
    ): Bitmap {
        if (suitBitmap == null) return baseBitmap

        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw the person base
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)

        // Matrix for suit overlay
        val matrix = Matrix()

        // Center suit on its own center
        val suitW = suitBitmap.width.toFloat()
        val suitH = suitBitmap.height.toFloat()
        val baseW = baseBitmap.width.toFloat()
        val baseH = baseBitmap.height.toFloat()

        // Scale factor: suit relative to base photo
        val scale = (baseW / suitW) * transform.scale
        matrix.postScale(scale, scale)

        val scaledSuitW = suitW * scale
        val scaledSuitH = suitH * scale

        // Rotate around center of the scaled suit
        matrix.postRotate(
            transform.rotation,
            scaledSuitW / 2f,
            scaledSuitH / 2f
        )

        // Translate to desired position
        // Base center + user offset
        val posX = (baseW - scaledSuitW) / 2f + transform.offsetX
        val posY = (baseH * 0.42f) + transform.offsetY
        matrix.postTranslate(posX, posY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (transform.alpha.coerceIn(0f, 1f) * 255).toInt()
        }

        canvas.drawBitmap(suitBitmap, matrix, paint)
        return result
    }

    /**
     * Renders a complete high-resolution A4 sheet containing the grid of photos
     * with 3 mm cut spacing and guidelines.
     */
    suspend fun renderA4Sheet(
        photoBitmap: Bitmap,
        plan: A4GridPlan,
        drawCutGuides: Boolean = true,
        dpi: Int = 300
    ): Bitmap = withContext(Dispatchers.Default) {
        val a4WidthPx = plan.getA4WidthPx(dpi)
        val a4HeightPx = plan.getA4HeightPx(dpi)

        val a4Bitmap = Bitmap.createBitmap(a4WidthPx, a4HeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(a4Bitmap)

        // Clean white background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.rgb(200, 205, 215)
            style = Paint.Style.STROKE
            strokeWidth = (dpi / 300f * 1.5f).coerceAtLeast(1f)
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }

        val cutGuidePaint = Paint().apply {
            color = Color.rgb(160, 170, 185)
            strokeWidth = (dpi / 300f * 1.5f).coerceAtLeast(1f)
            style = Paint.Style.STROKE
        }

        val cornerMarkerLen = (plan.spacingCm / 2.54f * dpi * 0.8f).coerceAtLeast(10f)

        // Draw each photo cell
        for (cell in plan.cells) {
            val cellX = cell.xPx(dpi).toFloat()
            val cellY = cell.yPx(dpi).toFloat()
            val cellW = cell.widthPx(dpi).toFloat()
            val cellH = cell.heightPx(dpi).toFloat()

            val destRect = RectF(cellX, cellY, cellX + cellW, cellY + cellH)

            // Draw photo
            canvas.drawBitmap(
                photoBitmap,
                Rect(0, 0, photoBitmap.width, photoBitmap.height),
                destRect,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )

            if (drawCutGuides) {
                // Dashed border around the photo
                canvas.drawRect(destRect, borderPaint)

                // Corner crosshairs / cut guide markers outside the 3mm margin
                // Top-left
                canvas.drawLine(cellX - 8f, cellY, cellX - 8f - cornerMarkerLen, cellY, cutGuidePaint)
                canvas.drawLine(cellX, cellY - 8f, cellX, cellY - 8f - cornerMarkerLen, cutGuidePaint)

                // Top-right
                canvas.drawLine(cellX + cellW + 8f, cellY, cellX + cellW + 8f + cornerMarkerLen, cellY, cutGuidePaint)
                canvas.drawLine(cellX + cellW, cellY - 8f, cellX + cellW, cellY - 8f - cornerMarkerLen, cutGuidePaint)

                // Bottom-left
                canvas.drawLine(cellX - 8f, cellY + cellH, cellX - 8f - cornerMarkerLen, cellY + cellH, cutGuidePaint)
                canvas.drawLine(cellX, cellY + cellH + 8f, cellX, cellY + cellH + 8f + cornerMarkerLen, cutGuidePaint)

                // Bottom-right
                canvas.drawLine(cellX + cellW + 8f, cellY + cellH, cellX + cellW + 8f + cornerMarkerLen, cellY + cellH, cutGuidePaint)
                canvas.drawLine(cellX + cellW, cellY + cellH + 8f, cellX + cellW, cellY + cellH + 8f + cornerMarkerLen, cutGuidePaint)
            }
        }

        a4Bitmap
    }
}
