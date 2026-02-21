package com.example.healthitt.ui.homescreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit
) {
    var stage by remember { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val orbOffset1 by infiniteTransition.animateValue(
        initialValue = Offset.Zero,
        targetValue = Offset(100f, 150f),
        typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rotation"
    )

    val scanLinePos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "scanLine"
    )

    LaunchedEffect(Unit) {
        delay(200)
        stage = 1
        delay(700)
        stage = 2
        delay(500)
        stage = 3
        delay(500)
        stage = 4
    }

    Box(modifier = Modifier.fillMaxSize().background(NightDark)) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
            val gridSpacing = 60.dp.toPx()
            for (x in 0..(size.width / gridSpacing).toInt()) {
                drawLine(color = NeonCyan, start = Offset(x * gridSpacing, 0f), end = Offset(x * gridSpacing, size.height), strokeWidth = 1f)
            }
            for (y in 0..(size.height / gridSpacing).toInt()) {
                drawLine(color = NeonCyan, start = Offset(0f, y * gridSpacing), end = Offset(size.width, y * gridSpacing), strokeWidth = 1f)
            }
            drawLine(
                brush = Brush.verticalGradient(listOf(Color.Transparent, NeonGreen, Color.Transparent), startY = scanLinePos * size.height - 100f, endY = scanLinePos * size.height + 100f),
                start = Offset(0f, scanLinePos * size.height), end = Offset(size.width, scanLinePos * size.height), strokeWidth = 4f
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(120.dp)) {
            drawCircle(brush = Brush.radialGradient(listOf(NeonGreen.copy(0.2f), Color.Transparent)), radius = 1500f, center = Offset(size.width * 0.8f + orbOffset1.x, size.height * 0.1f + orbOffset1.y))
            drawCircle(brush = Brush.radialGradient(listOf(NeonCyan.copy(0.15f), Color.Transparent)), radius = 1300f, center = Offset(size.width * 0.2f - orbOffset1.x, size.height * 0.9f - orbOffset1.y))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val availableWidth = maxWidth
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = (availableWidth * 0.1f).coerceAtLeast(24.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(availableHeight * 0.15f))

                // Logo
                AnimatedVisibility(visible = stage >= 1, enter = fadeIn(tween(1000)) + scaleIn(spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow))) {
                    Box(contentAlignment = Alignment.Center) {
                        val logoSize = (availableWidth * 0.4f).coerceIn(120.dp, 200.dp)
                        Canvas(modifier = Modifier.size(logoSize).rotate(rotation)) {
                            drawPath(
                                path = Path().apply {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val radius = size.width / 2
                                    moveTo(center.x, center.y - radius)
                                    lineTo(center.x + radius, center.y)
                                    lineTo(center.x, center.y + radius)
                                    lineTo(center.x - radius, center.y)
                                    close()
                                },
                                color = NeonGreen.copy(alpha = 0.3f),
                                style = Stroke(width = 2f)
                            )
                        }
                        val glowScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "glow")
                        Box(modifier = Modifier.size(logoSize * 0.5f).graphicsLayer { scaleX = glowScale; scaleY = glowScale }.blur(40.dp).background(NeonGreen.copy(0.4f), CircleShape))
                        Surface(modifier = Modifier.size(logoSize * 0.5f), shape = RoundedCornerShape(logoSize * 0.15f), color = NightDark.copy(alpha = 0.8f), border = BorderStroke(2.dp, Brush.sweepGradient(listOf(NeonGreen, NeonCyan, NeonGreen)))) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("H", color = PureWhite, fontSize = (logoSize.value * 0.25f).sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(visible = stage >= 2, enter = fadeIn() + slideInVertically(initialOffsetY = { -20 })) {
                        Text(
                            text = "HEALTHITT",
                            fontSize = (availableWidth.value * 0.12f).coerceIn(40f, 62f).sp,
                            fontWeight = FontWeight.Black,
                            style = LocalTextStyle.current.copy(brush = Brush.horizontalGradient(ActiveGradient)),
                            letterSpacing = (-2).sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    AnimatedVisibility(visible = stage >= 3, enter = expandHorizontally(expandFrom = Alignment.CenterHorizontally) + fadeIn()) {
                        Surface(color = NeonGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Text(text = " Your Personal Fitness Assistant ", fontSize = 12.sp, color = NeonGreen, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), letterSpacing = 2.sp)
                        }
                    }
                }

                Spacer(Modifier.height(availableHeight * 0.1f))

                AnimatedVisibility(visible = stage >= 4, enter = fadeIn(tween(1200)) + slideInVertically(initialOffsetY = { 50 })) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unlock your metabolic potential with a clean modern dashboard and a high-performance community.",
                            color = PureWhite.copy(0.5f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier.fillMaxWidth().height(64.dp).drawWithContent { drawContent(); drawRoundRect(brush = Brush.sweepGradient(listOf(NeonGreen, NeonCyan, NeonGreen)), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()), style = Stroke(width = 2f), alpha = 0.5f) },
                            colors = ButtonDefaults.buttonColors(containerColor = NightDark),
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Start Your Journey", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
