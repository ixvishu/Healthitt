@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthitt.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.edit
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(userEmail: String, onLogout: () -> Unit, onNavigateToWorkouts: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var todaySteps by remember { mutableIntStateOf(0) }
    var weeklyHistory by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var calorieGoal by remember { mutableIntStateOf(2000) }
    
    // UI State
    var showAccountDetails by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }
    var showAnalysis by remember { mutableStateOf(false) }

    // Auto-Update State
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    val currentAppVersion = 1

    // AI Integration
    var aiHealthTip by remember { mutableStateOf("Consulting the health oracle...") }
    var isAiLoading by remember { mutableStateOf(true) }

    // Food AI Integration
    var foodAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isFoodAnalyzing by remember { mutableStateOf(false) }
    var showFoodResult by remember { mutableStateOf(false) }
    
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
    val sharedPrefs = remember { context.getSharedPreferences("healthitt_local_data", Context.MODE_PRIVATE) }

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun downloadAndUpdate(url: String) {
        isUpdating = true
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "healthitt_update.apk")
                    val fos = FileOutputStream(file)
                    fos.write(response.body?.bytes())
                    fos.close()
                    withContext(Dispatchers.Main) {
                        isUpdating = false
                        showUpdateDialog = false
                        installApk(file)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isUpdating = false
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun fetchAiTip() {
        isAiLoading = true
        scope.launch {
            try {
                val seed = Random().nextInt(100000)
                val themes = listOf("recovery", "nutrition", "mental clarity", "strength", "habits")
                val response = generativeModel.generateContent("Fitness tip theme: ${themes.random()}. Seed: $seed. Give a unique health tip. Max 10 words.")
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
        scope.launch {
            try {
                // Resize bitmap to avoid payload limits (max 1024px)
                val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val width = if (ratio > 1) 1024 else (1024 * ratio).toInt()
                    val height = if (ratio > 1) (1024 / ratio).toInt() else 1024
                    Bitmap.createScaledBitmap(bitmap, width, height, true)
                } else {
                    bitmap
                }

                val inputContent = content {
                    image(scaledBitmap)
                    text("Analyze this food image. Provide: 1. Food Name 2. Estimated Calories 3. Protein (g) 4. Carbs (g) 5. Fats (g). Use a clear, aesthetic list.")
                }
                val response = generativeModel.generateContent(inputContent)
                foodAnalysisResult = response.text ?: "AI could not identify this food. Please try a clearer photo."
            } catch (e: Exception) {
                foodAnalysisResult = "Analysis Failed: ${e.localizedMessage ?: "Network or API error"}. Please check your connection."
            } finally {
                isFoodAnalyzing = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            scope.launch(Dispatchers.IO) {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_temp.jpg")
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    withContext(Dispatchers.Main) {
                        bitmap?.let { analyzeFoodImage(it) } ?: run {
                            foodAnalysisResult = "Failed to load image from camera."
                            showFoodResult = true
                        }
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                withContext(Dispatchers.Main) {
                    bitmap?.let { analyzeFoodImage(it) }
                }
            }
        }
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDate = sdf.format(Date())

    LaunchedEffect(Unit) {
        fetchAiTip()
        todaySteps = sharedPrefs.getInt("local_steps_${userEmailKey}_$todayDate", 0)

        database.child("app_config").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latestVersion = snapshot.child("version_code").getValue(Int::class.java) ?: 1
                val downloadUrl = snapshot.child("apk_url").getValue(String::class.java) ?: ""
                if (latestVersion > currentAppVersion && downloadUrl.isNotEmpty()) {
                    updateUrl = downloadUrl
                    showUpdateDialog = true
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(User::class.java)
                calorieGoal = snapshot.child("calorieGoal").getValue(Int::class.java) ?: 2000
                val dbSteps = snapshot.child("daily_history").child(todayDate).getValue(Int::class.java) ?: 0
                if (dbSteps > todaySteps) {
                    todaySteps = dbSteps
                    sharedPrefs.edit { putInt("local_steps_${userEmailKey}_$todayDate", todaySteps) }
                }
                
                val historyList = mutableListOf<Pair<String, Int>>()
                val historySnapshot = snapshot.child("daily_history")
                for (i in 0..14) { 
                    val date = sdf.format(Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000))
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
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF121212),
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Healthitt",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFBB86FC),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Account Details", color = Color.White) },
                        selected = false,
                        onClick = { 
                            showAccountDetails = true 
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AccountCircle, null, tint = Color(0xFFBB86FC)) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    
                    NavigationDrawerItem(
                        label = { Text("About App", color = Color.White) },
                        selected = false,
                        onClick = { 
                            showAboutApp = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Info, null, tint = Color(0xFFBB86FC)) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        label = { Text("Logout", color = Color.Red) },
                        selected = false,
                        onClick = onLogout,
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color(0xFF1A0033), Color(0xFF2D0052))))) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Hello,", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f))
                        Text(text = user?.name ?: "User", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
                
                // Step Counter with Glow Effect
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(280.dp).shadow(20.dp, CircleShape, spotColor = Color(0xFFBB86FC))) {
                        drawCircle(color = Color.White.copy(0.03f), style = Stroke(20.dp.toPx()))
                    }
                    StepCounterView(steps = todaySteps, calorieGoal = calorieGoal)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // AI Tip Card
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.15f))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFBB86FC), strokeWidth = 2.dp)
                        } else {
                            Text(text = aiHealthTip, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NEW: Gym Workout Plans Section (PROMINENT)
                Text(
                    text = "Training Plans",
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    onClick = onNavigateToWorkouts,
                    modifier = Modifier.fillMaxWidth().height(140.dp).shadow(15.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFBB86FC), Color(0xFF6200EE))))) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Explore Workouts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                                Text("160+ exercises with professional guides", color = Color.White.copy(0.8f), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(color = Color.White.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                                    Text("Go to Library", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(Icons.Default.FitnessCenter, null, tint = Color.White.copy(0.25f), modifier = Modifier.size(80.dp).weight(0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI Tools Section
                Text(
                    text = "AI Health Tools",
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AIToolCard(
                        title = "Capture Meal",
                        icon = Icons.Default.CameraAlt,
                        color = Color(0xFF03DAC5),
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "food_temp.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(uri)
                        }
                    )
                    AIToolCard(
                        title = "Upload Photo",
                        icon = Icons.Default.CloudUpload,
                        color = Color(0xFFCF6679),
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                
                // View Stats Button
                Button(
                    onClick = { showAnalysis = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp).shadow(10.dp, RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.BarChart, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Your Progress Stats", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Account Details Dialog
    if (showAccountDetails) {
        AlertDialog(
            onDismissRequest = { showAccountDetails = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Account Details", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountItem(label = "Name", value = user?.name ?: "N/A")
                    AccountItem(label = "Email", value = user?.email ?: "N/A")
                    AccountItem(label = "Phone", value = user?.phone ?: "N/A")
                    AccountItem(label = "Weight", value = "${user?.weight ?: "N/A"} kg")
                    AccountItem(label = "Height", value = "${user?.height ?: "N/A"} ft")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDetails = false }) {
                    Text("Close", color = Color(0xFFBB86FC))
                }
            }
        )
    }

    // About App Dialog
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
                    Text(
                        text = "Current Features:",
                        color = Color(0xFFBB86FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "• Real-time Step Tracking\n• AI-Powered Meal Analysis\n• 15-Day Progress Insights\n• Personalized Health Tips",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFBB86FC).copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: Healthitt is currently in its active building stage. We are continuously refining the experience. Stay tuned as more innovative features will be updated slowly to help you achieve your peak fitness.",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFBB86FC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutApp = false }) {
                    Text("Back", color = Color(0xFFBB86FC))
                }
            }
        )
    }

    // Analysis Modal
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
                    HistoryRow(date = if(date == todayDate) "Today" else date, steps = steps)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Auto-Update Dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { }, 
            containerColor = Color(0xFF1A1A1A),
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Update, null, tint = Color(0xFFBB86FC))
                    Spacer(Modifier.width(12.dp))
                    Text("Update Available", color = Color.White)
                }
            },
            text = { Text("A new version of Healthitt is ready. Improve your health with the latest features!", color = Color.White.copy(0.7f)) },
            confirmButton = {
                if (isUpdating) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC), modifier = Modifier.size(24.dp))
                } else {
                    Button(onClick = { downloadAndUpdate(updateUrl) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))) {
                        Text("Update Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (showFoodResult) {
        AlertDialog(
            onDismissRequest = { showFoodResult = false }, containerColor = Color(0xFF0A0A0A),
            confirmButton = { TextButton(onClick = { showFoodResult = false }) { Text("Done", color = Color(0xFFBB86FC)) } },
            title = { Text("Nutritional AI", color = Color.White) },
            text = { 
                if (isFoodAnalyzing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFFBB86FC))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Decoding nutritional data...", color = Color.White.copy(0.6f))
                    }
                } else {
                    Text(foodAnalysisResult ?: "", color = Color.White.copy(0.8f), lineHeight = 24.sp) 
                }
            }
        )
    }
}

@Composable
fun AIToolCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
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

@Composable
fun StepCounterView(steps: Int, calorieGoal: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White.copy(0.03f), style = Stroke(25.dp.toPx()))
            drawArc(brush = Brush.sweepGradient(listOf(Color(0xFF4A148C), Color(0xFFBB86FC), Color(0xFF4A148C))), startAngle = -90f, sweepAngle = (steps/10000f)*360f, useCenter = false, style = Stroke(25.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = steps.toString(), style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Black)
            Text(text = "STEPS", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
        }
    }
}
