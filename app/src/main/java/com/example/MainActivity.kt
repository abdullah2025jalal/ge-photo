package com.example

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.model.PhotoDimension
import com.example.ui.A4ExportScreen
import com.example.ui.CameraScreen
import com.example.ui.SuitFittingScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface AppScreen {
    data object Camera : AppScreen
    data class SuitFitting(val photo: Bitmap) : AppScreen
    data class A4Export(val processedPhoto: Bitmap) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PhotoGridApp()
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridApp() {
    var currentDimension by remember { mutableStateOf(PhotoDimension.DEFAULT) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Camera) }

    when (val screen = currentScreen) {
        is AppScreen.Camera -> {
            CameraScreen(
                currentDimension = currentDimension,
                onDimensionChanged = { newDim ->
                    currentDimension = newDim
                },
                onPhotoCaptured = { bitmap ->
                    currentScreen = AppScreen.SuitFitting(bitmap)
                }
            )
        }

        is AppScreen.SuitFitting -> {
            SuitFittingScreen(
                rawPhoto = screen.photo,
                dimension = currentDimension,
                onBack = {
                    currentScreen = AppScreen.Camera
                },
                onProceedToGrid = { processedPhoto ->
                    currentScreen = AppScreen.A4Export(processedPhoto)
                }
            )
        }

        is AppScreen.A4Export -> {
            A4ExportScreen(
                processedPhoto = screen.processedPhoto,
                dimension = currentDimension,
                onBack = {
                    currentScreen = AppScreen.SuitFitting(screen.processedPhoto)
                }
            )
        }
    }
}
