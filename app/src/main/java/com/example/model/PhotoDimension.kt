package com.example.model

data class PhotoDimension(
    val name: String,
    val widthCm: Float,
    val heightCm: Float,
    val isCustom: Boolean = false
) {
    val aspectRatio: Float
        get() = if (heightCm > 0f) widthCm / heightCm else 0.75f

    fun getWidthPx(dpi: Int = 300): Int = ((widthCm / 2.54f) * dpi).toInt().coerceAtLeast(100)

    fun getHeightPx(dpi: Int = 300): Int = ((heightCm / 2.54f) * dpi).toInt().coerceAtLeast(100)

    val displayText: String
        get() = "$widthCm × $heightCm سم"

    companion object {
        val PRESETS = listOf(
            PhotoDimension("3 × 4 سم (معاملات وهوية)", 3.0f, 4.0f),
            PhotoDimension("3.5 × 4.5 سم (شنغن وأوروبا)", 3.5f, 4.5f),
            PhotoDimension("4 × 6 سم (جوازات ورسميات)", 4.0f, 6.0f),
            PhotoDimension("5 × 5 سم (فيزا أمريكا 2x2)", 5.0f, 5.0f),
            PhotoDimension("5 × 7 سم (بورتريه واستوديو)", 5.0f, 7.0f),
        )

        val DEFAULT = PRESETS[0]
    }
}
