package com.abodycode.photomagic

import com.abodycode.photomagic.model.A4GridPlan
import com.abodycode.photomagic.model.DocumentDuplexMode
import com.abodycode.photomagic.model.ItemType
import com.abodycode.photomagic.model.PhotoDimension
import com.abodycode.photomagic.model.PrintPhotoItem
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
    fun testPhotosAndDocumentNoOverlap() {
        // Row 1: 6 photos of 3x4 cm
        val photos = PrintPhotoItem(
            name = "صور شخصية",
            itemType = ItemType.PHOTO,
            widthCm = 3.0f,
            heightCm = 4.0f,
            copiesCount = 6
        )

        // Row 2: Document 8.5x5.4 cm
        val document = PrintPhotoItem(
            name = "بطاقة هوية",
            itemType = ItemType.DOCUMENT,
            widthCm = 8.5f,
            heightCm = 5.4f,
            copiesCount = 1
        )

        val plan = A4GridPlan.calculate(
            items = listOf(photos, document),
            marginCm = 0.6f,
            spacingCm = 0.1f
        )

        // Total placed: 6 photos + 1 document = 7
        assertEquals(7, plan.placedPhotos.size)

        // Photos on row 1 must be at Y = 0.6 cm
        val photosList = plan.placedPhotos.filter { it.photoItem.itemType == ItemType.PHOTO }
        assertEquals(6, photosList.size)
        photosList.forEach { assertEquals(0.6f, it.yCm, 0.01f) }

        // Document MUST be below row 1: 0.6 + 4.0 + 0.1 = 4.7 cm
        val docPlaced = plan.placedPhotos.first { it.photoItem.itemType == ItemType.DOCUMENT }
        assertEquals(4.7f, docPlaced.yCm, 0.01f)
    }

    @Test
    fun testDocumentAddedFirstStillPlacesPhotosAtTop() {
        // Document is first in list, but photos should still be placed at top (row 1)
        val document = PrintPhotoItem(
            name = "بطاقة هوية",
            itemType = ItemType.DOCUMENT,
            widthCm = 8.5f,
            heightCm = 5.4f,
            copiesCount = 1
        )
        val photos = PrintPhotoItem(
            name = "صور شخصية",
            itemType = ItemType.PHOTO,
            widthCm = 3.0f,
            heightCm = 4.0f,
            copiesCount = 6
        )

        val plan = A4GridPlan.calculate(
            items = listOf(document, photos),
            marginCm = 0.6f,
            spacingCm = 0.1f
        )

        // Photos on row 1: Y = 0.6 cm
        val photosList = plan.placedPhotos.filter { it.photoItem.itemType == ItemType.PHOTO }
        assertEquals(6, photosList.size)
        photosList.forEach { assertEquals(0.6f, it.yCm, 0.01f) }

        // Document in row below photos: Y = 0.6 + 4.0 + 0.1 = 4.7 cm
        val docPlaced = plan.placedPhotos.first { it.photoItem.itemType == ItemType.DOCUMENT }
        assertEquals(4.7f, docPlaced.yCm, 0.01f)
    }

    @Test
    fun testDuplexBackSideDocumentGeneratesPage2() {
        val photos = PrintPhotoItem(
            name = "صور شخصية",
            itemType = ItemType.PHOTO,
            widthCm = 3.0f,
            heightCm = 4.0f,
            copiesCount = 6
        )
        val document = PrintPhotoItem(
            name = "بطاقة هوية بوجهين",
            itemType = ItemType.DOCUMENT,
            duplexMode = DocumentDuplexMode.BACK_SIDE,
            widthCm = 8.5f,
            heightCm = 5.4f,
            copiesCount = 1
        )

        val plan = A4GridPlan.calculate(
            items = listOf(photos, document),
            marginCm = 0.6f,
            spacingCm = 0.1f
        )

        // Must produce 2 pages (Front page + Back page)
        assertEquals(2, plan.totalPagesCount)

        // Front face on page 0 at Y = 4.7 cm, X = 0.6 cm
        val frontFace = plan.placedPhotos.first { it.photoItem.itemType == ItemType.DOCUMENT && !it.isBackSide }
        assertEquals(0, frontFace.pageIndex)
        assertEquals(0.6f, frontFace.xCm, 0.01f)
        assertEquals(4.7f, frontFace.yCm, 0.01f)

        // Back face on page 1 at Y = 4.7 cm (exact same row height), X = 21.0 - 0.6 - 8.5 = 11.9 cm
        val backFace = plan.placedPhotos.first { it.photoItem.itemType == ItemType.DOCUMENT && it.isBackSide }
        assertEquals(1, backFace.pageIndex)
        assertEquals(11.9f, backFace.xCm, 0.01f)
        assertEquals(4.7f, backFace.yCm, 0.01f)
    }
}
