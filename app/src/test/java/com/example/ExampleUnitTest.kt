package com.example

import com.example.model.A4GridPlan
import com.example.model.DocumentDuplexMode
import com.example.model.ItemType
import com.example.model.PhotoDimension
import com.example.model.PrintPhotoItem
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
    fun testA4GridPlanMultiCalculations() {
        val item1 = PrintPhotoItem(
            name = "صورة 1",
            sourceBitmap = null,
            widthCm = 3.0f,
            heightCm = 4.0f,
            copiesCount = 12
        )

        val plan = A4GridPlan.calculate(
            items = listOf(item1),
            marginCm = 0.6f,
            spacingCm = 0.1f // 1 mm spacing
        )

        assertEquals(12, plan.totalPhotosCount)
        assertTrue(plan.placedPhotos.size >= 12)

        // Check first row: exactly 6 photos of 3cm
        val firstRowPhotos = plan.placedPhotos.filter { it.yCm == 0.6f }
        assertEquals(6, firstRowPhotos.size)

        // Check horizontal 1mm (0.1 cm) spacing between photo 0 and photo 1
        val photo0 = firstRowPhotos[0]
        val photo1 = firstRowPhotos[1]
        val spacing = photo1.xCm - (photo0.xCm + photo0.widthCm)
        assertEquals(0.1f, spacing, 0.01f)

        // Check starting margin: exactly 0.6 cm
        assertEquals(0.6f, photo0.xCm, 0.01f)
        assertEquals(0.6f, photo0.yCm, 0.01f)

        // Check second row Y coordinate: 0.6 + 4.0 + 0.1 = 4.7 cm
        val secondRowPhotos = plan.placedPhotos.filter { it.yCm > 0.6f }
        val secondRowFirstPhoto = secondRowPhotos.first()
        assertEquals(4.7f, secondRowFirstPhoto.yCm, 0.01f)

        // Check EMU conversions: 1 cm = 360,000 EMU
        assertEquals((3.0f * 360000L).toLong(), photo0.widthEmu)
        assertEquals((4.0f * 360000L).toLong(), photo0.heightEmu)
    }

    @Test
    fun testDocumentSamePageLayout() {
        // Document: 8.5 x 5.4 cm with front and back on same page
        val docItem = PrintPhotoItem(
            name = "بطاقة هوية",
            itemType = ItemType.DOCUMENT,
            sourceBitmap = null,
            backBitmap = null, // Will test with mock or flag
            duplexMode = DocumentDuplexMode.SAME_PAGE,
            widthCm = 8.5f,
            heightCm = 5.4f,
            copiesCount = 1
        )

        // Create with back face present:
        // We can test both faces using reflection or placing directly
        val plan = A4GridPlan.calculate(listOf(docItem))
        assertEquals(1, plan.placedPhotos.size)
    }
}
