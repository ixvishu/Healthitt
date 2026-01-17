@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthitt.ui.dashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.healthitt.data.User
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    userEmail: String, 
    onLogout: () -> Unit, 
    onNavigateToWorkouts: () -> Unit,
    onNavigateToBMI: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var todaySteps by remember { mutableIntStateOf(0) }
    var weeklyHistory by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    
    // UI State
    var showAccountDetails by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }
    var showAnalysis by remember { mutableStateOf(false) }

    // Editable States
    var editWeight by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editCalorieGoal by remember { mutableStateOf("") }

    // AI States
    var aiHealthTip by remember { mutableStateOf("Consulting the health oracle...") }
    var isAiLoading by remember { mutableStateOf(true) }
    var foodAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isFoodAnalyzing by remember { mutableStateOf(false) }
    var showFoodResult by remember { mutableStateOf(false) }

    // Entrance Animation State
    var startAnims by remember { mutableStateOf(false) }
    val entryAlpha by animateFloatAsState(if (startAnims) 1f else 0f, tween(1000), label = "alpha")
    val entryOffsetY by animateDpAsState(if (startAnims) 0.dp else 40.dp, tween(800, easing = FastOutSlowInEasing), label = "offset")

    val userEmailKey = remember { userEmail.replace(".", "_") }
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBjr5NA9RL5ASEpbS3rmmSdvvZ_NLKHoA8",
            generationConfig = generationConfig { temperature = 1.0f }
        )
    }

    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference
    val userRef = database.child("users").child(userEmailKey)

    // Force enable sound and haptics for premium feel
    fun playClickSound() {
        view.isSoundEffectsEnabled = true
        view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun calculateAge(dob: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dob) ?: return 0
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) { 0 }
    }

    fun fetchAiTip() {
        isAiLoading = true
        scope.launch {
            try {
                val seed = Random().nextInt(100000)
                val response = generativeModel.generateContent("Give a unique, expert fitness tip. Max 10 words. Seed: $seed")
                aiHealthTip = response.text?.trim()?.removeSurrounding("\"") ?: "Movement is life."
            } catch (e: Exception) {
                aiHealthTip = "Consistency beats intensity."
            } finally {
                isAiLoading = false
            }
        }
    }

    fun analyzeFoodImage(bitmap: Bitmap) {
        isFoodAnalyzing = true
        showFoodResult = true
        foodAnalysisResult = "Analyzing your meal..."
        scope.launch(Dispatchers.IO) {
            try {
                // More robust bitmap scaling for AI processing
                val maxDim = 720
                val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                    val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                    Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                } else {
                    bitmap
                }

                val inputContent = content {
                    image(scaledBitmap)
                    text("Analyze this food and provide: Name, Calories, Protein, Carbs, Fats. Professional style.")
                }
                val response = generativeModel.generateContent(inputContent)
                withContext(Dispatchers.Main) {
                    foodAnalysisResult = response.text ?: "Could not identify food. Please use a clearer image."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    foodAnalysisResult = "AI connection error. Please try again in a moment."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isFoodAnalyzing = false
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_temp.jpg")
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.let { analyzeFoodImage(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    withContext(Dispatchers.Main) {
                        bitmap?.let { analyzeFoodImage(it) }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        startAnims = true
        fetchAiTip()
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(User::class.java)
                user?.let {
                    editWeight = it.weight
                    editHeight = it.height
                    editCalorieGoal = it.calorieGoal.toString()
                }
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                todaySteps = snapshot.child("daily_history").child(todayDate).getValue(Int::class.java) ?: 0
                
                val historyList = mutableListOf<Pair<String, Int>>()
                val historySnapshot = snapshot.child("daily_history")
                for (i in 0..14) { 
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000))
                    val steps = historySnapshot.child(date).getValue(Int::class.java) ?: 0
                    historyList.add(date to steps)
                }
                weeklyHistory = historyList.reversed()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF121212), modifier = Modifier.width(300.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Healthitt", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFBB86FC), fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    DrawerItem("Account Details", Icons.Default.AccountCircle) { 
                        playClickSound(); showAccountDetails = true; scope.launch { drawerState.close() } 
                    }
                    DrawerItem("BMI Calculator", Icons.Default.Calculate) { 
                        playClickSound(); onNavigateToBMI(); scope.launch { drawerState.close() } 
                    }
                    DrawerItem("About App", Icons.Default.Info) { 
                        playClickSound(); showAboutApp = true; scope.launch { drawerState.close() } 
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DrawerItem("Logout", Icons.AutoMirrored.Filled.Logout, Color.Red) { 
                        playClickSound(); onLogout() 
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color(0xFF1A0033), Color(0xFF2D0052))))) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).graphicsLayer {
                    alpha = entryAlpha
                    translationY = with(density) { entryOffsetY.toPx() }
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playClickSound(); scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Welcome,", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f))
                        Text(user?.name ?: "User", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
                StepCounterView(steps = todaySteps)
                
                Spacer(modifier = Modifier.height(40.dp))
                PremiumCard {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFBB86FC), strokeWidth = 2.dp)
                        } else {
                            Text(aiHealthTip, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Training Plans", modifier = Modifier.fillMaxWidth().padding(start = 4.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveActionCard(
                    title = "Explore Workouts",
                    subtitle = "160+ Expert Exercises",
                    icon = Icons.Default.FitnessCenter,
                    gradient = listOf(Color(0xFFBB86FC), Color(0xFF6200EE)),
                    onClick = { playClickSound(); onNavigateToWorkouts() }
                )

                Spacer(modifier = Modifier.height(32.dp))
                Text("AI Health Lab", modifier = Modifier.fillMaxWidth().padding(start = 4.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedToolCard("Capture", Icons.Default.CameraAlt, Color(0xFF03DAC5), Modifier.weight(1f)) {
                        playClickSound()
                        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_temp.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(uri)
                    }
                    AnimatedToolCard("Upload", Icons.Default.CloudUpload, Color(0xFFCF6679), Modifier.weight(1f)) {
                        playClickSound(); galleryLauncher.launch("image/*")
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                PremiumButton("View Progress Stats", Icons.Default.BarChart) {
                    playClickSound(); showAnalysis = true
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    if (showAccountDetails) {
        AlertDialog(
            onDismissRequest = { showAccountDetails = false },
            containerColor = Color(0xFF1A1A1A),
            confirmButton = {
                TextButton(onClick = {
                    playClickSound()
                    userRef.child("weight").setValue(editWeight)
                    userRef.child("height").setValue(editHeight)
                    val newGoal = editCalorieGoal.toIntOrNull() ?: 2000
                    userRef.child("calorieGoal").setValue(newGoal)
                    showAccountDetails = false
                    Toast.makeText(context, "Details Updated!", Toast.LENGTH_SHORT).show()
                }) { Text("Update", color = Color(0xFFBB86FC)) }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDetails = false }) { Text("Cancel", color = Color.White.copy(0.6f)) }
            },
            title = { Text("Profile Information", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AccountItem("Name", user?.name ?: "N/A")
                    AccountItem("Email", user?.email ?: "N/A")
                    AccountItem("Current Age", "${calculateAge(user?.dob ?: "")} Years")
                    
                    OutlinedTextField(
                        value = editWeight, onValueChange = { editWeight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                    )
                    OutlinedTextField(
                        value = editHeight, onValueChange = { editHeight = it },
                        label = { Text("Height (ft.in)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                    )
                    OutlinedTextField(
                        value = editCalorieGoal, onValueChange = { editCalorieGoal = it },
                        label = { Text("Daily Calorie Goal") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                    )
                }
            }
        )
    }

    if (showAboutApp) {
        AlertDialog(
            onDismissRequest = { showAboutApp = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("About Healthitt", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Healthitt is your premium personal fitness companion designed to track your daily progress with elegance and intelligence.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Real-time Step Tracking\n• AI-Powered Meal Analysis\n• 160+ Workout Plans\n• Dynamic BMI Calculator", color = Color.White.copy(alpha = 0.7f))
                }
            },
            confirmButton = { TextButton(onClick = { showAboutApp = false }) { Text("Close", color = Color(0xFFBB86FC)) } }
        )
    }

    if (showFoodResult) {
        AlertDialog(onDismissRequest = { showFoodResult = false }, containerColor = Color(0xFF0A0A0A),
            confirmButton = { TextButton(onClick = { showFoodResult = false }) { Text("Done", color = Color(0xFFBB86FC)) } },
            title = { Text("Nutritional AI", color = Color.White) },
            text = { 
                if (isFoodAnalyzing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = Color(0xFFBB86FC))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reading meal data...", color = Color.White.copy(0.6f))
                    }
                } else {
                    Text(foodAnalysisResult ?: "", color = Color.White.copy(0.8f)) 
                }
            }
        )
    }

    if (showAnalysis) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysis = false },
            containerColor = Color(0xFF0F001A),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Steps Overview", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(24.dp))
                ModernBarGraph(history = weeklyHistory)
                Spacer(modifier = Modifier.height(32.dp))
                weeklyHistory.reversed().forEach { (date, steps) ->
                    HistoryRow(date = date, steps = steps)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StepCounterView(steps: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "scale")
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.size(250.dp).scale(glowScale)) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFBB86FC).copy(0.1f), Color.Transparent)), radius = size.minDimension / 1.1f)
        }
        Canvas(modifier = Modifier.size(230.dp)) {
            drawCircle(Color.White.copy(0.05f), style = Stroke(15.dp.toPx()))
            drawArc(Brush.sweepGradient(listOf(Color(0xFF4A148C), Color(0xFFBB86FC), Color(0xFF4A148C))), -90f, (steps/10000f)*360f, false, style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(steps.toString(), style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Black, fontSize = 64.sp)
            Text("STEPS TODAY", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun InteractiveActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, gradient: List<Color>, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().height(140.dp).scale(scale).shadow(20.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(gradient))) {
            Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = Color.White.copy(0.8f), fontSize = 14.sp)
                }
                Icon(icon, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(90.dp))
            }
        }
    }
}

@Composable
fun AnimatedToolCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")

    Box(
        modifier = modifier
            .height(110.dp)
            .scale(scale)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = color),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PremiumButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().height(65.dp).scale(scale).shadow(15.dp, RoundedCornerShape(20.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(icon, null, tint = Color.Black)
        Spacer(Modifier.width(12.dp))
        Text(text, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun PremiumCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(15.dp, RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFBB86FC).copy(0.1f), RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.White, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, color = color, fontWeight = FontWeight.Medium) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null, tint = if(color == Color.White) Color(0xFFBB86FC) else color) },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@Composable
fun AccountItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ModernBarGraph(history: List<Pair<String, Int>>) {
    val maxSteps = history.maxOfOrNull { it.second }?.coerceAtLeast(10000) ?: 10000
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }

    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        history.forEach { (_, steps) ->
            val barHeight = (steps.toFloat() / maxSteps).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(barHeight * animProgress.value)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (steps >= 10000) listOf(Color(0xFF00C853), Color(0xFFB2FF59)) 
                                    else listOf(Color(0xFFBB86FC), Color(0xFF4A148C))
                        )
                    )
            )
        }
    }
}

@Composable
fun HistoryRow(date: String, steps: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = date, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text(text = "$steps steps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            CircularProgressIndicator(
                progress = { (steps / 10000f).coerceIn(0f, 1f) },
                modifier = Modifier.size(32.dp),
                color = if (steps >= 10000) Color(0xFF00C853) else Color(0xFFBB86FC),
                strokeWidth = 3.dp,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}
