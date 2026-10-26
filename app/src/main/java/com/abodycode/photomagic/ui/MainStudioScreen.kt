package com.abodycode.photomagic.ui

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abodycode.photomagic.R
import com.abodycode.photomagic.model.A4GridPlan
import com.abodycode.photomagic.model.DocumentDuplexMode
import com.abodycode.photomagic.model.ItemType
import com.abodycode.photomagic.model.PrintPhotoItem
import com.abodycode.photomagic.ui.auth.StudioStrings
import com.abodycode.photomagic.ui.auth.studioStringsFor
import com.abodycode.photomagic.util.BitmapProcessor
import com.abodycode.photomagic.util.DocxExportHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/** Visual scheme for the studio surface — dark keeps the original
 *  slate-blue design, light mirrors it on white/blue-gray so the WHOLE
 *  screen (cards, tabs, dialogs, inputs, badges) follows the chosen mode. */
data class StudioPalette(
    val background: Color,
    val surface: Color,
    val frame: Color,
    val text: Color,
    val textHigh: Color,
    val textDim: Color,
    val textFaint: Color,
    val accent: Color,
    val action: Color,
    val blue: Color,
    val dangerBg: Color,
    val danger: Color,
    val photoBadge: Color,
    val docBadge: Color,
    val canvas: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val chipBg: Color,
    val chipText: Color,
)

fun studioPalette(dark: Boolean): StudioPalette = if (dark) {
    StudioPalette(
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        frame = Color(0xFF334155),
        text = Color.White,
        textHigh = Color(0xFFE2E8F0),
        textDim = Color(0xFF94A3B8),
        textFaint = Color(0xFF64748B),
        accent = Color(0xFF38BDF8),
        action = Color(0xFF0284C7),
        blue = Color(0xFF2563EB),
        dangerBg = Color(0xFF7F1D1D).copy(alpha = 0.6f),
        danger = Color(0xFFF87171),
        photoBadge = Color(0xFF0F766E),
        docBadge = Color(0xFF1E3A8A),
        canvas = Color.Black,
        inputBg = Color(0xFF0F172A),
        inputBorder = Color(0xFF475569),
        chipBg = Color(0xFF0F172A),
        chipText = Color(0xFFCBD5E1),
    )
} else {
    StudioPalette(
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        frame = Color(0xFFCBD5E1),
        text = Color(0xFF0F172A),
        textHigh = Color(0xFF1E293B),
        textDim = Color(0xFF475569),
        textFaint = Color(0xFF94A3B8),
        accent = Color(0xFF0284C7),
        action = Color(0xFF0284C7),
        blue = Color(0xFF2563EB),
        dangerBg = Color(0xFFFEE2E2),
        danger = Color(0xFFDC2626),
        photoBadge = Color(0xFF0F766E),
        docBadge = Color(0xFF1E3A8A),
        canvas = Color.Black,
        inputBg = Color.White,
        inputBorder = Color(0xFF94A3B8),
        chipBg = Color.White,
        chipText = Color(0xFF475569),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(locale: String, dark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val t: StudioStrings = studioStringsFor(locale)
    val p: StudioPalette = studioPalette(dark)

    // Queue of photos / documents
    val printItems = remember { mutableStateListOf<PrintPhotoItem>() }

    // Active Tab: 0 = Queue & Settings, 1 = A4 Multi-Page Word View
    var activeTabIndex by remember { mutableIntStateOf(0) }

    // Dialog state for adding document
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showAddDocumentDialog by remember { mutableStateOf(false) }
    var itemToEditDimensions by remember { mutableStateOf<PrintPhotoItem?>(null) }

    // Whether the back button has a step to return to (dialog open, or the
    // preview tab is active). Mirrors the system BackHandler conditions.
    val canBackStep = activeTabIndex != 0 || showAddTypeDialog ||
        showAddDocumentDialog || itemToEditDimensions != null

    // Handle system Back gesture / hardware back button:
    // Returns to previous step (closes dialogs or switches from Tab 1 to Tab 0)
    BackHandler(
        enabled = canBackStep
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

    fun backOneStep() {
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
            uris.forEach { uri ->
                val bitmap = BitmapProcessor.loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    val count = printItems.count { it.itemType == ItemType.PHOTO } + 1
                    printItems.add(
                        PrintPhotoItem(
                            name = t.photoName(count),
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

    // Grid calculation (Calculates row wrapping and prevents photo/document overlap)
    val gridPlan = remember(printItems.toList()) {
        A4GridPlan.calculate(
            items = printItems,
            marginCm = 0.6f,   // Exactly 0.6 cm margin
            spacingCm = 0.1f   // Exactly 1 mm (0.1 cm) spacing
        )
    }

    // Rendered pages cache for multi-page Word-like vertical view
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    var isRenderingPreview by remember { mutableStateOf(false) }
    var isExportingDocx by remember { mutableStateOf(false) }

    LaunchedEffect(gridPlan, activeTabIndex) {
        if (printItems.isNotEmpty()) {
            isRenderingPreview = true
            scope.launch {
                try {
                    val newMap = mutableMapOf<Int, Bitmap>()
                    for (pageIdx in 0 until gridPlan.totalPagesCount) {
                        val preview = BitmapProcessor.renderA4Sheet(
                            plan = gridPlan,
                            pageIndex = pageIdx,
                            dpi = 150
                        )
                        newMap[pageIdx] = preview
                    }
                    pageBitmaps.clear()
                    pageBitmaps.putAll(newMap)
                } finally {
                    isRenderingPreview = false
                }
            }
        } else {
            pageBitmaps.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
    ) {
        // ABODY CODE hero: big CENTERED logo, with the working back button
        // beside it (fixed left edge, like the app badge / home in the top
        // bar) and the centered "add photo" button right below the logo.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.abody_code_logo),
                    contentDescription = "ABODY CODE",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
                IconButton(
                    onClick = { backOneStep() },
                    enabled = canBackStep,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .testTag("back_step_btn"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = t.back,
                        tint = if (canBackStep) p.accent else p.textFaint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Centered "add photo" button right below the big logo
        Button(
            onClick = { showAddTypeDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = p.action),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("add_item_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = t.addPhotoBtn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = activeTabIndex,
            containerColor = p.background,
            contentColor = p.text,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .height(38.dp)
        ) {
            Tab(
                selected = activeTabIndex == 0,
                onClick = { activeTabIndex = 0 },
                text = {
                    Text(
                        text = t.tabItems(printItems.size),
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
                        text = t.tabPreview(gridPlan.totalPagesCount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        // Tab Content
        if (activeTabIndex == 0) {
            PhotosListTab(
                strings = t,
                palette = p,
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
            A4WordMultiPageTab(
                strings = t,
                palette = p,
                gridPlan = gridPlan,
                pageBitmaps = pageBitmaps,
                isRendering = isRenderingPreview,
                isExporting = isExportingDocx,
                onExportDocx = {
                    if (!isExportingDocx) {
                        isExportingDocx = true
                        scope.launch {
                            exportAndShareDocx(context, gridPlan, t)
                            isExportingDocx = false
                        }
                    }
                },
                onSavePageHiRes = { pageIdx ->
                    scope.launch {
                        saveA4HighResImage(context, gridPlan, pageIdx, t)
                    }
                },
                onSaveAllPagesHiRes = {
                    scope.launch {
                        for (pg in 0 until gridPlan.totalPagesCount) {
                            saveA4HighResImage(context, gridPlan, pg, t)
                        }
                    }
                }
            )
        }
    }

    // Selection dialog: Add Standard Photo vs Add Document
    if (showAddTypeDialog) {
        AddTypeChoiceDialog(
            strings = t,
            palette = p,
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
            strings = t,
            palette = p,
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
            strings = t,
            palette = p,
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
    strings: StudioStrings,
    palette: StudioPalette,
    items: List<PrintPhotoItem>,
    onAddNew: () -> Unit,
    onUpdateItem: (PrintPhotoItem) -> Unit,
    onDeleteItem: (PrintPhotoItem) -> Unit,
    onEditDimensions: (PrintPhotoItem) -> Unit,
    onGoToPreview: () -> Unit
) {
    val t = strings
    val p = palette
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = p.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = p.accent,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = t.emptyTitle,
                        color = p.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t.emptyBody,
                        color = p.textDim,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAddNew,
                        colors = ButtonDefaults.buttonColors(containerColor = p.action),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t.emptyAdd)
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
                        strings = t,
                        palette = p,
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
                color = p.surface,
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
                            text = t.total(photosCount, docsCount),
                            color = p.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = t.specsShort,
                            color = p.accent,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onGoToPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = p.blue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(t.viewPaper)
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
    strings: StudioStrings,
    palette: StudioPalette,
    item: PrintPhotoItem,
    index: Int,
    onUpdate: (PrintPhotoItem) -> Unit,
    onDelete: () -> Unit,
    onEditDimensions: () -> Unit
) {
    val t = strings
    val p = palette
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, p.frame, RoundedCornerShape(14.dp))
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
                        color = if (item.itemType == ItemType.DOCUMENT) p.docBadge else p.photoBadge,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (item.itemType == ItemType.DOCUMENT) t.docBadge else t.photoBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        color = p.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Explicitly separated Edit and Delete buttons (no overlap, minimum 10.dp spacing)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditDimensions,
                        modifier = Modifier
                            .size(36.dp)
                            .background(p.frame, RoundedCornerShape(8.dp))
                            .testTag("edit_item_btn_$index")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = t.edit, tint = p.accent, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(p.dangerBg, RoundedCornerShape(8.dp))
                            .testTag("delete_item_btn_$index")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = t.del, tint = p.danger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Thumbnails & Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.itemType == ItemType.DOCUMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(p.canvas),
                                contentAlignment = Alignment.Center
                            ) {
                                item.sourceBitmap?.let { bmp ->
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = t.front, modifier = Modifier.fillMaxSize())
                                } ?: Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray)
                            }
                            Text(t.front, color = p.textDim, fontSize = 10.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(p.canvas),
                                contentAlignment = Alignment.Center
                            ) {
                                item.backBitmap?.let { bmp ->
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = t.backFace, modifier = Modifier.fillMaxSize())
                                } ?: Text(t.none, color = p.textFaint, fontSize = 9.sp)
                            }
                            Text(t.backFace, color = p.textDim, fontSize = 10.sp)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.canvas),
                        contentAlignment = Alignment.Center
                    ) {
                        item.sourceBitmap?.let { bmp ->
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = item.name, modifier = Modifier.fillMaxSize())
                        } ?: Icon(Icons.Default.Photo, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t.dims(item.dimensionDisplayText),
                        color = p.textHigh,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (item.itemType == ItemType.DOCUMENT) {
                        Text(
                            text = if (item.duplexMode == DocumentDuplexMode.SAME_PAGE)
                                t.samePageDetail
                            else
                                t.backSideDetail,
                            color = p.accent,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = if (item.widthCm == 3.0f) t.standardRows else t.customDims,
                            color = p.textDim,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Copies Stepper - Clean uncircled icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = t.copiesLabel,
                            color = p.textHigh,
                            fontSize = 11.sp
                        )

                        IconButton(
                            onClick = {
                                if (item.copiesCount > 1) {
                                    onUpdate(item.copy(copiesCount = item.copiesCount - 1))
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("decrease_copies_$index")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = t.decrease, tint = p.text, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "${item.copiesCount}",
                            color = p.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                onUpdate(item.copy(copiesCount = item.copiesCount + 1))
                            },
                            modifier = Modifier.size(32.dp).testTag("increase_copies_$index")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = t.increase, tint = p.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTypeChoiceDialog(
    strings: StudioStrings,
    palette: StudioPalette,
    onDismiss: () -> Unit,
    onSelectPhoto: () -> Unit,
    onSelectDocument: () -> Unit
) {
    val t = strings
    val p = palette
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = t.chooseTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = p.text
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPhoto() }
                        .border(1.dp, p.action, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = p.inputBg)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = p.accent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(t.photoOptionTitle, color = p.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(t.photoOptionBody, color = p.textDim, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDocument() }
                        .border(1.dp, p.accent, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = p.inputBg)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = p.accent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(t.docOptionTitle, color = p.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(t.docOptionBody, color = p.textDim, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(t.cancel, color = p.textDim)
                }
            }
        }
    }
}

@Composable
fun AddDocumentDialog(
    strings: StudioStrings,
    palette: StudioPalette,
    onDismiss: () -> Unit,
    onConfirm: (PrintPhotoItem) -> Unit
) {
    val t = strings
    val p = palette
    val context = LocalContext.current
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var widthInput by remember { mutableStateOf("8.5") }
    var heightInput by remember { mutableStateOf("5.4") }
    var copiesInput by remember { mutableStateOf("1") }
    var duplexMode by remember { mutableStateOf(DocumentDuplexMode.SAME_PAGE) }

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
            colors = CardDefaults.cardColors(containerColor = p.surface),
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = t.addDocTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = p.text
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(105.dp)
                            .clickable {
                                pickFrontLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .border(
                                1.5.dp,
                                if (frontBitmap != null) p.accent else p.inputBorder,
                                RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = p.inputBg)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (frontBitmap != null) {
                                Image(bitmap = frontBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                                ) {
                                    Text(t.frontDone, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = p.accent)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(t.frontPick, color = p.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(t.requiredTag, color = p.textDim, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(105.dp)
                            .clickable {
                                pickBackLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .border(
                                1.5.dp,
                                if (backBitmap != null) p.accent else p.inputBorder,
                                RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = p.inputBg)
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
                                        Icon(Icons.Default.Close, contentDescription = t.del, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                                ) {
                                    Text(t.backDone, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = p.textDim)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(t.backPickHint, color = p.text, fontSize = 11.sp)
                                    Text(t.optionalTag, color = p.textDim, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = t.placementTitle,
                    color = p.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { duplexMode = DocumentDuplexMode.SAME_PAGE },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = duplexMode == DocumentDuplexMode.SAME_PAGE,
                        onClick = { duplexMode = DocumentDuplexMode.SAME_PAGE },
                        colors = RadioButtonDefaults.colors(selectedColor = p.accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(t.samePageTitle, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(t.samePageBody, color = p.textDim, fontSize = 10.sp)
                    }
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
                        colors = RadioButtonDefaults.colors(selectedColor = p.accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(t.backSideTitle, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(t.backSideBody, color = p.textDim, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it },
                        label = { Text(t.widthCm) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            focusedContainerColor = p.inputBg,
                            unfocusedContainerColor = p.inputBg,
                            focusedLabelColor = p.accent,
                            unfocusedLabelColor = p.textDim,
                            focusedBorderColor = p.accent,
                            unfocusedBorderColor = p.inputBorder,
                            cursorColor = p.text
                        )
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text(t.heightCm) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            focusedContainerColor = p.inputBg,
                            unfocusedContainerColor = p.inputBg,
                            focusedLabelColor = p.accent,
                            unfocusedLabelColor = p.textDim,
                            focusedBorderColor = p.accent,
                            unfocusedBorderColor = p.inputBorder,
                            cursorColor = p.text
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val docPresets = listOf(
                    Triple(t.presetId, 8.5f, 5.4f),
                    Triple(t.presetPassport, 12.5f, 8.8f),
                    Triple(t.presetHalfA5, 14.8f, 21.0f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(docPresets) { (name, w, h) ->
                        FilterChip(
                            selected = widthInput == w.toString() && heightInput == h.toString(),
                            onClick = {
                                widthInput = w.toString()
                                heightInput = h.toString()
                            },
                            label = { Text(name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = p.action,
                                selectedLabelColor = Color.White,
                                containerColor = p.chipBg,
                                labelColor = p.chipText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = copiesInput,
                    onValueChange = { copiesInput = it },
                    label = { Text(t.copiesPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.text,
                        unfocusedTextColor = p.text,
                        focusedContainerColor = p.inputBg,
                        unfocusedContainerColor = p.inputBg,
                        focusedLabelColor = p.accent,
                        unfocusedLabelColor = p.textDim,
                        focusedBorderColor = p.accent,
                        unfocusedBorderColor = p.inputBorder,
                        cursorColor = p.text
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(t.cancel, color = p.textDim)
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
                                        name = t.docName(w, h),
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
                                Toast.makeText(context, t.needFront, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = p.action)
                    ) {
                        Text(t.addDocAction)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPhotoDimensionDialog(
    strings: StudioStrings,
    palette: StudioPalette,
    item: PrintPhotoItem,
    onDismiss: () -> Unit,
    onConfirm: (PrintPhotoItem) -> Unit
) {
    val t = strings
    val p = palette
    var widthInput by remember { mutableStateOf(item.widthCm.toString()) }
    var heightInput by remember { mutableStateOf(item.heightCm.toString()) }
    var copiesInput by remember { mutableStateOf(item.copiesCount.toString()) }
    var duplexMode by remember { mutableStateOf(item.duplexMode) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = t.editTitle(item.name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = p.text
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it },
                        label = { Text(t.widthCm) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            focusedContainerColor = p.inputBg,
                            unfocusedContainerColor = p.inputBg,
                            focusedLabelColor = p.accent,
                            unfocusedLabelColor = p.textDim,
                            focusedBorderColor = p.accent,
                            unfocusedBorderColor = p.inputBorder,
                            cursorColor = p.text
                        )
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text(t.heightCm) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            focusedContainerColor = p.inputBg,
                            unfocusedContainerColor = p.inputBg,
                            focusedLabelColor = p.accent,
                            unfocusedLabelColor = p.textDim,
                            focusedBorderColor = p.accent,
                            unfocusedBorderColor = p.inputBorder,
                            cursorColor = p.text
                        )
                    )
                }

                if (item.itemType == ItemType.DOCUMENT && item.hasBackFace) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = t.placementTitle,
                        color = p.text,
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
                            colors = RadioButtonDefaults.colors(selectedColor = p.accent)
                        )
                        Text(t.samePageShort, color = p.text, fontSize = 11.sp)
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
                            colors = RadioButtonDefaults.colors(selectedColor = p.accent)
                        )
                        Text(t.backSideShort, color = p.text, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = copiesInput,
                    onValueChange = { copiesInput = it },
                    label = { Text(t.copiesPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.text,
                        unfocusedTextColor = p.text,
                        focusedContainerColor = p.inputBg,
                        unfocusedContainerColor = p.inputBg,
                        focusedLabelColor = p.accent,
                        unfocusedLabelColor = p.textDim,
                        focusedBorderColor = p.accent,
                        unfocusedBorderColor = p.inputBorder,
                        cursorColor = p.text
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(t.cancel, color = p.textDim)
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
                        colors = ButtonDefaults.buttonColors(containerColor = p.action)
                    ) {
                        Text(t.saveEdit)
                    }
                }
            }
        }
    }
}

/**
 * Multi-page vertical scrolling view displaying all A4 sheets smoothly like Microsoft Word
 */
@Composable
fun A4WordMultiPageTab(
    strings: StudioStrings,
    palette: StudioPalette,
    gridPlan: A4GridPlan,
    pageBitmaps: Map<Int, Bitmap>,
    isRendering: Boolean,
    isExporting: Boolean,
    onExportDocx: () -> Unit,
    onSavePageHiRes: (Int) -> Unit,
    onSaveAllPagesHiRes: () -> Unit
) {
    val t = strings
    val p = palette
    Column(modifier = Modifier.fillMaxSize()) {
        if (isRendering && pageBitmaps.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p.accent, modifier = Modifier.size(46.dp))
            }
        } else {
            // Vertical scrolling view showing all pages of the document like Word
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(gridPlan.totalPagesCount) { pageIdx ->
                    val bmp = pageBitmaps[pageIdx]
                    val isBackPage = gridPlan.placedPhotos.any { it.isBackSide && it.pageIndex == pageIdx }
                    val isFrontPage = gridPlan.placedPhotos.any { !it.isBackSide && it.pageIndex == pageIdx }

                    val pageLabel = when {
                        isBackPage && !isFrontPage -> t.sheetBack(pageIdx + 1, gridPlan.totalPagesCount)
                        !isBackPage && isFrontPage -> t.sheetFront(pageIdx + 1, gridPlan.totalPagesCount)
                        else -> t.sheetPlain(pageIdx + 1, gridPlan.totalPagesCount)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Page Header in Word style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isBackPage) p.docBadge else p.photoBadge,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = pageLabel,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            TextButton(
                                onClick = { onSavePageHiRes(pageIdx) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = p.accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t.saveThisSheet, color = p.accent, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // A4 Sheet: Standard 21.0 / 29.7 aspect ratio, solid white photo paper, no dashed borders
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(21.0f / 29.7f)
                                .shadow(12.dp, shape = RoundedCornerShape(4.dp))
                                .border(1.dp, p.frame, RoundedCornerShape(4.dp)),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = pageLabel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = p.accent, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Export Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t.exportSummaryTitle,
                        color = p.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Surface(
                        color = p.accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = t.exportSummaryBadge(gridPlan.totalPagesCount),
                            color = p.accent,
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
                    colors = ButtonDefaults.buttonColors(containerColor = p.blue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t.buildingDocx)
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t.exportDocxBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Export: Save All Pages
                OutlinedButton(
                    onClick = onSaveAllPagesHiRes,
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_all_pages_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.accent)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t.exportAllBtn, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t.exportNote,
                    color = p.textDim,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private suspend fun exportAndShareDocx(
    context: Context,
    plan: A4GridPlan,
    t: StudioStrings
) {
    try {
        val fileName = "PhotoMagic_A4_Multi_${System.currentTimeMillis()}.docx"
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
            putExtra(Intent.EXTRA_SUBJECT, t.shareSubject)
            putExtra(Intent.EXTRA_TEXT, t.shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, t.shareChooser))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, t.wordError(e.message ?: ""), Toast.LENGTH_LONG).show()
    }
}

private suspend fun saveA4HighResImage(
    context: Context,
    plan: A4GridPlan,
    pageIndex: Int,
    t: StudioStrings
) {
    try {
        Toast.makeText(context, t.processing(pageIndex + 1), Toast.LENGTH_SHORT).show()
        val a4HiRes = BitmapProcessor.renderA4Sheet(
            plan = plan,
            pageIndex = pageIndex,
            dpi = 300
        )

        val fileName = "PhotoMagic_A4_p${pageIndex + 1}_${System.currentTimeMillis()}.png"
        saveBitmapToGallery(context, a4HiRes, fileName)
        Toast.makeText(context, t.savedOk(pageIndex + 1), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, t.saveFailed(e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String) {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoMagic")
        }
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoMagic").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
