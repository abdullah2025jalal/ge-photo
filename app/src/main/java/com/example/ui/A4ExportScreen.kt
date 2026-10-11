package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.A4GridPlan
import com.example.model.PhotoDimension
import com.example.util.BitmapProcessor
import com.example.util.DocxExportHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A4ExportScreen(
    processedPhoto: Bitmap,
    dimension: PhotoDimension,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Calculate grid layout
    var selectedCount by remember { mutableStateOf<Int?>(null) }
    var drawCutGuides by remember { mutableStateOf(true) }

    val gridPlan = remember(dimension, selectedCount) {
        A4GridPlan.calculate(
            dimension = dimension,
            requestedCount = selectedCount,
            spacingCm = 0.3f, // 3 mm as specified
            marginCm = 1.0f
        )
    }

    // Render A4 visual preview
    var a4PreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(true) }
    var isExportingDocx by remember { mutableStateOf(false) }

    LaunchedEffect(gridPlan, drawCutGuides) {
        isRenderingPreview = true
        scope.launch {
            try {
                // Render at 150 DPI for snappy visual preview
                val preview = BitmapProcessor.renderA4Sheet(
                    photoBitmap = processedPhoto,
                    plan = gridPlan,
                    drawCutGuides = drawCutGuides,
                    dpi = 150
                )
                a4PreviewBitmap = preview
            } finally {
                isRenderingPreview = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .testTag("back_to_fitting_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "شبكة A4 وتصدير Word",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "صفحة A4 (21×29.7 سم) | مسافة القص: 3 ملم",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp
                )
            }

            // Quick Share button
            IconButton(
                onClick = {
                    scope.launch {
                        exportAndShareDocx(context, processedPhoto, gridPlan)
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF0284C7), CircleShape)
                    .testTag("top_share_docx_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "مشاركة",
                    tint = Color.White
                )
            }
        }

        // Visual A4 Sheet Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRenderingPreview || a4PreviewBitmap == null) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(42.dp)
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // A4 standard aspect ratio: 21.0 / 29.7 ≈ 0.707
                    val a4Ratio = 21.0f / 29.7f
                    val containerRatio = maxWidth / maxHeight

                    val frameModifier = if (a4Ratio > containerRatio) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(a4Ratio)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .aspectRatio(a4Ratio)
                    }

                    // A4 Sheet visual with paper shadow
                    Card(
                        modifier = frameModifier
                            .shadow(16.dp, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = a4PreviewBitmap!!.asImageBitmap(),
                            contentDescription = "معاينة صفحة A4",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom Controls and Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Info badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عدد الصور في الصفحة:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${gridPlan.selectedCount} من أصل ${gridPlan.maxCount} صورة (${gridPlan.cols} أعمدة × ${gridPlan.rows} صفوف)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity preset chips
                val countOptions = listOf(4, 6, 8, 12, gridPlan.maxCount).distinct()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    countOptions.forEach { count ->
                        val isSelected = gridPlan.selectedCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCount = count },
                            label = {
                                Text(
                                    text = if (count == gridPlan.maxCount) "كامل الصفحة ($count)" else "$count صور",
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(12.dp))

                // Primary Word Export Action Button (Docx in front of text)
                Button(
                    onClick = {
                        if (!isExportingDocx) {
                            isExportingDocx = true
                            scope.launch {
                                exportAndShareDocx(
                                    context = context,
                                    photoBitmap = processedPhoto,
                                    plan = gridPlan
                                )
                                isExportingDocx = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_docx_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExportingDocx) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري بناء ملف Word (.docx)...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير إلى ملف Word (.docx) - التفاف أمام النص",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Actions Row: Save High-Res A4 Sheet & Save Single Photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                saveA4HighResImage(context, processedPhoto, gridPlan)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_a4_image_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ صفحة A4 (300DPI)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                saveSinglePhoto(context, processedPhoto, dimension)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_single_photo_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ صورة واحدة", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• الصور داخل ملف Word تم ضبطها بوضع (أمام النص) لسهولة التحريك وتعديل المقاسات داخل Word مباشرة بدون إفساد التنسيق.",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Generates Word DOCX with floating images in front of text and opens Android Share / Open intent.
 */
private suspend fun exportAndShareDocx(
    context: Context,
    photoBitmap: Bitmap,
    plan: A4GridPlan
) {
    try {
        val fileName = "PhotoGrid_${plan.dimension.widthCm}x${plan.dimension.heightCm}_A4.docx"
        val docxFile = DocxExportHelper.generateDocx(
            context = context,
            photoBitmap = photoBitmap,
            plan = plan,
            fileName = fileName
        )

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            docxFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "شبكة صور A4 - جاهزة للطباعة")
            putExtra(Intent.EXTRA_TEXT, "تم إنشاء شبكة صور A4 (${plan.dimension.displayText}) بمسافة قص 3 ملم والتفاف أمام النص.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو فتح ملف Word"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "حدث خطأ أثناء تصدير ملف Word: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Renders and saves high-resolution A4 image (300 DPI) to device storage.
 */
private suspend fun saveA4HighResImage(
    context: Context,
    photoBitmap: Bitmap,
    plan: A4GridPlan
) {
    try {
        Toast.makeText(context, "جاري حفظ صفحة A4 بدقة الطباعة 300 DPI...", Toast.LENGTH_SHORT).show()
        val a4HiRes = BitmapProcessor.renderA4Sheet(
            photoBitmap = photoBitmap,
            plan = plan,
            drawCutGuides = true,
            dpi = 300
        )

        val fileName = "PhotoGrid_A4_${System.currentTimeMillis()}.png"
        saveBitmapToGallery(context, a4HiRes, fileName)
        Toast.makeText(context, "تم حفظ صفحة A4 بنجاح في مجلد الصور!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل حفظ الصفحة: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves a single processed passport photo at 300 DPI.
 */
private suspend fun saveSinglePhoto(
    context: Context,
    photo: Bitmap,
    dimension: PhotoDimension
) {
    try {
        val scaled = BitmapProcessor.scaleToDimension(photo, dimension, 300)
        val fileName = "ID_Photo_${dimension.widthCm}x${dimension.heightCm}_${System.currentTimeMillis()}.png"
        saveBitmapToGallery(context, scaled, fileName)
        Toast.makeText(context, "تم حفظ الصورة الفردية بنجاح!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل حفظ الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String) {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoGrid")
        }
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoGrid").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
