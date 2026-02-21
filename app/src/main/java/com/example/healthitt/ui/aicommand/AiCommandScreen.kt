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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

    fun runHealthSimulation() {
        if (user == null) return
        isAnalyzing = true
        scope.launch {
            try {
                delay(2000) 
                val prompt = """
                    Act as a elite fitness coach. User: ${user?.name}, Stats: ${user?.weight}kg, ${user?.height}ft.
                    Provide:
                    1. Estimated 'Athletic Age' (how old their fitness level feels).
                    2. A short, high-energy fitness outlook for the next 10 years.
                    3. One professional 'Pro-Athlete Tip'.
                    Format: BodyAge: [Number] | Outlook: [Text] | Tip: [Text]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val parts = response.text?.split("|") ?: emptyList()
                
                if (parts.size >= 3) {
                    bioAge = parts[0].filter { it.isDigit() }.toIntOrNull()
                    forecastText = parts[1].replace("Outlook:", "").trim()
                    tipText = parts[2].replace("Tip:", "").trim()
                    syncScore = (0.8f + (Math.random() * 0.15f)).toFloat() 
                }
            } catch (e: Exception) {
                forecastText = "Network glitch. Ready for a retry?"
            } finally {
                isAnalyzing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        // --- BATTERY EFFICIENT BACKGROUND ---
        // Zero animations in background when static to save battery
        if (isAnalyzing) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val bgAlpha by infiniteTransition.animateFloat(
                initialValue = 0.05f, targetValue = 0.15f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ElectricLime.copy(alpha = bgAlpha), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(500f, 500f),
                            radius = 1000f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            
            Text(
                "ATHLETIC AI",
                style = MaterialTheme.typography.displaySmall,
                color = ElectricLime,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                "PERFORMANCE SIMULATION",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite.copy(alpha = 0.4f),
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(50.dp))

            // --- ANIMATED CIRCULAR PROGRESS ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                val animatedScore by animateFloatAsState(
                    targetValue = if (isAnalyzing) 0.2f else if (bioAge != null) syncScore else 0f,
                    animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "score"
                )

                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = PureWhite.copy(alpha = 0.05f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                
                CircularProgressIndicator(
                    progress = animatedScore,
                    modifier = Modifier.fillMaxSize(),
                    color = if (isAnalyzing) NeonCyan else ElectricLime,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isAnalyzing) {
                        val rotation = rememberInfiniteTransition(label = "rot")
                        val angle by rotation.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle"
                        )
                        Icon(
                            Icons.Rounded.Sync, null, 
                            tint = ElectricLime, 
                            modifier = Modifier.size(60.dp).rotate(angle)
                        )
                    } else if (bioAge != null) {
                        Text("FITNESS AGE", color = PureWhite.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$bioAge", color = PureWhite, fontSize = 80.sp, fontWeight = FontWeight.Black, letterSpacing = (-4).sp)
                        Text("LEVEL: ELITE", color = ElectricLime, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    } else {
                        Icon(Icons.Rounded.Bolt, null, tint = ElectricLime, modifier = Modifier.size(100.dp))
                    }
                }
            }

            Spacer(Modifier.height(50.dp))

            // --- ACTION BUTTON ---
            Button(
                onClick = { runHealthSimulation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isAnalyzing) DarkSurface else ElectricLime),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                enabled = !isAnalyzing
            ) {
                Text(
                    if (bioAge == null) "RUN ANALYSIS" else "SYNC DATA",
                    color = MidnightBlack, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // --- REVEAL CONTENT ---
            AnimatedVisibility(
                visible = bioAge != null && !isAnalyzing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(32.dp))
                    
                    PerformanceCard(
                        title = "10-YEAR PROJECTION",
                        content = forecastText,
                        icon = Icons.Rounded.Timeline,
                        accentColor = NeonCyan
                    )

                    Spacer(Modifier.height(16.dp))

                    PerformanceCard(
                        title = "ELITE TRAINING TIP",
                        content = tipText,
                        icon = Icons.Rounded.Verified,
                        accentColor = BlazeOrange
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // PERFORMANCE SYNC
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BIOMETRIC ACCURACY", color = PureWhite.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${(syncScore * 100).toInt()}%", color = PureWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            }
                            LinearProgressIndicator(
                                progress = syncScore,
                                color = ElectricLime,
                                trackColor = PureWhite.copy(alpha = 0.05f),
                                modifier = Modifier.width(80.dp).height(8.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun PerformanceCard(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(content, color = PureWhite, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
