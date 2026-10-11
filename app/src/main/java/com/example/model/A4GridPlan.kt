package com.example.model

data class GridCell(
    val row: Int,
    val col: Int,
    val index: Int,
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

    // Pixel coordinates at given DPI (default 300 DPI)
    fun xPx(dpi: Int = 300): Int = ((xCm / 2.54f) * dpi).toInt()
    fun yPx(dpi: Int = 300): Int = ((yCm / 2.54f) * dpi).toInt()
    fun widthPx(dpi: Int = 300): Int = ((widthCm / 2.54f) * dpi).toInt()
    fun heightPx(dpi: Int = 300): Int = ((heightCm / 2.54f) * dpi).toInt()
}

data class A4GridPlan(
    val dimension: PhotoDimension,
    val pageWidthCm: Float = 21.0f,
    val pageHeightCm: Float = 29.7f,
    val spacingCm: Float = 0.3f, // 3 mm as specified by user
    val marginCm: Float = 1.0f,
    val maxCount: Int,
    val selectedCount: Int,
    val cols: Int,
    val rows: Int,
    val cells: List<GridCell>
) {
    // A4 dimensions in EMUs
    val pageWidthEmu: Long = (pageWidthCm * 360000L).toLong()
    val pageHeightEmu: Long = (pageHeightCm * 360000L).toLong()

    // A4 dimensions in pixels at 300 DPI: (21.0 / 2.54) * 300 ≈ 2480 px, (29.7 / 2.54) * 300 ≈ 3508 px
    fun getA4WidthPx(dpi: Int = 300): Int = ((pageWidthCm / 2.54f) * dpi).toInt()
    fun getA4HeightPx(dpi: Int = 300): Int = ((pageHeightCm / 2.54f) * dpi).toInt()

    companion object {
        fun calculate(
            dimension: PhotoDimension,
            requestedCount: Int? = null,
            spacingCm: Float = 0.3f, // 3 mm
            marginCm: Float = 1.0f
        ): A4GridPlan {
            val pageWidth = 21.0f
            val pageHeight = 29.7f

            val availableWidth = pageWidth - (marginCm * 2f)
            val availableHeight = pageHeight - (marginCm * 2f)

            // Number of photos per row: (availableWidth + spacing) / (photoWidth + spacing)
            val cols = ((availableWidth + spacingCm) / (dimension.widthCm + spacingCm)).toInt().coerceAtLeast(1)
            // Number of photos per column
            val rows = ((availableHeight + spacingCm) / (dimension.heightCm + spacingCm)).toInt().coerceAtLeast(1)

            val maxPossible = cols * rows
            val count = requestedCount?.coerceIn(1, maxPossible) ?: maxPossible

            // Center the entire grid within the page
            val totalGridWidth = (cols * dimension.widthCm) + ((cols - 1) * spacingCm)
            val totalGridHeight = (rows * dimension.heightCm) + ((rows - 1) * spacingCm)
            val startX = (pageWidth - totalGridWidth) / 2f
            val startY = (pageHeight - totalGridHeight) / 2f

            val cells = mutableListOf<GridCell>()
            var index = 0
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (index < count) {
                        val x = startX + c * (dimension.widthCm + spacingCm)
                        val y = startY + r * (dimension.heightCm + spacingCm)
                        cells.add(
                            GridCell(
                                row = r,
                                col = c,
                                index = index,
                                xCm = x,
                                yCm = y,
                                widthCm = dimension.widthCm,
                                heightCm = dimension.heightCm
                            )
                        )
                        index++
                    }
                }
            }

            return A4GridPlan(
                dimension = dimension,
                pageWidthCm = pageWidth,
                pageHeightCm = pageHeight,
                spacingCm = spacingCm,
                marginCm = marginCm,
                maxCount = maxPossible,
                selectedCount = count,
                cols = cols,
                rows = rows,
                cells = cells
            )
        }
    }
}
