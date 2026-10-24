package com.abodycode.photomagic.model

import kotlin.math.max

data class PlacedPhotoInstance(
    val photoItem: PrintPhotoItem,
    val isBackSide: Boolean = false,
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
    val marginCm: Float = 0.6f,        // 0.6 cm margins
    val spacingCm: Float = 0.1f,       // 1 mm spacing between items and rows
    val placedPhotos: List<PlacedPhotoInstance>,
    val totalPhotosCount: Int,
    val totalPagesCount: Int
) {
    val pageWidthEmu: Long = (pageWidthCm * 360000L).toLong()
    val pageHeightEmu: Long = (pageHeightCm * 360000L).toLong()

    fun getA4WidthPx(dpi: Int = 300): Int = ((pageWidthCm / 2.54f) * dpi).toInt()
    fun getA4HeightPx(dpi: Int = 300): Int = ((pageHeightCm / 2.54f) * dpi).toInt()

    companion object {
        fun calculate(
            items: List<PrintPhotoItem>,
            marginCm: Float = 0.6f,
            spacingCm: Float = 0.1f
        ): A4GridPlan {
            val pageWidth = 21.0f
            val pageHeight = 29.7f

            val minX = marginCm
            val maxX = pageWidth - marginCm
            val minY = marginCm
            val maxY = pageHeight - marginCm

            val placed = mutableListOf<PlacedPhotoInstance>()
            var globalIndex = 0

            // 1. Separate items: Photos are always prioritized in the top row(s),
            // and Documents are always placed in rows below the photos with exactly 1 mm spacing!
            val photoItems = items.filter { it.itemType == ItemType.PHOTO }
            val docItems = items.filter { it.itemType == ItemType.DOCUMENT }

            var currentPage = 0
            var currentX = minX
            var currentY = minY
            var currentRowMaxHeight = 0f
            var photosInCurrentRow = 0

            // Layout all standard photos in top rows first
            for (photoItem in photoItems) {
                val w = photoItem.widthCm.coerceAtLeast(1.0f)
                val h = photoItem.heightCm.coerceAtLeast(1.0f)

                for (copy in 0 until photoItem.copiesCount) {
                    val exceedsWidth = (currentX + w) > (maxX + 0.01f)
                    val reachesMaxFor3cm = (w <= 3.05f && photosInCurrentRow >= 6)

                    if (exceedsWidth || reachesMaxFor3cm) {
                        // Move down to next row of photos
                        currentY += currentRowMaxHeight + spacingCm
                        currentX = minX
                        currentRowMaxHeight = 0f
                        photosInCurrentRow = 0
                    }

                    if ((currentY + h) > (maxY + 0.01f)) {
                        currentPage++
                        currentX = minX
                        currentY = minY
                        currentRowMaxHeight = 0f
                        photosInCurrentRow = 0
                    }

                    placed.add(
                        PlacedPhotoInstance(
                            photoItem = photoItem,
                            isBackSide = false,
                            instanceIndex = globalIndex++,
                            pageIndex = currentPage,
                            xCm = currentX,
                            yCm = currentY,
                            widthCm = w,
                            heightCm = h
                        )
                    )

                    photosInCurrentRow++
                    currentRowMaxHeight = max(currentRowMaxHeight, h)
                    currentX += w + spacingCm
                }
            }

            // If photos were placed in the last row, advance Y by row height + 1 mm spacing
            // so documents are placed in the row BELOW the photos with 1 mm gap!
            if (photosInCurrentRow > 0) {
                currentY += currentRowMaxHeight + spacingCm
                currentX = minX
                currentRowMaxHeight = 0f
                photosInCurrentRow = 0
            }

            // Layout all documents in the row(s) below the photos
            for (docItem in docItems) {
                val w = docItem.widthCm.coerceAtLeast(1.0f)
                val h = docItem.heightCm.coerceAtLeast(1.0f)

                for (copy in 0 until docItem.copiesCount) {
                    // Check if document fits vertically on current page
                    if ((currentY + h) > (maxY + 0.01f)) {
                        val highestPage = placed.maxOfOrNull { it.pageIndex } ?: currentPage
                        currentPage = highestPage + 1
                        currentX = minX
                        currentY = minY
                    }

                    if (docItem.duplexMode == DocumentDuplexMode.BACK_SIDE) {
                        // Duplex Mode:
                        // Front face on Front Page (currentPage) at left margin (0.6 cm)
                        // Back face on Back Page (currentPage + 1) at mirror position (0.6 cm from right)
                        // Both at the exact same vertical position Y (1 mm below photos)
                        val frontPage = currentPage
                        val backPage = currentPage + 1

                        val frontX = minX
                        placed.add(
                            PlacedPhotoInstance(
                                photoItem = docItem,
                                isBackSide = false,
                                instanceIndex = globalIndex++,
                                pageIndex = frontPage,
                                xCm = frontX,
                                yCm = currentY,
                                widthCm = w,
                                heightCm = h
                            )
                        )

                        val backX = pageWidth - marginCm - w
                        placed.add(
                            PlacedPhotoInstance(
                                photoItem = docItem,
                                isBackSide = true,
                                instanceIndex = globalIndex++,
                                pageIndex = backPage,
                                xCm = backX,
                                yCm = currentY,
                                widthCm = w,
                                heightCm = h
                            )
                        )

                        // Advance row for next document copy
                        currentY += h + spacingCm
                        currentX = minX
                    } else {
                        // Same Page Mode: Both faces on the same page
                        if (docItem.hasBackFace) {
                            // Back face on Left (0.6 cm from left margin)
                            val backX = minX
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = docItem,
                                    isBackSide = true,
                                    instanceIndex = globalIndex++,
                                    pageIndex = currentPage,
                                    xCm = backX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )

                            // Front face on Right (0.6 cm from right margin)
                            val frontX = pageWidth - marginCm - w
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = docItem,
                                    isBackSide = false,
                                    instanceIndex = globalIndex++,
                                    pageIndex = currentPage,
                                    xCm = frontX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )
                        } else {
                            // Single face document on the same page
                            val frontX = minX
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = docItem,
                                    isBackSide = false,
                                    instanceIndex = globalIndex++,
                                    pageIndex = currentPage,
                                    xCm = frontX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )
                        }

                        // Advance row vertically by document height + 1 mm spacing
                        currentY += h + spacingCm
                        currentX = minX
                    }
                }
            }

            val maxPageInPlaced = placed.maxOfOrNull { it.pageIndex } ?: 0
            val totalPages = if (placed.isEmpty()) 1 else (maxPageInPlaced + 1)

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
