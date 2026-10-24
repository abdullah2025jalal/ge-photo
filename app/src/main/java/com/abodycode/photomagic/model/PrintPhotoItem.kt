package com.abodycode.photomagic.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

enum class ItemType {
    PHOTO,      // صورة شخصية / عادية
    DOCUMENT    // مستند / هوية / بطاقة (وجه أمامي وخلفي)
}

enum class DocumentDuplexMode {
    SAME_PAGE,  // الوجهان في نفس الصفحة (الأمامي على اليمين والخلفي على اليسار)
    BACK_SIDE   // الوجه الخلفي بالصفحة الثانية (للطباعة على ظهر الورقة)
}

data class PrintPhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val itemType: ItemType = ItemType.PHOTO,
    // Photo source / Document front face
    val sourceBitmap: Bitmap? = null,
    val uri: Uri? = null,
    // Document back face (optional)
    val backBitmap: Bitmap? = null,
    val backUri: Uri? = null,
    val duplexMode: DocumentDuplexMode = DocumentDuplexMode.SAME_PAGE,
    // Dimensions in centimeters
    val widthCm: Float = 3.0f,
    val heightCm: Float = 4.0f,
    val copiesCount: Int = 1
) {
    val aspectRatio: Float
        get() = if (heightCm > 0f) widthCm / heightCm else 0.75f

    val dimensionDisplayText: String
        get() = "$widthCm × $heightCm سم"

    val hasBackFace: Boolean
        get() = itemType == ItemType.DOCUMENT && backBitmap != null
}
