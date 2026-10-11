package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PhotoDimension
import com.example.model.SuitCategory
import com.example.model.SuitItem
import com.example.model.SuitTransform
import com.example.util.BitmapProcessor
import com.example.util.PoseHelper
import com.example.util.SegmentationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuitFittingScreen(
    rawPhoto: Bitmap,
    dimension: PhotoDimension,
    onBack: () -> Unit,
    onProceedToGrid: (processedPhoto: Bitmap) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Processing states
    var isSegmenting by remember { mutableStateOf(true) }
    var isolatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSuit by remember { mutableStateOf(SuitItem.ALL_SUITS[0]) }
    var suitBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var autoTransform by remember { mutableStateOf(SuitTransform.DEFAULT) }

    // Interactive transform states
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    // Background color choice
    val backgroundColors = listOf(
        Pair("أبيض نقي", android.graphics.Color.WHITE),
        Pair("أزرق فاتح", android.graphics.Color.rgb(225, 238, 250)),
        Pair("رمادي فاتح", android.graphics.Color.rgb(235, 238, 242)),
        Pair("عاجي", android.graphics.Color.rgb(250, 250, 245))
    )
    var selectedBgColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }

    // Active Category tab
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        Pair("الكل", SuitCategory.ALL),
        Pair("قاط رجالي", SuitCategory.MEN),
        Pair("صدرية بنات", SuitCategory.GIRL),
        Pair("بدون", SuitCategory.NONE)
    )

    // Load initial suit
    LaunchedEffect(selectedSuit) {
        suitBitmap = BitmapProcessor.loadSuitBitmap(context, selectedSuit.drawableResId)
    }

    // Run ML Kit Selfie Segmentation and Pose Detection
    LaunchedEffect(rawPhoto, selectedBgColor) {
        isSegmenting = true
        scope.launch {
            try {
                // 1. Isolate person and set pure background color
                val isolated = SegmentationHelper.removeBackground(rawPhoto, selectedBgColor)
                isolatedBitmap = isolated

                // 2. Pose detection for auto-aligning suit
                val poseTransform = PoseHelper.estimateSuitPlacement(isolated)
                autoTransform = poseTransform

                // Apply initial transform
                offsetX = poseTransform.offsetX
                offsetY = poseTransform.offsetY
                scale = poseTransform.scale
                rotation = poseTransform.rotation
            } catch (e: Exception) {
                e.printStackTrace()
                isolatedBitmap = rawPhoto
            } finally {
                isSegmenting = false
            }
        }
    }

    // Dynamic composite bitmap for preview and export
    val currentTransform = remember(offsetX, offsetY, scale, rotation) {
        SuitTransform(
            offsetX = offsetX,
            offsetY = offsetY,
            scale = scale,
            rotation = rotation
        )
    }

    val previewComposite = remember(isolatedBitmap, suitBitmap, currentTransform) {
        val base = isolatedBitmap ?: rawPhoto
        if (selectedSuit.category == SuitCategory.NONE || suitBitmap == null) {
            base
        } else {
            BitmapProcessor.compositeSuit(base, suitBitmap, currentTransform)
        }
    }

    var showFineTunePanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .testTag("back_to_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "عزل الخلفية وتلبيس البدلة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${dimension.name} (${dimension.displayText})",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
            }

            // Next button to A4 grid
            Button(
                onClick = {
                    val finalScaledPhoto = BitmapProcessor.scaleToDimension(previewComposite, dimension)
                    onProceedToGrid(finalScaledPhoto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("proceed_to_grid_btn")
            ) {
                Text("شبكة A4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Main preview container with interactive touch manipulation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSegmenting) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF38BDF8),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "جاري عزل الخلفية بالذكاء الاصطناعي (ML Kit)...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "استبدال الخلفية بالأبيض النقي وضبط مقاسات البدلة",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val targetRatio = dimension.aspectRatio
                    val containerRatio = maxWidth / maxHeight

                    val frameModifier = if (targetRatio > containerRatio) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(targetRatio)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .aspectRatio(targetRatio)
                    }

                    // Interactive touch box: user can drag or pinch on the preview directly!
                    Box(
                        modifier = frameModifier
                            .clip(RoundedCornerShape(16.dp))
                            .shadow(12.dp)
                            .background(Color.White)
                            .border(2.dp, Color(0xFF0284C7), RoundedCornerShape(16.dp))
                            .pointerInput(selectedSuit) {
                                if (selectedSuit.category != SuitCategory.NONE) {
                                    detectTransformGestures { _, pan, zoom, rot ->
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        scale = (scale * zoom).coerceIn(0.4f, 2.5f)
                                        rotation += rot
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewComposite.asImageBitmap(),
                            contentDescription = "معاينة الصورة المعالجة",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating hint badge
                        if (selectedSuit.category != SuitCategory.NONE) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "اسحب أو كبّر بالأصابع لضبط البدلة بدقة",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Customization Controls
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
                // Category tabs
                TabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .height(38.dp)
                ) {
                    categories.forEachIndexed { index, (title, _) ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Suit Selection Carousel
                val filteredSuits = when (categories[selectedCategoryIndex].second) {
                    SuitCategory.ALL -> SuitItem.ALL_SUITS
                    SuitCategory.MEN -> SuitItem.ALL_SUITS.filter { it.category == SuitCategory.MEN || it.category == SuitCategory.NONE }
                    SuitCategory.GIRL -> SuitItem.ALL_SUITS.filter { it.category == SuitCategory.GIRL || it.category == SuitCategory.NONE }
                    SuitCategory.NONE -> SuitItem.ALL_SUITS.filter { it.category == SuitCategory.NONE }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredSuits) { suit ->
                        val isSelected = selectedSuit.id == suit.id
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { selectedSuit = suit }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("suit_item_${suit.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFF0F172A)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (suit.drawableResId != 0) {
                                        Image(
                                            painter = painterResource(id = suit.drawableResId),
                                            contentDescription = suit.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = suit.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Background color selector & Fine-tune toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لون الخلفية:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        backgroundColors.forEach { (name, colorInt) ->
                            val isColorSelected = selectedBgColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isColorSelected) 2.5.dp else 1.dp,
                                        color = if (isColorSelected) Color(0xFF0284C7) else Color(0xFF64748B),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBgColor = colorInt },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = if (colorInt == android.graphics.Color.WHITE) Color.Black else Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedSuit.category != SuitCategory.NONE) {
                        Button(
                            onClick = { showFineTunePanel = !showFineTunePanel },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showFineTunePanel) Color(0xFF0284C7) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("toggle_finetune_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showFineTunePanel) "إخفاء الضبط" else "ضبط دقيق",
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Expandable Fine-Tuning Sliders & Nudge Buttons
                AnimatedVisibility(visible = showFineTunePanel && selectedSuit.category != SuitCategory.NONE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أزرار التحكم الدقيق للملابس",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            IconButton(
                                onClick = {
                                    // Reset to auto pose transform
                                    offsetX = autoTransform.offsetX
                                    offsetY = autoTransform.offsetY
                                    scale = autoTransform.scale
                                    rotation = autoTransform.rotation
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "إعادة التعيين",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Sliders: Scale & Rotation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الحجم: %.2f".format(scale),
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                modifier = Modifier.width(75.dp)
                            )
                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الميلان: %.0f°".format(rotation),
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                modifier = Modifier.width(75.dp)
                            )
                            Slider(
                                value = rotation,
                                onValueChange = { rotation = it },
                                valueRange = -30f..30f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }

                        // Directional Nudge Pad (Up, Down, Left, Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تحريك:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            IconButton(
                                onClick = { offsetX -= 8f },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "يسار", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { offsetY -= 8f },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { offsetY += 8f },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { offsetX += 8f },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "يمين", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
