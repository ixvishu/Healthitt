package com.example.healthitt.ui.homescreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthitt.ui.theme.*

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1500, delayMillis = 600),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(modifier = Modifier.fillMaxSize().background(NightDark)) {
        // MODERN NEON FLOW BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize().blur(90.dp)) {
            val xPos = size.width * (0.5f + 0.2f * kotlin.math.sin(floatAnim * 2 * Math.PI.toFloat()))
            val yPos = size.height * (0.4f + 0.1f * kotlin.math.cos(floatAnim * 2 * Math.PI.toFloat()))
            
            drawCircle(
                brush = Brush.radialGradient(listOf(NeonGreen.copy(0.12f), Color.Transparent)),
                radius = 1200f,
                center = Offset(xPos, yPos)
            )
            
            drawCircle(
                brush = Brush.radialGradient(listOf(NeonCyan.copy(0.08f), Color.Transparent)),
                radius = 1000f,
                center = Offset(size.width - xPos, size.height - yPos)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(100.dp))

            // BRAND IDENTITY WITH NEON GLOW
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(logoScale).alpha(contentAlpha)) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer Glow
                    Surface(
                        modifier = Modifier.size(120.dp).blur(20.dp),
                        shape = CircleShape,
                        color = NeonGreen.copy(0.2f)
                    ) {}
                    
                    // Core Icon Placeholder
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = GlassCard,
                        border = BorderStroke(2.dp, Brush.linearGradient(ActiveGradient))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("H", color = PureWhite, fontSize = 40.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = "Healthitt",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(ActiveGradient)
                    ),
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "Your Health, Evolved",
                    fontSize = 14.sp,
                    color = PureWhite.copy(0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(contentAlpha)) {
                Text(
                    text = "Track steps, food, and sleep easily.\nAll your health data in one place.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhite.copy(0.4f),
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 60.dp).alpha(contentAlpha)) {
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(ActiveGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Get Started", color = NightDark, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = NeonGreen) {}
                    Spacer(Modifier.width(12.dp))
                    Text("Secure & Private", color = PureWhite.copy(0.3f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    HealthittTheme {
        WelcomeScreen(onNavigateToLogin = {})
    }
}
