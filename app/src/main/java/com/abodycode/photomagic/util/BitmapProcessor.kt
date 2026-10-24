package com.abodycode.photomagic.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.abodycode.photomagic.model.A4GridPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object BitmapProcessor {

    /**
     * Loads a bitmap safely from a content URI.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crops source bitmap to exact target aspect ratio (width / height), keeping it centered.
     */
    fun cropToAspectRatio(source: Bitmap, targetRatio: Float): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val currentRatio = srcWidth.toFloat() / srcHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int

        if (currentRatio > targetRatio) {
            cropHeight = srcHeight
            cropWidth = (srcHeight * targetRatio).toInt().coerceIn(1, srcWidth)
        } else {
            cropWidth = srcWidth
            cropHeight = (srcWidth / targetRatio).toInt().coerceIn(1, srcHeight)
        }

        val startX = ((srcWidth - cropWidth) / 2).coerceAtLeast(0)
        val startY = ((srcHeight - cropHeight) / 2).coerceAtLeast(0)

        return Bitmap.createBitmap(source, startX, startY, cropWidth, cropHeight)
    }

    /**
     * Renders a complete high-resolution A4 sheet for photo paper (ورق صور مقوى)
     * at 300 DPI without dashed borders, with 1 mm spacing and 0.6 cm margins.
     */
    suspend fun renderA4Sheet(
        plan: A4GridPlan,
        pageIndex: Int = 0,
        dpi: Int = 300
    ): Bitmap = withContext(Dispatchers.Default) {
        val a4WidthPx = plan.getA4WidthPx(dpi)
        val a4HeightPx = plan.getA4HeightPx(dpi)

        val a4Bitmap = Bitmap.createBitmap(a4WidthPx, a4HeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(a4Bitmap)

        // Pure white background for photo paper
        canvas.drawColor(Color.WHITE)

        val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val pagePhotos = plan.placedPhotos.filter { it.pageIndex == pageIndex }

        // Cache cropped bitmaps for performance
        val croppedCache = mutableMapOf<String, Bitmap>()

        for (placed in pagePhotos) {
            val item = placed.photoItem
            val sourceBitmap = if (placed.isBackSide) item.backBitmap else item.sourceBitmap

            val cellX = placed.xPx(dpi).toFloat()
            val cellY = placed.yPx(dpi).toFloat()
            val cellW = placed.widthPx(dpi).toFloat()
            val cellH = placed.heightPx(dpi).toFloat()

            val destRect = RectF(cellX, cellY, cellX + cellW, cellY + cellH)

            if (sourceBitmap != null) {
                val cacheKey = "${item.id}_${if (placed.isBackSide) "back" else "front"}_${item.widthCm}_${item.heightCm}"

                val cropped = croppedCache.getOrPut(cacheKey) {
                    cropToAspectRatio(sourceBitmap, item.aspectRatio)
                }

                // Draw photo with zero borders or dashed lines as requested
                canvas.drawBitmap(
                    cropped,
                    Rect(0, 0, cropped.width, cropped.height),
                    destRect,
                    photoPaint
                )
            } else if (placed.isBackSide) {
                // If back side image is not provided, draw a clean placeholder card
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#F8FAFC")
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = (dpi / 150f) * 2f
                }
                val textPaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = (dpi / 150f) * 16f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawRect(destRect, bgPaint)
                canvas.drawRect(destRect, borderPaint)
                canvas.drawText("الوجه الخلفي للمستند", destRect.centerX(), destRect.centerY() + (textPaint.textSize / 3f), textPaint)
            }
        }

        a4Bitmap
    }
}
