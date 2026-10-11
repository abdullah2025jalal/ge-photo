package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.PhotoDimension
import com.example.util.BitmapProcessor
import java.io.InputStream
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    currentDimension: PhotoDimension,
    onDimensionChanged: (PhotoDimension) -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val loadedBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (loadedBitmap != null) {
                    val cropped = BitmapProcessor.cropToAspectRatio(loadedBitmap, currentDimension.aspectRatio)
                    onPhotoCaptured(cropped)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showCustomDimensionDialog by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var showPoseGuide by remember { mutableStateOf(true) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Dimension selector & quick presets
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C2541).copy(alpha = 0.92f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF48CAE4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أبعاد الصورة المطلوبة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { showCustomDimensionDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0077B6)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("custom_dimension_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تحديد أبعاد مخصصة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PhotoDimension.PRESETS) { preset ->
                            val isSelected = currentDimension.widthCm == preset.widthCm &&
                                    currentDimension.heightCm == preset.heightCm
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDimensionChanged(preset) },
                                label = {
                                    Text(
                                        text = preset.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00B4D8),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2A3A5E),
                                    labelColor = Color(0xFFE0E6ED)
                                )
                            )
                        }
                    }

                    // Active ratio display
                    Text(
                        text = "القياس الحالي: ${currentDimension.widthCm} × ${currentDimension.heightCm} سم (النسبة: %.2f:1)".format(
                            currentDimension.aspectRatio
                        ),
                        color = Color(0xFF90E0EF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Camera Viewport Container (Dynamically sized and shaped to the exact ratio)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dynamically calculate camera view frame respecting current dimension aspect ratio
                        val targetRatio = currentDimension.aspectRatio
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

                        Box(
                            modifier = frameModifier
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, Color(0xFF48CAE4), RoundedCornerShape(18.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // CameraX PreviewView with ScaleType.FILL_CENTER
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        bindCameraUseCases(
                                            cameraProvider = cameraProvider,
                                            lifecycleOwner = lifecycleOwner,
                                            previewView = previewView,
                                            imageCapture = imageCapture,
                                            lensFacing = lensFacing
                                        )
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                update = { previewView ->
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        bindCameraUseCases(
                                            cameraProvider = cameraProvider,
                                            lifecycleOwner = lifecycleOwner,
                                            previewView = previewView,
                                            imageCapture = imageCapture,
                                            lensFacing = lensFacing
                                        )
                                    }, ContextCompat.getMainExecutor(context))
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Silhouette / Pose alignment guide
                            if (showPoseGuide) {
                                PoseGuideOverlay(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Watermark / Dimension badge inside camera viewport
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${currentDimension.widthCm}×${currentDimension.heightCm} cm",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    // No permission fallback card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF48CAE4),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "إذن الكاميرا مطلوب",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يرجى منح صلاحية الكاميرا لالتقاط صورة المعاملات بالقياسات المطلوبة",
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                            ) {
                                Text("منح الصلاحية")
                            }
                        }
                    }
                }
            }

            // Camera Controls Bottom Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B132B).copy(alpha = 0.95f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pick from gallery
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1C2541), CircleShape)
                            .testTag("gallery_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "اختيار من المعرض",
                            tint = Color(0xFF48CAE4),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Flash mode toggle
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1C2541), CircleShape)
                            .testTag("flash_toggle_btn")
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "الفلاش",
                            tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Color(0xFFFFD166) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Shutter button (Large circular button)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (!isCapturing && hasCameraPermission) {
                                    isCapturing = true
                                    takePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        lensFacing = lensFacing,
                                        targetRatio = currentDimension.aspectRatio,
                                        onSuccess = { bitmap ->
                                            isCapturing = false
                                            onPhotoCaptured(bitmap)
                                        },
                                        onError = {
                                            isCapturing = false
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF00B4D8), CircleShape)
                                .testTag("shutter_btn")
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "التقاط الصورة",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // Toggle Pose Alignment Guide
                    IconButton(
                        onClick = { showPoseGuide = !showPoseGuide },
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (showPoseGuide) Color(0xFF0077B6) else Color(0xFF1C2541),
                                CircleShape
                            )
                            .testTag("pose_guide_toggle_btn")
                    ) {
                        Text(
                            text = if (showPoseGuide) "دليل ON" else "دليل OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Flip camera (Front / Back)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1C2541), CircleShape)
                            .testTag("switch_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "تبديل الكاميرا",
                            tint = Color(0xFF48CAE4),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Custom dimension dialog
        if (showCustomDimensionDialog) {
            CustomDimensionDialog(
                currentDimension = currentDimension,
                onDismiss = { showCustomDimensionDialog = false },
                onConfirm = { customDim ->
                    showCustomDimensionDialog = false
                    onDimensionChanged(customDim)
                }
            )
        }
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int
) {
    try {
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    lensFacing: Int,
    targetRatio: Float,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Fix rotation & mirror for front camera
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val matrix = Matrix().apply {
                        postRotate(rotationDegrees.toFloat())
                        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            postScale(-1f, 1f)
                        }
                    }
                    val orientedBitmap = Bitmap.createBitmap(
                        rawBitmap,
                        0,
                        0,
                        rawBitmap.width,
                        rawBitmap.height,
                        matrix,
                        true
                    )

                    // Crop to exact aspect ratio
                    val croppedBitmap = BitmapProcessor.cropToAspectRatio(orientedBitmap, targetRatio)

                    ContextCompat.getMainExecutor(context).execute {
                        onSuccess(croppedBitmap)
                    }
                } catch (e: Exception) {
                    ContextCompat.getMainExecutor(context).execute {
                        onError(e)
                    }
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute {
                    onError(exception)
                }
            }
        }
    )
}

@Composable
fun PoseGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokePaint = Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f), 0f)
        )
        val guideColor = Color(0xAAFFFFFF)

        // Draw oval for head
        val headWidth = w * 0.44f
        val headHeight = h * 0.40f
        val headLeft = (w - headWidth) / 2f
        val headTop = h * 0.12f

        drawOval(
            color = guideColor,
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            style = strokePaint
        )

        // Eye line guideline
        val eyeY = headTop + headHeight * 0.45f
        drawLine(
            color = Color(0x6648CAE4),
            start = Offset(headLeft + 15f, eyeY),
            end = Offset(headLeft + headWidth - 15f, eyeY),
            strokeWidth = 2f
        )

        // Draw shoulders arc path
        val shoulderPath = Path().apply {
            val startX = w * 0.08f
            val endX = w * 0.92f
            val shoulderY = headTop + headHeight + (h * 0.05f)

            moveTo(startX, h)
            quadraticTo(
                w * 0.18f, shoulderY,
                (w - headWidth) / 2f + 10f, shoulderY - 15f
            )
            moveTo((w + headWidth) / 2f - 10f, shoulderY - 15f)
            quadraticTo(
                w * 0.82f, shoulderY,
                endX, h
            )
        }

        drawPath(
            path = shoulderPath,
            color = guideColor,
            style = strokePaint
        )
    }
}

@Composable
fun CustomDimensionDialog(
    currentDimension: PhotoDimension,
    onDismiss: () -> Unit,
    onConfirm: (PhotoDimension) -> Unit
) {
    var widthInput by remember { mutableStateOf(currentDimension.widthCm.toString()) }
    var heightInput by remember { mutableStateOf(currentDimension.heightCm.toString()) }
    var nameInput by remember { mutableStateOf("مخصص") }

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
                    text = "تحديد أبعاد الصورة بالسنتيمتر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "أدخل العرض والارتفاع المطلوب للصورة بدقة",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it },
                        label = { Text("العرض (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_width_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("الارتفاع (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_height_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val parsedWidth = widthInput.toFloatOrNull() ?: 0f
                val parsedHeight = heightInput.toFloatOrNull() ?: 0f
                val ratio = if (parsedHeight > 0f) parsedWidth / parsedHeight else 0f

                Text(
                    text = if (parsedWidth > 0 && parsedHeight > 0) {
                        "النسبة الناتجة: %.2f:1 | دقة 300DPI: %d × %d بكسل".format(
                            ratio,
                            ((parsedWidth / 2.54f) * 300).toInt(),
                            ((parsedHeight / 2.54f) * 300).toInt()
                        )
                    } else {
                        "يرجى إدخال أرقام صحيحة"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            val w = widthInput.toFloatOrNull() ?: 3.0f
                            val h = heightInput.toFloatOrNull() ?: 4.0f
                            if (w > 0f && h > 0f) {
                                onConfirm(
                                    PhotoDimension(
                                        name = "$w × $h سم",
                                        widthCm = w,
                                        heightCm = h,
                                        isCustom = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.testTag("confirm_custom_dimension_btn")
                    ) {
                        Text("تطبيق الأبعاد")
                    }
                }
            }
        }
    }
}
