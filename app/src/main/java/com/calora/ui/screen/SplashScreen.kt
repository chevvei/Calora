package com.calora.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calora.R
import kotlinx.coroutines.delay

@Composable
fun SplashRoute(onNavigateToHome: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val sloganAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(600))
        delay(200)
        textAlpha.animateTo(1f, animationSpec = tween(500))
        delay(300)
        sloganAlpha.animateTo(1f, animationSpec = tween(500))
        delay(1200)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.35f)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = logoAlpha.value }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f - 80.dp.toPx())
            val radius = 50.dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = radius * 0.75f,
                center = center
            )

            val leafWidth = radius * 0.35f
            val leafHeight = radius * 0.7f

            val leaf1 = Path().apply {
                moveTo(center.x, center.y - radius * 0.35f)
                cubicTo(
                    center.x + leafWidth, center.y - leafHeight * 0.5f,
                    center.x + leafWidth * 0.8f, center.y + leafHeight * 0.3f,
                    center.x, center.y + radius * 0.15f
                )
                close()
            }
            drawPath(leaf1, color = Color(0xFF69F0AE), style = Fill)

            val leaf2 = Path().apply {
                moveTo(center.x, center.y - radius * 0.35f)
                cubicTo(
                    center.x - leafWidth, center.y - leafHeight * 0.5f,
                    center.x - leafWidth * 0.8f, center.y + leafHeight * 0.3f,
                    center.x, center.y + radius * 0.15f
                )
                close()
            }
            drawPath(leaf2, color = Color(0xFFB9F6CA), style = Fill)

            drawCircle(
                color = Color(0xFFFFD180),
                radius = radius * 0.12f,
                center = Offset(center.x, center.y - radius * 0.15f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 200.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                "Calora",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Snap. Know. Nourish.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB9F6CA),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { alpha = sloganAlpha.value }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "识别食物 · 估算营养 · 健康饮食",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = sloganAlpha.value }
            )
        }
    }
}
