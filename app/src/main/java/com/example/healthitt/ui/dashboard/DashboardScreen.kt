@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthitt.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Segment
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.healthitt.data.Medication
import com.example.healthitt.data.User
import com.example.healthitt.health.BleWatchManager
import com.example.healthitt.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

const val CURRENT_APP_VERSION = "4.2.7"

@Composable
fun DashboardScreen(
    userEmail: String, 
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onLogout: () -> Unit, 
    onNavigateToWorkouts: () -> Unit, 
    onNavigateToBMI: () -> Unit,
    onNavigateToTodo: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var todaySteps by remember { mutableIntStateOf(0) }
    
    var waterCount by remember { mutableIntStateOf(0) }
    var sleepHours by remember { mutableFloatStateOf(0f) }
    var medsStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val bleManager = remember { BleWatchManager(context) }
    val watchConnectionState by bleManager.connectionState.collectAsState()
    val watchHeartRate by bleManager.heartRate.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showNutriModal by remember { mutableStateOf(false) }
    var isScanningFood by remember { mutableStateOf(false) }
    var nutritionalResult by remember { mutableStateOf("") }

    val userEmailKey = remember { userEmail.replace(".", "_") }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference
    val userRef = database.child("users").child(userEmailKey)
    val dailyStatsRef = userRef.child("daily_stats").child(todayDate)

    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val mainTextColor = MaterialTheme.colorScheme.onBackground

    var showAccountDetails by remember { mutableStateOf(false) }
    var showWaterTracker by remember { mutableStateOf(false) }
    var showMeditation by remember { mutableStateOf(false) }
    var showSleepLog by remember { mutableStateOf(false) }
    var showMedsReminder by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }
    var showHealthVault by remember { mutableStateOf(false) }
    var showWatchConnect by remember { mutableStateOf(false) }
    
    var aiHealthTip by remember { mutableStateOf("Syncing your vitals...") }
    var isAiLoading by remember { mutableStateOf(true) }

    // Using Gemini Pro Vision for image analysis and Gemini Pro for text
    val visionModel = remember {
        GenerativeModel(
            modelName = "gemini-pro-vision", 
            apiKey = "AIzaSyBjr5NA9RL5ASEpbS3rmmSdvvZ_NLKHoA8", 
            generationConfig = generationConfig { temperature = 0.4f }
        )
    }
    val textModel = remember {
        GenerativeModel(
            modelName = "gemini-pro", 
            apiKey = "AIzaSyBjr5NA9RL5ASEpbS3rmmSdvvZ_NLKHoA8", 
            generationConfig = generationConfig { temperature = 0.7f }
        )
    }

    fun analyzeFood(bitmap: Bitmap) {
        isScanningFood = true
        nutritionalResult = "Analyzing your meal..."
        scope.launch {
            try {
                // Resize for efficiency
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                val response = visionModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text("Act as a professional nutritionist. Provide a brief analysis of this food image including estimated calories, macronutrients (protein, carbs, fats), and a health rating (1-10). Keep it concise.")
                    }
                )
                nutritionalResult = response.text ?: "AI couldn't identify the food clearly. Please try a different angle."
            } catch (e: Exception) {
                nutritionalResult = "AI Offline: System updating. Please try again later."
            } finally { isScanningFood = false }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            showNutriModal = true
            analyzeFood(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error launching camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to scan food.", Toast.LENGTH_SHORT).show()
        }
    }

    fun hapticFeedback() { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = textModel.generateContent("Provide a short, powerful health motivation for a high-performance athlete (max 10 words).")
                aiHealthTip = response.text?.trim() ?: "Consistency is the key to progress."
            } catch (_: Exception) { 
                aiHealthTip = "Small habits build great results." 
            } finally { 
                isAiLoading = false 
            }
        }
        
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    user = snapshot.getValue(User::class.java)
                    todaySteps = snapshot.child("daily_history").child(todayDate).getValue(Int::class.java) ?: 0
                    waterCount = snapshot.child("daily_stats").child(todayDate).child("water").getValue(Int::class.java) ?: 0
                    sleepHours = snapshot.child("daily_stats").child(todayDate).child("sleep").getValue(Float::class.java) ?: 0f
                    
                    val medsSnapshot = snapshot.child("daily_stats").child(todayDate).child("meds_v2")
                    val currentMeds = mutableMapOf<String, Boolean>()
                    medsSnapshot.children.forEach {
                        val name = it.key ?: return@forEach
                        val taken = it.getValue(Boolean::class.java) ?: false
                        currentMeds[name] = taken
                    }
                    medsStatus = currentMeds
                } catch (e: Exception) {
                    println("Error loading user data: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = cardColor, modifier = Modifier.width(300.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
                    Text("Healthitt", style = MaterialTheme.typography.headlineLarge, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    Text("Elite Performance Tracker", color = mainTextColor.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(40.dp))
                    
                    DrawerItem("Identity", Icons.Rounded.Person) { hapticFeedback(); showAccountDetails = true; scope.launch { drawerState.close() } }
                    DrawerItem("Link Wearable", Icons.Rounded.Bluetooth) { hapticFeedback(); showWatchConnect = true; scope.launch { drawerState.close() } }
                    DrawerItem("Training", Icons.Rounded.FitnessCenter) { hapticFeedback(); onNavigateToWorkouts(); scope.launch { drawerState.close() } }
                    DrawerItem("Protocols", Icons.AutoMirrored.Rounded.Assignment) { hapticFeedback(); onNavigateToTodo(); scope.launch { drawerState.close() } }
                    DrawerItem("Biometrics", Icons.Rounded.MonitorWeight) { hapticFeedback(); onNavigateToBMI(); scope.launch { drawerState.close() } }
                    DrawerItem("Health Vault", Icons.Rounded.Https) { hapticFeedback(); showHealthVault = true; scope.launch { drawerState.close() } }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = mainTextColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DrawerItem("About App", Icons.Rounded.Info) { hapticFeedback(); showAboutApp = true; scope.launch { drawerState.close() } }
                    
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onThemeToggle(!isDarkMode) }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark Mode", color = mainTextColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle(it) }, colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary))
                    }
                    Spacer(Modifier.weight(1f))
                    DrawerItem("Sign Out", Icons.AutoMirrored.Rounded.Logout, RoseAccent) { onLogout() }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = bgColor,
            topBar = { DashboardHeader(user?.name ?: "Athlete", mainTextColor) { hapticFeedback(); scope.launch { drawerState.open() } } }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                
                VitalityPowerMeter(todaySteps, waterCount, sleepHours, mainTextColor)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                NutriScanBanner(mainTextColor) { 
                    hapticFeedback()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            cameraLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error launching camera", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                HealthTipBanner(aiHealthTip, isAiLoading, mainTextColor)
                
                if (watchConnectionState == "Connected") {
                    Spacer(modifier = Modifier.height(24.dp))
                    LiveWearableBanner(watchHeartRate, mainTextColor)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Vital Telemetry", color = mainTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                StatGrid(isDarkMode, waterCount, sleepHours, medsStatus.values.count { it }, { showWaterTracker = true }, { showMeditation = true }, { showSleepLog = true }, { showMedsReminder = true })
                
                Spacer(modifier = Modifier.height(32.dp))
                DailyQuestBanner(mainTextColor)
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showNutriModal) NutriResultModal(capturedBitmap, nutritionalResult, isScanningFood, isDarkMode, mainTextColor) { showNutriModal = false }
    if (showWaterTracker) WaterTrackerModal(waterCount, { dailyStatsRef.child("water").setValue(it) }, isDarkMode, mainTextColor) { showWaterTracker = false }
    if (showSleepLog) SleepLogModal(sleepHours, { dailyStatsRef.child("sleep").setValue(it) }, isDarkMode, mainTextColor) { showSleepLog = false }
    if (showMedsReminder) MedicationManagerModal(user?.medicationsList ?: emptyList(), medsStatus, { id, taken -> dailyStatsRef.child("meds_v2").child(id).setValue(taken) }, { newList -> userRef.child("medications").setValue(newList) }, isDarkMode, mainTextColor) { showMedsReminder = false }
    if (showHealthVault) VaultDialog(user, userRef, isDarkMode) { showHealthVault = false }
    if (showWatchConnect) WatchDialog(bleManager, isDarkMode, mainTextColor) { showWatchConnect = false }
    if (showMeditation) MeditationModal(isDarkMode) { showMeditation = false }
    if (showAccountDetails) ProfileDialog(user, userRef, isDarkMode) { showAccountDetails = false }
    if (showAboutApp) AboutDialog(isDarkMode, mainTextColor) { showAboutApp = false }
}

@Composable
fun VitalityPowerMeter(steps: Int, water: Int, sleep: Float, textColor: Color) {
    val stepScore = (steps / 10000f).coerceIn(0f, 1f) * 40
    val waterScore = (water / 8f).coerceIn(0f, 1f) * 30
    val sleepScore = (sleep / 8f).coerceIn(0f, 1f) * 30
    val totalScore = (stepScore + waterScore + sleepScore).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(EmeraldPrimary.copy(alpha = 0.2f), SkyAccent.copy(alpha = 0.2f))))
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("VITALITY POWER", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$totalScore%", color = textColor, fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(12.dp))
                        Text("${steps} STEPS", color = textColor.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                Icon(Icons.Rounded.Bolt, null, tint = AmberAccent, modifier = Modifier.size(48.dp).scale(1.2f))
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(textColor.copy(alpha = 0.05f))) {
                Box(modifier = Modifier.fillMaxHeight().weight(stepScore + 0.1f).background(EmeraldPrimary))
                Box(modifier = Modifier.fillMaxHeight().weight(waterScore + 0.1f).background(SkyAccent))
                Box(modifier = Modifier.fillMaxHeight().weight(sleepScore + 0.1f).background(AmberAccent))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PowerStat("STEPS", EmeraldPrimary)
                PowerStat("WATER", SkyAccent)
                PowerStat("SLEEP", AmberAccent)
            }
            
            Spacer(Modifier.height(20.dp))
            
            Text(
                text = when {
                    totalScore > 80 -> "STATUS: ELITE ATHLETE READY"
                    totalScore > 50 -> "STATUS: OPTIMIZED FOR TRAINING"
                    else -> "STATUS: RECOVERY PROTOCOL ACTIVE"
                },
                color = textColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PowerStat(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun DailyQuestBanner(textColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SkyAccent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = SkyAccent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("DAILY QUEST", color = SkyAccent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
                Text("Hit 10k steps for a Power Boost", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun MedicationManagerModal(
    medications: List<Medication>, 
    statusMap: Map<String, Boolean>,
    onUpdateStatus: (String, Boolean) -> Unit,
    onUpdateMedsList: (List<Medication>) -> Unit,
    isDarkMode: Boolean, 
    textColor: Color, 
    onDismiss: () -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var editingMed by remember { mutableStateOf<Medication?>(null) }
    val context = LocalContext.current

    fun handleSave(med: Medication) {
        val newList = if (editingMed != null) {
            medications.map { if (it.id == med.id) med else it }
        } else {
            medications + med
        }
        if (med.isEnabled) scheduleMedicationAlarm(context, med)
        onUpdateMedsList(newList)
        isAdding = false
        editingMed = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Done") } },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Medication, null, tint = EmeraldPrimary)
                Spacer(Modifier.width(12.dp))
                Text("Medication Tracker", color = textColor, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(medications) { med ->
                        MedicationRow(
                            med = med,
                            isTaken = statusMap[med.id] ?: false,
                            onToggle = { onUpdateStatus(med.id, it) },
                            onEdit = { editingMed = med },
                            onDelete = { 
                                cancelMedicationAlarm(context, med)
                                onUpdateMedsList(medications.filter { it.id != med.id }) 
                            },
                            onToggleReminder = { enabled ->
                                val updatedMed = med.copy(isEnabled = enabled)
                                if (enabled) scheduleMedicationAlarm(context, updatedMed)
                                else cancelMedicationAlarm(context, updatedMed)
                                onUpdateMedsList(medications.map { if(it.id == med.id) updatedMed else it })
                            },
                            textColor = textColor
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { isAdding = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = if(isDarkMode) DeepSlate else Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("New Medication", color = if(isDarkMode) DeepSlate else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )

    if (isAdding || editingMed != null) {
        MedicationEditorDialog(
            initialMed = editingMed,
            onSave = ::handleSave,
            onDismiss = { isAdding = false; editingMed = null },
            isDarkMode = isDarkMode
        )
    }
}

@Composable
fun MedicationRow(
    med: Medication, 
    isTaken: Boolean, 
    onToggle: (Boolean) -> Unit, 
    onEdit: () -> Unit, 
    onDelete: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    textColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        color = textColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isTaken, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary))
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(med.time, color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, tint = SkyAccent, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, null, tint = RoseAccent, modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.1f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Icon(if(med.isEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, null, tint = if(med.isEnabled) AmberAccent else textColor.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                Text(if(med.isEnabled) "Reminder On" else "Reminder Off", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = med.isEnabled, 
                    onCheckedChange = onToggleReminder, 
                    modifier = Modifier.scale(0.8f), 
                    colors = SwitchDefaults.colors(checkedThumbColor = AmberAccent)
                )
            }
        }
    }
}

@Composable
fun MedicationEditorDialog(
    initialMed: Medication?, 
    onSave: (Medication) -> Unit, 
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    var name by remember { mutableStateOf(initialMed?.name ?: "") }
    var time by remember { mutableStateOf(initialMed?.time ?: "08:00") }
    var enabled by remember { mutableStateOf(initialMed?.isEnabled ?: true) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            Button(onClick = { if(name.isNotBlank()) onSave(Medication(initialMed?.id ?: UUID.randomUUID().toString(), name, time, enabled)) }, shape = RoundedCornerShape(12.dp)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if(initialMed == null) "Add Medication" else "Edit Medication", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Vitamin D)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Surface(
                    onClick = {
                        val t = time.split(":")
                        TimePickerDialog(context, { _, h, m -> time = String.format(Locale.US, "%02d:%02d", h, m) }, t[0].toInt(), t[1].toInt(), true).show()
                    },
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, null)
                        Spacer(Modifier.width(12.dp))
                        Text(time, fontSize = 16.sp)
                    }
                }
            }
        }
    )
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleMedicationAlarm(context: Context, med: Medication) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Intent().also { intent ->
            intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            context.startActivity(intent)
        }
        return
    }

    val intent = Intent(context, MedicationReceiver::class.java).apply {
        putExtra("med_name", med.name)
        putExtra("med_id", med.id)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, med.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    
    val calendar = Calendar.getInstance().apply {
        val t = med.time.split(":")
        set(Calendar.HOUR_OF_DAY, t[0].toInt())
        set(Calendar.MINUTE, t[1].toInt())
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
    }

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
}

fun cancelMedicationAlarm(context: Context, med: Medication) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MedicationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, med.id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
    if (pendingIntent != null) alarmManager.cancel(pendingIntent)
}

@Composable
fun NutriScanBanner(textColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = EmeraldPrimary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Nutri-Scan AI", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Snapshot your meal for instant analysis", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = EmeraldPrimary)
        }
    }
}

@Composable
fun HealthTipBanner(tip: String, loading: Boolean, textColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SkyAccent.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = AmberAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SkyAccent)
            else Text(tip, color = textColor.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LiveWearableBanner(hr: Int, textColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RoseAccent.copy(alpha = 0.05f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Favorite, null, tint = RoseAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Syncing: $hr BPM", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatGrid(isDarkMode: Boolean, water: Int, sleep: Float, meds: Int, onWater: () -> Unit, onZen: () -> Unit, onSleep: () -> Unit, onMeds: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatTile("Hydration", "$water Gl", Icons.Rounded.WaterDrop, SkyAccent, Modifier.weight(1f), isDarkMode, onWater)
            StatTile("Zen Mode", "Start", Icons.Rounded.SelfImprovement, EmeraldPrimary, Modifier.weight(1f), isDarkMode, onZen)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatTile("Recovery", "${sleep.toInt()}h", Icons.Rounded.Bedtime, AmberAccent, Modifier.weight(1f), isDarkMode, onSleep)
            StatTile("Meds", "$meds Taken", Icons.Rounded.Medication, RoseAccent, Modifier.weight(1f), isDarkMode, onMeds)
        }
    }
}

@Composable
fun StatTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, isDarkMode: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(16.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DashboardHeader(name: String, textColor: Color, onMenu: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) { 
            Icon(Icons.AutoMirrored.Rounded.Segment, null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp)) 
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("EVALUATING", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text(name.uppercase(), color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun NutriResultModal(capturedBitmap: Bitmap?, nutritionalResult: String, isScanningFood: Boolean, isDarkMode: Boolean, mainTextColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = EmeraldPrimary, fontWeight = FontWeight.Black) } },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("NUTRI-ANALYSIS", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                capturedBitmap?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.height(16.dp))
                if (isScanningFood) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                } else {
                    Text(nutritionalResult, color = mainTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    )
}

@Composable
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color? = null, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null, tint = color ?: EmeraldPrimary) },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

@Composable
fun ProfileDialog(user: User?, userRef: DatabaseReference, isDarkMode: Boolean, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf(user?.weight ?: "") }
    var height by remember { mutableStateOf(user?.height ?: "") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, 
        confirmButton = { Button(onClick = { userRef.child("weight").setValue(weight); userRef.child("height").setValue(height); onDismiss() }, shape = RoundedCornerShape(12.dp)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Profile Identity", fontWeight = FontWeight.Bold) },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { 
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (ft)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            } 
        }
    )
}

@Composable
fun WaterTrackerModal(count: Int, onUpdate: (Int) -> Unit, isDarkMode: Boolean, textColor: Color, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Save") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Text("Hydration Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(24.dp))
                Text("$count Glasses", color = textColor, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FilledIconButton(onClick = { if(count > 0) onUpdate(count - 1) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.Remove, null) }
                    FilledIconButton(onClick = { onUpdate(count + 1) }, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary)) { Icon(Icons.Rounded.Add, null) }
                }
            }
        }
    )
}

@Composable
fun SleepLogModal(hours: Float, onUpdate: (Float) -> Unit, isDarkMode: Boolean, textColor: Color, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Log") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Recovery Sleep", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(24.dp))
                Text("${hours.toInt()} Hours", color = textColor, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                Slider(value = hours, onValueChange = onUpdate, valueRange = 0f..12f, colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary))
            }
        }
    )
}

@Composable
fun MeditationModal(isDarkMode: Boolean, onDismiss: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    var zenText by remember { mutableStateOf("Ready?") }
    val infiniteTransition = rememberInfiniteTransition(label = "zen_pulse_transition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zen_pulse_scale"
    )

    LaunchedEffect(active) {
        if (active) {
            while (true) {
                zenText = "Inhale..."
                delay(3000)
                zenText = "Exhale..."
                delay(3000)
            }
        } else {
            zenText = "Ready?"
        }
    }

    val color by animateColorAsState(
        targetValue = if (active) SkyAccent else EmeraldPrimary,
        animationSpec = tween(2000),
        label = "zen_pulse_color"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Finish", color = EmeraldPrimary) } },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    if (active) {
                        Surface(
                            modifier = Modifier.size(120.dp).scale(scale),
                            shape = CircleShape,
                            color = color.copy(alpha = 0.2f)
                        ) {}
                    }
                    
                    Icon(
                        Icons.Rounded.SelfImprovement,
                        null,
                        tint = color,
                        modifier = Modifier.size(100.dp).scale(if(active) scale * 0.8f else 1f)
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = zenText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(40.dp))
                
                Button(
                    onClick = { active = !active },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Text(if (active) "Stop Session" else "Start Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun VaultDialog(user: User?, userRef: DatabaseReference, isDarkMode: Boolean, onDismiss: () -> Unit) {
    var contact by remember { mutableStateOf(user?.emergencyContact ?: "") }
    var blood by remember { mutableStateOf(user?.bloodGroup ?: "") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, 
        confirmButton = { Button(onClick = { userRef.child("emergencyContact").setValue(contact); userRef.child("bloodGroup").setValue(blood); onDismiss() }, shape = RoundedCornerShape(12.dp)) { Text("Save") } },
        title = { Text("Health Vault", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = blood, onValueChange = { blood = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        }
    )
}

@Composable
fun WatchDialog(bleManager: BleWatchManager, isDarkMode: Boolean, textColor: Color, onDismiss: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    val foundDevices by bleManager.foundDevices.collectAsState()
    val connectionState by bleManager.connectionState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.all { it.value }) { isScanning = true; bleManager.startDiscovery() }
    }

    AlertDialog(
        onDismissRequest = { bleManager.stopDiscovery(); onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Close") } },
        title = { Text("Universal Sync", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isScanning) {
                    Button(onClick = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        permissionLauncher.launch(perms)
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Initialize Scan") }
                } else {
                    if (connectionState == "Connecting...") LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldPrimary)
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(foundDevices) { device ->
                            val deviceName = try {
                                @SuppressLint("MissingPermission")
                                device.name ?: "Wearable Device"
                            } catch (_: SecurityException) {
                                "Wearable Device"
                            }
                            Text(deviceName, modifier = Modifier.fillMaxWidth().clickable { bleManager.connectDevice(device) }.padding(12.dp), color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AboutDialog(isDarkMode: Boolean, textColor: Color, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Close") } }, 
        title = { Text("Core System", fontWeight = FontWeight.Bold) },
        text = { Text("Healthitt version $CURRENT_APP_VERSION. Premium athlete telemetry engine.", color = textColor) }
    )
}
