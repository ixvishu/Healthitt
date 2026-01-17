package com.example.healthitt.ui.bmi

import android.view.SoundEffectConstants
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.data.User
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMIScreen(userEmail: String, onBack: () -> Unit) {
    val view = LocalView.current
    val userEmailKey = remember { userEmail.replace(".", "_") }
    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference
    val userRef = database.child("users").child(userEmailKey)

    var weight by remember { mutableStateOf("") }
    var heightFeet by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var age by remember { mutableIntStateOf(25) }
    
    var bmiResult by remember { mutableDoubleStateOf(0.0) }
    var maintenanceCals by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    fun calculateAge(dob: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dob) ?: return 25
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var a = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) a--
            a
        } catch (e: Exception) { 25 }
    }

    LaunchedEffect(Unit) {
        userRef.get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            user?.let {
                weight = it.weight
                val h = it.height.split(".")
                if (h.size == 2) {
                    heightFeet = h[0]
                    heightInches = h[1]
                } else if (h.isNotEmpty()) {
                    heightFeet = h[0]
                }
                age = calculateAge(it.dob)
                gender = it.gender
                bmiResult = it.bmi
            }
        }
    }

    fun runCalculations() {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        val w = weight.toDoubleOrNull() ?: return
        val hFeet = heightFeet.toDoubleOrNull() ?: return
        val hInches = heightInches.toDoubleOrNull() ?: 0.0
        
        // 1. BMI Calculation (Precise Metric Conversion)
        val heightInCm = ((hFeet * 12) + hInches) * 2.54
        val heightInMeters = heightInCm / 100.0
        val bmi = w / (heightInMeters * heightInMeters)
        bmiResult = round(bmi * 10.0) / 10.0

        // 2. Maintenance Calories (Mifflin-St Jeor Equation)
        val bmr = if (gender == "Male") {
            (10 * w) + (6.25 * heightInCm) - (5 * age) + 5
        } else {
            (10 * w) + (6.25 * heightInCm) - (5 * age) - 161
        }
        maintenanceCals = (bmr * 1.2).toInt()

        showResult = true
        
        // Sync to Firebase
        userRef.child("bmi").setValue(bmiResult)
        userRef.child("weight").setValue(weight)
        userRef.child("height").setValue("$heightFeet.$heightInches")
        userRef.child("gender").setValue(gender)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Calculator", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF1A0033))))) {
            Column(
                modifier = Modifier.padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(15.dp, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Personal Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("Male", "Female").forEach { g ->
                                Button(
                                    onClick = { gender = g; view.playSoundEffect(SoundEffectConstants.CLICK) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (gender == g) Color(0xFFBB86FC) else Color.White.copy(0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(g, color = if (gender == g) Color.Black else Color.White)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = weight, onValueChange = { weight = it },
                            label = { Text("Current Weight (kg)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = heightFeet, onValueChange = { heightFeet = it },
                                label = { Text("Height (ft)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                            )
                            OutlinedTextField(
                                value = heightInches, onValueChange = { heightInches = it },
                                label = { Text("Inches") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFBB86FC))
                            )
                        }
                        
                        Button(
                            onClick = { runCalculations() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Calculate Results", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showResult || bmiResult > 0) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Your Body Mass Index", color = Color.White.copy(0.6f), fontSize = 14.sp)
                    Text(bmiResult.toString(), color = Color(0xFFBB86FC), fontSize = 56.sp, fontWeight = FontWeight.Black)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFBB86FC).copy(0.1f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Daily Calorie Insights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                            CalorieGoalItem("Maintenance", "$maintenanceCals kcal", "To stay at current weight", Color.White)
                            CalorieGoalItem("Weight Loss", "${maintenanceCals - 500} kcal", "Recommended 500 cal deficit", Color(0xFF03DAC5))
                            CalorieGoalItem("Weight Gain", "${maintenanceCals + 400} kcal", "Healthy muscle building", Color(0xFFFFD700))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                Text("Official BMI Classification", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                BMITable()
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CalorieGoalItem(label: String, value: String, description: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, color = Color.White.copy(0.5f), fontSize = 12.sp)
        }
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(0.05f))
}

@Composable
fun BMITable() {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(0.05f)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))) {
        BMIRow("BMI Range", "Health Status", isHeader = true)
        BMIRow("Less than 18.5", "Underweight", Color(0xFFFFC107))
        BMIRow("18.5 – 24.9", "Healthy", Color(0xFF4CAF50))
        BMIRow("25.0 – 29.9", "Overweight", Color(0xFFFF9800))
        BMIRow("30.0 or more", "Obese", Color(0xFFF44336))
    }
}

@Composable
fun BMIRow(range: String, category: String, color: Color = Color.White, isHeader: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(range, color = if(isHeader) Color(0xFFBB86FC) else Color.White.copy(0.7f), fontWeight = if(isHeader) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(category, color = if(isHeader) Color(0xFFBB86FC) else color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
