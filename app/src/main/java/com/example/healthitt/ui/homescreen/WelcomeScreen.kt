package com.example.healthitt.ui.homescreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthitt.R
import com.example.healthitt.ui.theme.HealthittTheme

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Black, Color(0xFF4A148C), Color.Black),
                    start = androidx.compose.ui.geometry.Offset(offset, offset),
                    end = androidx.compose.ui.geometry.Offset(offset + 1000f, offset + 1000f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Updated to use your healthitt logo
            Image(
                painter = painterResource(id = R.drawable.logo_healthitt),
                contentDescription = "Healthitt Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Healthitt",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Your journey to fitness begins here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .scale(if (startAnimation) 1f else 0.8f)
            ) {
                Text("Get Started")
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
