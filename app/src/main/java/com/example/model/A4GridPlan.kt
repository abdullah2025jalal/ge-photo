package com.example.model

import kotlin.math.max

data class PlacedPhotoInstance(
    val photoItem: PrintPhotoItem,
    val instanceIndex: Int,
    val pageIndex: Int,
    val xCm: Float,
    val yCm: Float,
    val widthCm: Float,
    val heightCm: Float
) {
    // 1 cm = 360,000 EMU (English Metric Units for Microsoft Word OpenXML)
    val xEmu: Long get() = (xCm * 360000L).toLong()
    val yEmu: Long get() = (yCm * 360000L).toLong()
    val widthEmu: Long get() = (widthCm * 360000L).toLong()
    val heightEmu: Long get() = (heightCm * 360000L).toLong()

    // 300 DPI high resolution pixels
    fun xPx(dpi: Int = 300): Int = ((xCm / 2.54f) * dpi).toInt()
    fun yPx(dpi: Int = 300): Int = ((yCm / 2.54f) * dpi).toInt()
    fun widthPx(dpi: Int = 300): Int = ((widthCm / 2.54f) * dpi).toInt()
    fun heightPx(dpi: Int = 300): Int = ((heightCm / 2.54f) * dpi).toInt()
}

data class A4GridPlan(
    val pageWidthCm: Float = 21.0f,
    val pageHeightCm: Float = 29.7f,
    val marginCm: Float = 0.6f,        // 0.6 cm cropped margin around sheet
    val spacingCm: Float = 0.1f,       // 1 mm spacing between photos and rows
    val placedPhotos: List<PlacedPhotoInstance>,
    val totalPhotosCount: Int,
    val totalPagesCount: Int
) {
    val pageWidthEmu: Long = (pageWidthCm * 360000L).toLong()
    val pageHeightEmu: Long = (pageHeightCm * 360000L).toLong()

    // High quality 300 DPI dimensions: 21.0 / 2.54 * 300 ≈ 2480 px, 29.7 / 2.54 * 300 ≈ 3508 px
    fun getA4WidthPx(dpi: Int = 300): Int = ((pageWidthCm / 2.54f) * dpi).toInt()
    fun getA4HeightPx(dpi: Int = 300): Int = ((pageHeightCm / 2.54f) * dpi).toInt()

    companion object {
        fun calculate(
            items: List<PrintPhotoItem>,
            marginCm: Float = 0.6f,   // 0.6 cm margins
            spacingCm: Float = 0.1f   // 1 mm (0.1 cm) spacing
        ): A4GridPlan {
            val pageWidth = 21.0f
            val pageHeight = 29.7f

            val minX = marginCm
            val maxX = pageWidth - marginCm
            val minY = marginCm
            val maxY = pageHeight - marginCm

            val placed = mutableListOf<PlacedPhotoInstance>()
            var currentPage = 0
            var currentX = minX
            var currentY = minY
            var currentRowMaxHeight = 0f
            var photosInCurrentRow = 0
            var globalIndex = 0

            // Expand all copies for each photo item
            for (item in items) {
                val w = item.widthCm.coerceAtLeast(1.0f)
                val h = item.heightCm.coerceAtLeast(1.0f)

                for (copy in 0 until item.copiesCount) {
                    // Check if photo fits in the current row:
                    // Max 6 photos per row for 3cm photos, or if currentX + w > maxX
                    val exceedsWidth = (currentX + w) > (maxX + 0.01f)
                    val reachesMaxFor3cm = (w <= 3.05f && photosInCurrentRow >= 6)

                    if (exceedsWidth || reachesMaxFor3cm) {
                        // Move to next row
                        currentX = minX
                        currentY += currentRowMaxHeight + spacingCm
                        currentRowMaxHeight = 0f
                        photosInCurrentRow = 0
                    }

                    // Check if exceeds page height:
                    if ((currentY + h) > (maxY + 0.01f)) {
                        // Move to next page
                        currentPage++
                        currentX = minX
                        currentY = minY
                        currentRowMaxHeight = 0f
                        photosInCurrentRow = 0
                    }

                    placed.add(
                        PlacedPhotoInstance(
                            photoItem = item,
                            instanceIndex = globalIndex,
                            pageIndex = currentPage,
                            xCm = currentX,
                            yCm = currentY,
                            widthCm = w,
                            heightCm = h
                        )
                    )

                    globalIndex++
                    photosInCurrentRow++
                    currentRowMaxHeight = max(currentRowMaxHeight, h)
                    currentX += w + spacingCm
                }
            }

            val totalPages = if (placed.isEmpty()) 1 else (currentPage + 1)

            return A4GridPlan(
                pageWidthCm = pageWidth,
                pageHeightCm = pageHeight,
                marginCm = marginCm,
                spacingCm = spacingCm,
                placedPhotos = placed,
                totalPhotosCount = placed.size,
                totalPagesCount = totalPages
            )
        }
    }
}
