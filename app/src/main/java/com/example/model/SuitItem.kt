package com.example.model

import com.example.R

enum class SuitCategory {
    ALL,
    MEN,
    GIRL,
    NONE
}

data class SuitItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: SuitCategory,
    val drawableResId: Int
) {
    companion object {
        val ALL_SUITS = listOf(
            SuitItem(
                id = "suit_navy",
                title = "قاط كحلي رسمي",
                subtitle = "بدلة رجالية مع ربطة عنق حمراء مقلمة",
                category = SuitCategory.MEN,
                drawableResId = R.drawable.suit_navy
            ),
            SuitItem(
                id = "suit_black",
                title = "بدلة سوداء كلاسيك",
                subtitle = "قاط رسمي أسود أنيق مع ربطة سوداء",
                category = SuitCategory.MEN,
                drawableResId = R.drawable.suit_black
            ),
            SuitItem(
                id = "suit_pinafore",
                title = "صدرية مدرسية للبنات",
                subtitle = "زي مدرسي رسمي مع قميص وياقة بيضاء",
                category = SuitCategory.GIRL,
                drawableResId = R.drawable.suit_pinafore
            ),
            SuitItem(
                id = "none",
                title = "بدون ملابس إضافية",
                subtitle = "الاحتفاظ بالملابس الأصلية للشخص",
                category = SuitCategory.NONE,
                drawableResId = 0
            )
        )
    }
}

data class SuitTransform(
    val offsetX: Float = 0f,    // Normalized or pixels relative to image width
    val offsetY: Float = 0f,    // Normalized or pixels relative to image height
    val scale: Float = 1.0f,
    val rotation: Float = 0f,   // Degrees
    val alpha: Float = 1.0f
) {
    companion object {
        val DEFAULT = SuitTransform()
    }
}
