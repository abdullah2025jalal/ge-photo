package com.example

import com.example.model.A4GridPlan
import com.example.model.PhotoDimension
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPhotoDimensionCalculations() {
        val dim = PhotoDimension("3x4 cm", 3.0f, 4.0f)
        assertEquals(0.75f, dim.aspectRatio, 0.001f)

        // 300 DPI: (3.0 / 2.54) * 300 ≈ 354 px
        val widthPx = dim.getWidthPx(300)
        assertTrue(widthPx in 350..360)

        // 300 DPI: (4.0 / 2.54) * 300 ≈ 472 px
        val heightPx = dim.getHeightPx(300)
        assertTrue(heightPx in 470..480)
    }

    @Test
    fun testA4GridPlanCalculations() {
        val dim = PhotoDimension("3x4 cm", 3.0f, 4.0f)
        val plan = A4GridPlan.calculate(
            dimension = dim,
            spacingCm = 0.3f, // 3 mm cut spacing
            marginCm = 1.0f
        )

        // Check cols and rows
        assertTrue(plan.cols >= 4)
        assertTrue(plan.rows >= 5)
        assertEquals(plan.cols * plan.rows, plan.maxCount)

        // Check cell spacing
        if (plan.cells.size >= 2) {
            val cell0 = plan.cells[0]
            val cell1 = plan.cells[1]
            val actualSpacing = cell1.xCm - (cell0.xCm + cell0.widthCm)
            assertEquals(0.3f, actualSpacing, 0.01f)
        }

        // Check EMU conversion: 1 cm = 360,000 EMU
        val firstCell = plan.cells.first()
        assertEquals((3.0f * 360000L).toLong(), firstCell.widthEmu)
        assertEquals((4.0f * 360000L).toLong(), firstCell.heightEmu)
    }
}
