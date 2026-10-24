package com.abodycode.photomagic.model

data class PhotoDimension(
    val name: String,
    val widthCm: Float,
    val heightCm: Float,
    val isCustom: Boolean = false
) {
    val aspectRatio: Float
        get() = if (heightCm > 0f) widthCm / heightCm else 0.75f

    fun getWidthPx(dpi: Int = 300): Int = ((widthCm / 2.54f) * dpi).toInt().coerceAtLeast(50)

    fun getHeightPx(dpi: Int = 300): Int = ((heightCm / 2.54f) * dpi).toInt().coerceAtLeast(50)

    val displayText: String
        get() = "$widthCm × $heightCm سم"

    companion object {
        val PRESETS = listOf(
            PhotoDimension("3 × 4 سم (معاملات - 6 بالسطر)", 3.0f, 4.0f),
            PhotoDimension("3.5 × 4.5 سم (شنغن وأوروبا)", 3.5f, 4.5f),
            PhotoDimension("4 × 6 سم (جوازات وبطاقات)", 4.0f, 6.0f),
            PhotoDimension("5 × 5 سم (تأشيرة أمريكا 2x2)", 5.0f, 5.0f),
            PhotoDimension("5 × 7 سم (بورتريه شخصي)", 5.0f, 7.0f),
            PhotoDimension("10 × 15 سم (بطاقة صور كاملة)", 10.0f, 15.0f)
        )

        val DEFAULT = PRESETS[0]
    }
}
