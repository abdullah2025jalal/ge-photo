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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.A4GridPlan
import com.example.model.PhotoDimension
import com.example.model.PrintPhotoItem
import com.example.util.BitmapProcessor
import com.example.util.DocxExportHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // List of photos in the studio queue
    val photoItems = remember { mutableStateListOf<PrintPhotoItem>() }

    // Active Tab: 0 = Photos & Settings, 1 = A4 Preview & Export
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    // Multi-photo picker
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEachIndexed { idx, uri ->
                val bitmap = BitmapProcessor.loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    val count = photoItems.size + 1
                    photoItems.add(
                        PrintPhotoItem(
                            name = "صورة $count",
                            sourceBitmap = bitmap,
                            uri = uri,
                            widthCm = 3.0f,
                            heightCm = 4.0f,
                            copiesCount = 6 // Default 6 copies (1 full row)
                        )
                    )
                }
            }
        }
    }

    // Grid calculation
    val gridPlan = remember(photoItems.toList()) {
        A4GridPlan.calculate(
            items = photoItems,
            marginCm = 0.6f,   // Exactly 0.6 cm margin
            spacingCm = 0.1f   // Exactly 1 mm (0.1 cm) spacing
        )
    }

    // A4 preview bitmap
    var a4PreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(false) }
    var isExportingDocx by remember { mutableStateOf(false) }

    LaunchedEffect(gridPlan, selectedPageIndex) {
        if (photoItems.isNotEmpty()) {
            isRenderingPreview = true
            scope.launch {
                try {
                    val preview = BitmapProcessor.renderA4Sheet(
                        plan = gridPlan,
                        pageIndex = selectedPageIndex,
                        dpi = 150 // Smooth visual preview
                    )
                    a4PreviewBitmap = preview
                } finally {
                    isRenderingPreview = false
                }
            }
        } else {
            a4PreviewBitmap = null
        }
    }

    // Item being edited in custom dimensions dialog
    var itemToEditDimensions by remember { mutableStateOf<PrintPhotoItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Bar
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "استوديو طباعة شبكة الصور A4",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "طباعة ورق مقوى | مسافة 1 ملم | هامش 0.6 سم | 6 صور بالسطر",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }

                    // Add photos button
                    Button(
                        onClick = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_photos_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة صور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs: [ الصور والإعدادات ] | [ معاينة A4 والتصدير ]
                TabRow(
                    selectedTabIndex = activeTabIndex,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .height(38.dp)
                ) {
                    Tab(
                        selected = activeTabIndex == 0,
                        onClick = { activeTabIndex = 0 },
                        text = {
                            Text(
                                text = "الصور والإعدادات (${photoItems.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = activeTabIndex == 1,
                        onClick = { activeTabIndex = 1 },
                        text = {
                            Text(
                                text = "معاينة ورقة A4 والطباعة (${gridPlan.totalPhotosCount})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        if (activeTabIndex == 0) {
            // Photos List & Custom Dimensions Config
            PhotosListTab(
                photoItems = photoItems,
                onAddPhotos = {
                    multiPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onUpdateItem = { updatedItem ->
                    val idx = photoItems.indexOfFirst { it.id == updatedItem.id }
                    if (idx != -1) {
                        photoItems[idx] = updatedItem
                    }
                },
                onDeleteItem = { itemToDelete ->
                    photoItems.removeIf { it.id == itemToDelete.id }
                },
                onEditDimensions = { item ->
                    itemToEditDimensions = item
                },
                onGoToPreview = {
                    activeTabIndex = 1
                }
            )
        } else {
            // A4 Visual Preview & Word DOCX Export
            A4PreviewTab(
                gridPlan = gridPlan,
                photoItems = photoItems,
                previewBitmap = a4PreviewBitmap,
                isRendering = isRenderingPreview,
                isExporting = isExportingDocx,
                selectedPageIndex = selectedPageIndex,
                onPageChange = { selectedPageIndex = it },
                onExportDocx = {
                    if (!isExportingDocx) {
                        isExportingDocx = true
                        scope.launch {
                            exportAndShareDocx(context, gridPlan)
                            isExportingDocx = false
                        }
                    }
                },
                onSaveA4HiRes = {
                    scope.launch {
                        saveA4HighResImage(context, gridPlan, selectedPageIndex)
                    }
                }
            )
        }
    }

    // Edit Dimensions Dialog
    itemToEditDimensions?.let { item ->
        EditPhotoDimensionDialog(
            item = item,
            onDismiss = { itemToEditDimensions = null },
            onConfirm = { updatedItem ->
                val idx = photoItems.indexOfFirst { it.id == updatedItem.id }
                if (idx != -1) {
                    photoItems[idx] = updatedItem
                }
                itemToEditDimensions = null
            }
        )
    }
}

@Composable
fun PhotosListTab(
    photoItems: List<PrintPhotoItem>,
    onAddPhotos: () -> Unit,
    onUpdateItem: (PrintPhotoItem) -> Unit,
    onDeleteItem: (PrintPhotoItem) -> Unit,
    onEditDimensions: (PrintPhotoItem) -> Unit,
    onGoToPreview: () -> Unit
) {
    if (photoItems.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "لا توجد صور مضافة بعد",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اضغط على زر (إضافة صور) لاستيراد الصور من جهازك، وتحديد أبعاد وعدد نسخ كل صورة لطباعتها معاً على نفس ورقة A4.",
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAddPhotos,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_add_photos_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استيراد الصور من الجهاز")
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(photoItems, key = { _, it -> it.id }) { index, item ->
                    PhotoItemCard(
                        item = item,
                        index = index + 1,
                        onUpdate = onUpdateItem,
                        onDelete = { onDeleteItem(item) },
                        onEditDimensions = { onEditDimensions(item) }
                    )
                }
            }

            // Bottom bar with summary and proceed button
            Surface(
                color = Color(0xFF1E293B),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val totalCopies = photoItems.sumOf { it.copiesCount }
                        Text(
                            text = "المجموع: $totalCopies صورة من ${photoItems.size} نوع",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "مسافة 1 ملم | بدون خطوط مقطعة | 6 بالسطر (3سم)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onGoToPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("view_a4_sheet_btn")
                    ) {
                        Text("عرض ورقة A4")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItemCard(
    item: PrintPhotoItem,
    index: Int,
    onUpdate: (PrintPhotoItem) -> Unit,
    onDelete: () -> Unit,
    onEditDimensions: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = item.sourceBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info & Title
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صورة #$index (${item.dimensionDisplayText})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (item.widthCm == 3.0f) "تأخذ 6 صور بالسطر كحد أقصى" else "أبعاد خاصة",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Copies Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "عدد النسخ:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )

                        IconButton(
                            onClick = {
                                if (item.copiesCount > 1) {
                                    onUpdate(item.copy(copiesCount = item.copiesCount - 1))
                                }
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFF334155), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = Color.White, modifier = Modifier.size(14.dp))
                        }

                        Text(
                            text = "${item.copiesCount}",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        IconButton(
                            onClick = {
                                onUpdate(item.copy(copiesCount = item.copiesCount + 1))
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFF0284C7), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Action buttons: Edit Dimensions & Delete
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditDimensions,
                        modifier = Modifier.size(32.dp).background(Color(0xFF334155), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الأبعاد", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).background(Color(0xFF7F1D1D).copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick preset chips for this item
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PhotoDimension.PRESETS) { preset ->
                    val isSelected = item.widthCm == preset.widthCm && item.heightCm == preset.heightCm
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onUpdate(item.copy(widthCm = preset.widthCm, heightCm = preset.heightCm))
                        },
                        label = { Text(preset.name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun A4PreviewTab(
    gridPlan: A4GridPlan,
    photoItems: List<PrintPhotoItem>,
    previewBitmap: Bitmap?,
    isRendering: Boolean,
    isExporting: Boolean,
    selectedPageIndex: Int,
    onPageChange: (Int) -> Unit,
    onExportDocx: () -> Unit,
    onSaveA4HiRes: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Multi-page navigation (if more than 1 page)
        if (gridPlan.totalPagesCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedPageIndex > 0) onPageChange(selectedPageIndex - 1)
                    },
                    enabled = selectedPageIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الصفحة السابقة", tint = Color.White)
                }

                Text(
                    text = "ورقة ${selectedPageIndex + 1} من ${gridPlan.totalPagesCount}",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                IconButton(
                    onClick = {
                        if (selectedPageIndex < gridPlan.totalPagesCount - 1) onPageChange(selectedPageIndex + 1)
                    },
                    enabled = selectedPageIndex < gridPlan.totalPagesCount - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الصفحة التالية", tint = Color.White)
                }
            }
        }

        // A4 Paper Viewport
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRendering || previewBitmap == null) {
                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(42.dp))
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Standard A4 aspect ratio: 21.0 / 29.7 ≈ 0.707
                    val a4Ratio = 21.0f / 29.7f
                    val containerRatio = maxWidth / maxHeight

                    val frameModifier = if (a4Ratio > containerRatio) {
                        Modifier.fillMaxWidth().aspectRatio(a4Ratio)
                    } else {
                        Modifier.fillMaxSize().aspectRatio(a4Ratio)
                    }

                    // A4 Sheet with solid white photo paper appearance (no borders, no dashed lines)
                    Card(
                        modifier = frameModifier
                            .shadow(16.dp, shape = RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "معاينة صفحة A4 لورق الصور المقوى",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Info badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل ورقة الطباعة (Photo Paper):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "هوامش 0.6 سم | مسافة 1 ملم | 300 DPI",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Export: Word DOCX (In Front of Text)
                Button(
                    onClick = onExportDocx,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_docx_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري بناء ملف Word...")
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير إلى ملف Word (.docx) - التفاف أمام النص",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Export: Save High-Res A4 Image (300 DPI)
                OutlinedButton(
                    onClick = onSaveA4HiRes,
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_hires_a4_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ صفحة A4 صورة فائقة الدقة (300 DPI - ورق مقوى)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• تم استغلال مساحة الورقة كاملة مع هامش 0.6 سم ومسافة 1 ملم وبدون خطوط متقطعة لتسهيل القص والطباعة على ورق الصور المقوى.",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun EditPhotoDimensionDialog(
    item: PrintPhotoItem,
    onDismiss: () -> Unit,
    onConfirm: (PrintPhotoItem) -> Unit
) {
    var widthInput by remember { mutableStateOf(item.widthCm.toString()) }
    var heightInput by remember { mutableStateOf(item.heightCm.toString()) }
    var copiesInput by remember { mutableStateOf(item.copiesCount.toString()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تعديل أبعاد وعدد نسخ الصورة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it },
                        label = { Text("العرض (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("الارتفاع (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = copiesInput,
                    onValueChange = { copiesInput = it },
                    label = { Text("عدد النسخ المطلوب طباعتها") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = widthInput.toFloatOrNull() ?: item.widthCm
                            val h = heightInput.toFloatOrNull() ?: item.heightCm
                            val c = copiesInput.toIntOrNull() ?: item.copiesCount
                            if (w > 0f && h > 0f && c > 0) {
                                onConfirm(
                                    item.copy(
                                        widthCm = w,
                                        heightCm = h,
                                        copiesCount = c
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("حفظ التعديل")
                    }
                }
            }
        }
    }
}

/**
 * Generates Word DOCX with floating images in front of text and triggers share intent.
 */
private suspend fun exportAndShareDocx(
    context: Context,
    plan: A4GridPlan
) {
    try {
        val fileName = "PhotoGrid_A4_Multi_${System.currentTimeMillis()}.docx"
        val docxFile = DocxExportHelper.generateMultiPhotoDocx(
            context = context,
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
            putExtra(Intent.EXTRA_SUBJECT, "شبكة صور A4 لورق الصور المقوى")
            putExtra(Intent.EXTRA_TEXT, "تم إنشاء ملف Word (.docx) لصفحة A4 بهامش 0.6 سم ومسافة 1 ملم والتفاف حر أمام النص.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو فتح ملف Word"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "خطأ في تصدير Word: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Saves ultra-high-resolution A4 image (300 DPI) for photo paper to device storage.
 */
private suspend fun saveA4HighResImage(
    context: Context,
    plan: A4GridPlan,
    pageIndex: Int
) {
    try {
        Toast.makeText(context, "جاري معالجة صفحة A4 بدقة 300 DPI لورق الصور المقوى...", Toast.LENGTH_SHORT).show()
        val a4HiRes = BitmapProcessor.renderA4Sheet(
            plan = plan,
            pageIndex = pageIndex,
            dpi = 300
        )

        val fileName = "PhotoGrid_A4_PhotoPaper_${System.currentTimeMillis()}.png"
        saveBitmapToGallery(context, a4HiRes, fileName)
        Toast.makeText(context, "تم حفظ صفحة A4 بنجاح بأعلى دقة في مجلد الصور!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
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
