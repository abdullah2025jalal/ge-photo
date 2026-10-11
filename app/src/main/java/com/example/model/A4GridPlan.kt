package com.example.model

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
            var currentPage = 0
            var currentX = minX
            var currentY = minY
            var currentRowMaxHeight = 0f
            var photosInCurrentRow = 0
            var globalIndex = 0

            // Helper to wrap the current photo row
            fun flushCurrentPhotoRow() {
                if (photosInCurrentRow > 0) {
                    currentY += currentRowMaxHeight + spacingCm
                    currentX = minX
                    currentRowMaxHeight = 0f
                    photosInCurrentRow = 0
                }
            }

            for (item in items) {
                val w = item.widthCm.coerceAtLeast(1.0f)
                val h = item.heightCm.coerceAtLeast(1.0f)

                if (item.itemType == ItemType.DOCUMENT) {
                    // Documents must ALWAYS start on a new row below any prior photos
                    flushCurrentPhotoRow()

                    for (copy in 0 until item.copiesCount) {
                        if (item.duplexMode == DocumentDuplexMode.BACK_SIDE && item.hasBackFace) {
                            // Duplex Mode: Front on currentPage, Back on (currentPage + 1)
                            // Check if current Y fits on the page:
                            if ((currentY + h) > (maxY + 0.01f)) {
                                // Find highest page so far and advance
                                val highestPage = placed.maxOfOrNull { it.pageIndex } ?: currentPage
                                currentPage = if (highestPage % 2 == 0) highestPage + 2 else highestPage + 1
                                currentX = minX
                                currentY = minY
                                currentRowMaxHeight = 0f
                                photosInCurrentRow = 0
                            }

                            val frontPage = currentPage
                            val backPage = currentPage + 1

                            // Front face on Front Page (0.6 cm from left)
                            val frontX = minX
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = item,
                                    isBackSide = false,
                                    instanceIndex = globalIndex++,
                                    pageIndex = frontPage,
                                    xCm = frontX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )

                            // Back face on Back Page (exact mirror position: 0.6 cm from right edge)
                            val backX = pageWidth - marginCm - w
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = item,
                                    isBackSide = true,
                                    instanceIndex = globalIndex++,
                                    pageIndex = backPage,
                                    xCm = backX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )

                            // Advance row vertically for the next item on these pages
                            currentY += h + spacingCm
                            currentX = minX
                            currentRowMaxHeight = 0f
                            photosInCurrentRow = 0

                        } else {
                            // Same Page Document Mode: Front on Right (0.6cm from right), Back on Left (0.6cm from left)
                            if ((currentY + h) > (maxY + 0.01f)) {
                                currentPage = (placed.maxOfOrNull { it.pageIndex } ?: currentPage) + 1
                                currentX = minX
                                currentY = minY
                                currentRowMaxHeight = 0f
                                photosInCurrentRow = 0
                            }

                            if (item.hasBackFace) {
                                // Left face: Back face at distance 0.6 cm from left edge
                                val backX = minX
                                placed.add(
                                    PlacedPhotoInstance(
                                        photoItem = item,
                                        isBackSide = true,
                                        instanceIndex = globalIndex++,
                                        pageIndex = currentPage,
                                        xCm = backX,
                                        yCm = currentY,
                                        widthCm = w,
                                        heightCm = h
                                    )
                                )

                                // Right face: Front face at distance 0.6 cm from right edge
                                val frontX = pageWidth - marginCm - w
                                placed.add(
                                    PlacedPhotoInstance(
                                        photoItem = item,
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
                                val frontX = pageWidth - marginCm - w
                                placed.add(
                                    PlacedPhotoInstance(
                                        photoItem = item,
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
                            currentRowMaxHeight = 0f
                            photosInCurrentRow = 0
                        }
                    }
                } else {
                    // ItemType.PHOTO: Standard photo grid
                    for (copy in 0 until item.copiesCount) {
                        val exceedsWidth = (currentX + w) > (maxX + 0.01f)
                        val reachesMaxFor3cm = (w <= 3.05f && photosInCurrentRow >= 6)

                        if (exceedsWidth || reachesMaxFor3cm) {
                            flushCurrentPhotoRow()
                        }

                        if ((currentY + h) > (maxY + 0.01f)) {
                            currentPage = (placed.maxOfOrNull { it.pageIndex } ?: currentPage) + 1
                            currentX = minX
                            currentY = minY
                            currentRowMaxHeight = 0f
                            photosInCurrentRow = 0
                        }

                        placed.add(
                            PlacedPhotoInstance(
                                photoItem = item,
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
