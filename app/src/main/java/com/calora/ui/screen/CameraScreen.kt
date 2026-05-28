package com.calora.ui.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calora.camera.CaloraCameraManager
import com.calora.ui.theme.AiAccent
import com.calora.ui.theme.ScanLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraRoute(
    viewModel: CameraViewModel,
    onFoodRecognized: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraManager = remember { CaloraCameraManager() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    DisposableEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        onDispose { cameraManager.stopPreview() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "拍照识别",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("需要摄像头权限", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请在系统设置中授予 Calora 摄像头权限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            cameraManager.startPreview(ctx, lifecycleOwner, previewView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                ScanOverlay(isProcessing = isProcessing)

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PulsingDot()
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = {
                                isProcessing = true
                                cameraManager.takePhoto(
                                    context,
                                    ContextCompat.getMainExecutor(context)
                                ) { uri ->
                                    capturedImageUri = uri
                                    if (uri != null) {
                                        viewModel.classifyFood(uri) {
                                            isProcessing = false
                                            onFoodRecognized()
                                        }
                                    } else {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            content = {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanOverlay(isProcessing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxWidth = canvasWidth * 0.7f
            val boxHeight = boxWidth * 0.75f
            val left = (canvasWidth - boxWidth) / 2f
            val top = (canvasHeight - boxHeight) / 2f - 40.dp.toPx()

            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset.Zero,
                size = Size(canvasWidth, top)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top + boxHeight),
                size = Size(canvasWidth, canvasHeight - top - boxHeight)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top),
                size = Size(left, boxHeight)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(left + boxWidth, top),
                size = Size(canvasWidth - left - boxWidth, boxHeight)
            )

            val cornerLen = 40f
            val cornerWidth = 6f
            val cornerColor = if (isProcessing) AiAccent else ScanLine
            val corners = listOf(
                Offset(left, top),
                Offset(left + boxWidth, top),
                Offset(left, top + boxHeight),
                Offset(left + boxWidth, top + boxHeight)
            )
            drawLine(cornerColor, corners[0], Offset(corners[0].x + cornerLen, corners[0].y), cornerWidth)
            drawLine(cornerColor, corners[0], Offset(corners[0].x, corners[0].y + cornerLen), cornerWidth)
            drawLine(cornerColor, corners[1], Offset(corners[1].x - cornerLen, corners[1].y), cornerWidth)
            drawLine(cornerColor, corners[1], Offset(corners[1].x, corners[1].y + cornerLen), cornerWidth)
            drawLine(cornerColor, corners[2], Offset(corners[2].x + cornerLen, corners[2].y), cornerWidth)
            drawLine(cornerColor, corners[2], Offset(corners[2].x, corners[2].y - cornerLen), cornerWidth)
            drawLine(cornerColor, corners[3], Offset(corners[3].x - cornerLen, corners[3].y), cornerWidth)
            drawLine(cornerColor, corners[3], Offset(corners[3].x, corners[3].y - cornerLen), cornerWidth)

            if (isProcessing) {
                val scanY = top + boxHeight * scanProgress
                drawLine(
                    color = AiAccent,
                    start = Offset(left + 8f, scanY),
                    end = Offset(left + boxWidth - 8f, scanY),
                    strokeWidth = 3f,
                    blendMode = BlendMode.Screen
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot(size = 8.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI 正在分析...",
                            color = AiAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        "将食物对准框内",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(size: androidx.compose.ui.unit.Dp = 12.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AiAccent.copy(alpha = alpha))
    )
}
