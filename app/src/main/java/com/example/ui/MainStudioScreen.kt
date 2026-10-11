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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewAgenda
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.model.DocumentDuplexMode
import com.example.model.ItemType
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

    // Queue of photos / documents
    val printItems = remember { mutableStateListOf<PrintPhotoItem>() }

    // Active Tab: 0 = Queue & Settings, 1 = A4 Preview & Export
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    // Dialog state for adding document
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showAddDocumentDialog by remember { mutableStateOf(false) }
    var itemToEditDimensions by remember { mutableStateOf<PrintPhotoItem?>(null) }

    // Handle system Back gesture / hardware back button:
    // Returns to previous step (closes dialogs or switches from Tab 1 to Tab 0)
    BackHandler(
        enabled = activeTabIndex != 0 || showAddTypeDialog || showAddDocumentDialog || itemToEditDimensions != null
    ) {
        if (itemToEditDimensions != null) {
            itemToEditDimensions = null
        } else if (showAddDocumentDialog) {
            showAddDocumentDialog = false
        } else if (showAddTypeDialog) {
            showAddTypeDialog = false
        } else if (activeTabIndex != 0) {
            activeTabIndex = 0
        }
    }

    // Photo picker for standard photos
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEachIndexed { idx, uri ->
                val bitmap = BitmapProcessor.loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    val count = printItems.count { it.itemType == ItemType.PHOTO } + 1
                    printItems.add(
                        PrintPhotoItem(
                            name = "صورة $count",
                            itemType = ItemType.PHOTO,
                            sourceBitmap = bitmap,
                            uri = uri,
                            widthCm = 3.0f,
                            heightCm = 4.0f,
                            copiesCount = 6
                        )
                    )
                }
            }
        }
    }

    // Grid calculation
    val gridPlan = remember(printItems.toList()) {
        A4GridPlan.calculate(
            items = printItems,
            marginCm = 0.6f,   // Exactly 0.6 cm margin
            spacingCm = 0.1f   // Exactly 1 mm (0.1 cm) spacing
        )
    }

    // A4 preview bitmap
    var a4PreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(false) }
    var isExportingDocx by remember { mutableStateOf(false) }

    LaunchedEffect(gridPlan, selectedPageIndex) {
        if (printItems.isNotEmpty()) {
            isRenderingPreview = true
            scope.launch {
                try {
                    val pageIdx = selectedPageIndex.coerceIn(0, (gridPlan.totalPagesCount - 1).coerceAtLeast(0))
                    val preview = BitmapProcessor.renderA4Sheet(
                        plan = gridPlan,
                        pageIndex = pageIdx,
                        dpi = 150
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "استوديو طباعة شبكة الصور A4",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "ورق مقوى | هوامش 0.6 سم | مسافة 1 ملم | دعم المستندات وجهين",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }

                    // Add button opens selector (Photo vs Document)
                    Button(
                        onClick = { showAddTypeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_item_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة عنصر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
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
                                text = "العناصر (${printItems.size})",
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
            PhotosListTab(
                items = printItems,
                onAddNew = { showAddTypeDialog = true },
                onUpdateItem = { updatedItem ->
                    val idx = printItems.indexOfFirst { it.id == updatedItem.id }
                    if (idx != -1) {
                        printItems[idx] = updatedItem
                    }
                },
                onDeleteItem = { itemToDelete ->
                    printItems.removeIf { it.id == itemToDelete.id }
                },
                onEditDimensions = { item ->
                    itemToEditDimensions = item
                },
                onGoToPreview = {
                    activeTabIndex = 1
                }
            )
        } else {
            A4PreviewTab(
                gridPlan = gridPlan,
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

    // Selection dialog: Add Standard Photo vs Add Document
    if (showAddTypeDialog) {
        AddTypeChoiceDialog(
            onDismiss = { showAddTypeDialog = false },
            onSelectPhoto = {
                showAddTypeDialog = false
                multiPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSelectDocument = {
                showAddTypeDialog = false
                showAddDocumentDialog = true
            }
        )
    }

    // Add Document Dialog (Front & optional Back side with duplex option)
    if (showAddDocumentDialog) {
        AddDocumentDialog(
            onDismiss = { showAddDocumentDialog = false },
            onConfirm = { docItem ->
                printItems.add(docItem)
                showAddDocumentDialog = false
            }
        )
    }

    // Edit Item Dimensions Dialog
    itemToEditDimensions?.let { item ->
        EditPhotoDimensionDialog(
            item = item,
            onDismiss = { itemToEditDimensions = null },
            onConfirm = { updatedItem ->
                val idx = printItems.indexOfFirst { it.id == updatedItem.id }
                if (idx != -1) {
                    printItems[idx] = updatedItem
                }
                itemToEditDimensions = null
            }
        )
    }
}

@Composable
fun PhotosListTab(
    items: List<PrintPhotoItem>,
    onAddNew: () -> Unit,
    onUpdateItem: (PrintPhotoItem) -> Unit,
    onDeleteItem: (PrintPhotoItem) -> Unit,
    onEditDimensions: (PrintPhotoItem) -> Unit,
    onGoToPreview: () -> Unit
) {
    if (items.isEmpty()) {
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
                        text = "لا توجد عناصر مضافة بعد",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يمكنك إضافة صور شخصية عادية أو مستندات (هوية، رخصة، بطاقة مع وجه أمامي وخلفي) لطباعتها بأعلى دقة على ورقة A4.",
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAddNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة صورة أو مستند")
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
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    ItemCard(
                        item = item,
                        index = index + 1,
                        onUpdate = onUpdateItem,
                        onDelete = { onDeleteItem(item) },
                        onEditDimensions = { onEditDimensions(item) }
                    )
                }
            }

            // Bottom summary bar
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
                        val photosCount = items.filter { it.itemType == ItemType.PHOTO }.sumOf { it.copiesCount }
                        val docsCount = items.filter { it.itemType == ItemType.DOCUMENT }.sumOf { it.copiesCount }
                        Text(
                            text = "المجموع: $photosCount صورة | $docsCount مستند",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "هوامش 0.6 سم | مسافة 1 ملم | بدون خطوط مقطعة",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onGoToPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
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
fun ItemCard(
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
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with type badge and clearly separated action buttons (Edit & Delete separated)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (item.itemType == ItemType.DOCUMENT) Color(0xFF1E3A8A) else Color(0xFF0F766E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (item.itemType == ItemType.DOCUMENT) "مستند / هوية" else "صورة عادية",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Explicitly separated Edit and Delete buttons (no overlap, minimum 12.dp spacing)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditDimensions,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .testTag("edit_item_btn_$index")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .testTag("delete_item_btn_$index")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Thumbnails & Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnails
                if (item.itemType == ItemType.DOCUMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Front face thumbnail
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                item.sourceBitmap?.let { bmp ->
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "الأمامي", modifier = Modifier.fillMaxSize())
                                } ?: Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray)
                            }
                            Text("أمامي", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }

                        // Back face thumbnail
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                item.backBitmap?.let { bmp ->
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "خلفي", modifier = Modifier.fillMaxSize())
                                } ?: Text("بدون", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                            Text("خلفي", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        item.sourceBitmap?.let { bmp ->
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = item.name, modifier = Modifier.fillMaxSize())
                        } ?: Icon(Icons.Default.Photo, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Specs & Placement Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "القياس: ${item.dimensionDisplayText}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (item.itemType == ItemType.DOCUMENT) {
                        Text(
                            text = if (item.duplexMode == DocumentDuplexMode.SAME_PAGE)
                                "الوجهان بنفس الصفحة (0.6 سم من اليمين واليسار)"
                            else
                                "الوجه الخلفي في ظهر الورقة (صفحة ثانية)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = if (item.widthCm == 3.0f) "6 صور بالسطر كحد أقصى (عرض 3 سم)" else "أبعاد خاصة",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Copies Stepper - Clean icons WITHOUT circles around + and -
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "عدد النسخ:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )

                        // Clean uncircled minus button
                        IconButton(
                            onClick = {
                                if (item.copiesCount > 1) {
                                    onUpdate(item.copy(copiesCount = item.copiesCount - 1))
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("decrease_copies_$index")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "${item.copiesCount}",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Clean uncircled plus button
                        IconButton(
                            onClick = {
                                onUpdate(item.copy(copiesCount = item.copiesCount + 1))
                            },
                            modifier = Modifier.size(32.dp).testTag("increase_copies_$index")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTypeChoiceDialog(
    onDismiss: () -> Unit,
    onSelectPhoto: () -> Unit,
    onSelectDocument: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "اختر نوع العنصر المراد إضافته",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Choice 1: Regular Photo
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPhoto() }
                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("صورة شخصية / صور عادية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("تتكرر في صفوف شبكة A4 (مثل 3×4 سم، 6 بالسطر)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Choice 2: Document / ID Card (Front & Back)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDocument() }
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("مستند / هوية (وجه أمامي وخلفي)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("وجهان بنفس الصفحة أو في ظهر الورقة (صفحة ثانية)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (PrintPhotoItem) -> Unit
) {
    val context = LocalContext.current
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var widthInput by remember { mutableStateOf("8.5") } // Default ID card width in cm
    var heightInput by remember { mutableStateOf("5.4") } // Default ID card height in cm
    var copiesInput by remember { mutableStateOf("1") }
    var duplexMode by remember { mutableStateOf(DocumentDuplexMode.SAME_PAGE) }

    // Pickers for front and back
    val pickFrontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            frontBitmap = BitmapProcessor.loadBitmapFromUri(context, uri)
        }
    }

    val pickBackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            backBitmap = BitmapProcessor.loadBitmapFromUri(context, uri)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إضافة مستند أو هوية (وجه أمامي وخلفي)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Photos upload section (Front & Optional Back)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Front Face Upload Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(105.dp)
                            .clickable {
                                pickFrontLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .border(
                                1.5.dp,
                                if (frontBitmap != null) Color(0xFF38BDF8) else Color(0xFF475569),
                                RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (frontBitmap != null) {
                                Image(bitmap = frontBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                                ) {
                                    Text("الوجه الأمامي ✔", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("صورة الوجه الأمامي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("(مطلوب)", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Back Face Upload Card (Optional)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(105.dp)
                            .clickable {
                                pickBackLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .border(
                                1.5.dp,
                                if (backBitmap != null) Color(0xFF38BDF8) else Color(0xFF475569),
                                RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (backBitmap != null) {
                                Image(bitmap = backBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                Row(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { backBitmap = null },
                                        modifier = Modifier.size(22.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                                ) {
                                    Text("الوجه الخلفي ✔", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("صورة الوجه الخلفي", color = Color.White, fontSize = 11.sp)
                                    Text("(اختياري)", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Placement Options (Same Page vs Back Side)
                Text(
                    text = "موضع الوجهين في الطباعة:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Radio 1: Same Page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { duplexMode = DocumentDuplexMode.SAME_PAGE },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = duplexMode == DocumentDuplexMode.SAME_PAGE,
                        onClick = { duplexMode = DocumentDuplexMode.SAME_PAGE },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("الوجهان بنفس الصفحة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("الأمامي على اليمين والخلفي على اليسار (0.6 سم من حواف الورقة)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }

                // Radio 2: Back Side (Page 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { duplexMode = DocumentDuplexMode.BACK_SIDE },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = duplexMode == DocumentDuplexMode.BACK_SIDE,
                        onClick = { duplexMode = DocumentDuplexMode.BACK_SIDE },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("الوجه الخلفي في ظهر الورقة (صفحة ثانية)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("لطباعة الوجهين (Duplex) متطابقين ظهر لظهر بدقة", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dimensions Input with explicitly WHITE text color
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
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            cursorColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("الارتفاع (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            cursorColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick preset sizes chips
                val docPresets = listOf(
                    Pair("بطاقة هوية (8.5×5.4)", Pair(8.5f, 5.4f)),
                    Pair("جواز سفر (12.5×8.8)", Pair(12.5f, 8.8f)),
                    Pair("نصف ورقة A5 (14.8×21)", Pair(14.8f, 21.0f))
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(docPresets) { (name, size) ->
                        FilterChip(
                            selected = widthInput == size.first.toString() && heightInput == size.second.toString(),
                            onClick = {
                                widthInput = size.first.toString()
                                heightInput = size.second.toString()
                            },
                            label = { Text(name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = copiesInput,
                    onValueChange = { copiesInput = it },
                    label = { Text("عدد النسخ المطلوب طباعتها") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        cursorColor = Color.White
                    )
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
                            val w = widthInput.toFloatOrNull() ?: 8.5f
                            val h = heightInput.toFloatOrNull() ?: 5.4f
                            val c = copiesInput.toIntOrNull() ?: 1
                            if (frontBitmap != null && w > 0f && h > 0f && c > 0) {
                                onConfirm(
                                    PrintPhotoItem(
                                        name = "مستند ${w}×${h} سم",
                                        itemType = ItemType.DOCUMENT,
                                        sourceBitmap = frontBitmap,
                                        backBitmap = backBitmap,
                                        duplexMode = duplexMode,
                                        widthCm = w,
                                        heightCm = h,
                                        copiesCount = c
                                    )
                                )
                            } else {
                                Toast.makeText(context, "يرجى تحديد صورة الوجه الأمامي للمستند", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("إضافة المستند")
                    }
                }
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
    var duplexMode by remember { mutableStateOf(item.duplexMode) }

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
                    text = "تعديل أبعاد وعدد نسخ: ${item.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dimensions with explicitly WHITE text color
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
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            cursorColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("الارتفاع (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            cursorColor = Color.White
                        )
                    )
                }

                if (item.itemType == ItemType.DOCUMENT && item.hasBackFace) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "موضع الوجهين في الطباعة:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { duplexMode = DocumentDuplexMode.SAME_PAGE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = duplexMode == DocumentDuplexMode.SAME_PAGE,
                            onClick = { duplexMode = DocumentDuplexMode.SAME_PAGE },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Text("الوجهان بنفس الصفحة (يمين ويسار)", color = Color.White, fontSize = 11.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { duplexMode = DocumentDuplexMode.BACK_SIDE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = duplexMode == DocumentDuplexMode.BACK_SIDE,
                            onClick = { duplexMode = DocumentDuplexMode.BACK_SIDE },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Text("الوجه الخلفي بظهر الورقة (صفحة ثانية)", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = copiesInput,
                    onValueChange = { copiesInput = it },
                    label = { Text("عدد النسخ المطلوب طباعتها") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        cursorColor = Color.White
                    )
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
                                        copiesCount = c,
                                        duplexMode = duplexMode
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

@Composable
fun A4PreviewTab(
    gridPlan: A4GridPlan,
    previewBitmap: Bitmap?,
    isRendering: Boolean,
    isExporting: Boolean,
    selectedPageIndex: Int,
    onPageChange: (Int) -> Unit,
    onExportDocx: () -> Unit,
    onSaveA4HiRes: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Multi-page selector (e.g. Page 1 Front, Page 2 Back for duplex documents)
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

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "ورقة ${selectedPageIndex + 1} من أصل ${gridPlan.totalPagesCount}" +
                                if (gridPlan.placedPhotos.any { it.isBackSide && it.pageIndex == selectedPageIndex }) " (الوجه الخلفي)" else " (الوجه الأمامي)",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

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
                    val a4Ratio = 21.0f / 29.7f
                    val containerRatio = maxWidth / maxHeight

                    val frameModifier = if (a4Ratio > containerRatio) {
                        Modifier.fillMaxWidth().aspectRatio(a4Ratio)
                    } else {
                        Modifier.fillMaxSize().aspectRatio(a4Ratio)
                    }

                    // A4 Sheet: clean solid white photo paper without dashed borders
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
            putExtra(Intent.EXTRA_SUBJECT, "شبكة صور ومستندات A4 لورق الصور المقوى")
            putExtra(Intent.EXTRA_TEXT, "تم إنشاء ملف Word (.docx) لصفحة A4 بهامش 0.6 سم ومسافة 1 ملم والتفاف حر أمام النص.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو فتح ملف Word"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "خطأ في تصدير Word: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

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

        val fileName = "PhotoGrid_A4_PhotoPaper_p${pageIndex + 1}_${System.currentTimeMillis()}.png"
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
