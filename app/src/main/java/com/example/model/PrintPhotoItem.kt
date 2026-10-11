package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

data class PrintPhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceBitmap: Bitmap? = null,
    val uri: Uri? = null,
    val widthCm: Float = 3.0f,
    val heightCm: Float = 4.0f,
    val copiesCount: Int = 6
) {
    val aspectRatio: Float
        get() = if (heightCm > 0f) widthCm / heightCm else 0.75f

    val dimensionDisplayText: String
        get() = "$widthCm × $heightCm سم"
}
