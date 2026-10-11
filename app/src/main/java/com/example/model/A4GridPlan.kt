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

            // Separate handling for duplex documents and standard photos/same-page documents
            for (item in items) {
                val w = item.widthCm.coerceAtLeast(1.0f)
                val h = item.heightCm.coerceAtLeast(1.0f)

                if (item.itemType == ItemType.DOCUMENT) {
                    for (copy in 0 until item.copiesCount) {
                        if (item.duplexMode == DocumentDuplexMode.BACK_SIDE && item.hasBackFace) {
                            // Duplex Mode: Front on Page 1, Back on Page 2 (Mirrored for back-to-back printing)
                            // If remaining height on current front page is insufficient:
                            if ((currentY + h) > (maxY + 0.01f)) {
                                currentPage += 2 // Advance by a pair of pages
                                currentX = minX
                                currentY = minY
                                currentRowMaxHeight = 0f
                                photosInCurrentRow = 0
                            }

                            // Place Front Face on currentPage
                            val frontX = minX
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

                            // Place Back Face on (currentPage + 1) at exact mirrored X position
                            // For horizontal flip duplex, mirror position is: pageWidth - margin - w
                            val backX = pageWidth - marginCm - w
                            placed.add(
                                PlacedPhotoInstance(
                                    photoItem = item,
                                    isBackSide = true,
                                    instanceIndex = globalIndex++,
                                    pageIndex = currentPage + 1,
                                    xCm = backX,
                                    yCm = currentY,
                                    widthCm = w,
                                    heightCm = h
                                )
                            )

                            // Advance Y for both pages
                            currentY += h + spacingCm
                            currentX = minX
                            currentRowMaxHeight = 0f
                            photosInCurrentRow = 0

                        } else {
                            // Same Page Document Mode (Front on Right, Back on Left on the same row)
                            if ((currentY + h) > (maxY + 0.01f)) {
                                currentPage++
                                currentX = minX
                                currentY = minY
                                currentRowMaxHeight = 0f
                                photosInCurrentRow = 0
                            }

                            // If document has both faces:
                            if (item.hasBackFace) {
                                // Left face: Back face at distance 0.6 cm from left margin
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

                                // Right face: Front face at distance 0.6 cm from right margin
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
                                // Single face document
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

                            currentY += h + spacingCm
                            currentX = minX
                            currentRowMaxHeight = 0f
                            photosInCurrentRow = 0
                        }
                    }
                } else {
                    // ItemType.PHOTO: Standard grid packing
                    for (copy in 0 until item.copiesCount) {
                        val exceedsWidth = (currentX + w) > (maxX + 0.01f)
                        val reachesMaxFor3cm = (w <= 3.05f && photosInCurrentRow >= 6)

                        if (exceedsWidth || reachesMaxFor3cm) {
                            currentX = minX
                            currentY += currentRowMaxHeight + spacingCm
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
