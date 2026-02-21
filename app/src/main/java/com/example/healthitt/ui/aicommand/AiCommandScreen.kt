package com.example.healthitt.ui.aicommand

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.data.User
import com.example.healthitt.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AiCommandScreen(userEmail: String) {
    val scope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<User?>(null) }
    var bioAge by remember { mutableStateOf<Int?>(null) }
    var forecastText by remember { mutableStateOf("") }
    var tipText by remember { mutableStateOf("") }
    var syncScore by remember { mutableFloatStateOf(0f) }

    val userEmailKey = remember { userEmail.replace(".", "_") }
    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference
    val userRef = database.child("users").child(userEmailKey)

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyBjr5NA9RL5ASEpbS3rmmSdvvZ_NLKHoA8",
            generationConfig = generationConfig { temperature = 0.8f }
        )
    }

    LaunchedEffect(Unit) {
        userRef.get().addOnSuccessListener { snapshot ->
            user = snapshot.getValue(User::class.java)
        }
    }

    fun runLongevitySimulation() {
        if (user == null) return
        isAnalyzing = true
        scope.launch {
            try {
                delay(2500) 
                
                val prompt = """
                    Act as a warm, encouraging health coach for seniors. 
                    Subject: ${user?.name}, Gender: ${user?.gender}, Weight: ${user?.weight}kg, Height: ${user?.height}ft.
                    Provide:
                    1. Estimated Body Age (simple number).
                    2. A short, very positive health outlook for the next few years.
                    3. One very simple daily tip (like drinking water or walking).
                    Format: BodyAge: [Number] | Outlook: [Text] | Tip: [Text]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val parts = response.text?.split("|") ?: emptyList()
                
                if (parts.size >= 3) {
                    bioAge = parts[0].filter { it.isDigit() }.toIntOrNull()
                    forecastText = parts[1].replace("Outlook:", "").trim()
                    tipText = parts[2].replace("Tip:", "").trim()
                    syncScore = (0.75f + (Math.random() * 0.2f)).toFloat() 
                }
            } catch (e: Exception) {
                forecastText = "I'm having a little trouble connecting. Please try again in a moment."
            } finally {
                isAnalyzing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Soft background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = (-150).dp)
                .align(Alignment.TopCenter)
                .blur(100.dp)
                .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            
            Text(
                "Health Companion",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Your simple guide to a better tomorrow",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(48.dp))

            // Central Soft Hero Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing background for the score
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
                    )

                    if (isAnalyzing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 6.dp
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("Taking a look...", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    } else if (bioAge != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR BODY FEELS", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text("$bioAge", fontSize = 80.sp, fontWeight = FontWeight.Black, color = EmeraldPrimary, modifier = Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
                            Text("YEARS YOUNG", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = EmeraldPrimary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Ready for your check-up?", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { runLongevitySimulation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                enabled = !isAnalyzing
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HealthAndSafety, null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (bioAge == null) "Start My Health Check" else "Update My Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if ((forecastText.isNotEmpty() || tipText.isNotEmpty()) && !isAnalyzing) {
                Spacer(Modifier.height(32.dp))

                // Outlook Card
                SimpleInfoCard(
                    title = "Your Future Health",
                    content = forecastText,
                    icon = Icons.Rounded.WbSunny,
                    accentColor = SkyAccent
                )

                Spacer(Modifier.height(16.dp))

                // Tip Card
                SimpleInfoCard(
                    title = "Simple Daily Tip",
                    content = tipText.ifEmpty { "Try walking for 10 minutes after lunch today to feel more active." },
                    icon = Icons.Rounded.Lightbulb,
                    accentColor = AmberAccent
                )

                Spacer(Modifier.height(32.dp))

                // Score Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Health Match", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("${(syncScore * 100).toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = syncScore,
                                color = EmeraldPrimary,
                                strokeWidth = 8.dp,
                                modifier = Modifier.size(60.dp),
                                trackColor = EmeraldPrimary.copy(alpha = 0.1f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Icon(Icons.Rounded.Check, null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun SimpleInfoCard(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = accentColor)
                Spacer(Modifier.height(4.dp))
                Text(content, fontSize = 16.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            }
        }
    }
}
