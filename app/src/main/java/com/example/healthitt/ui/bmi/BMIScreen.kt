package com.example.healthitt.ui.bmi

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.data.User
import com.example.healthitt.ui.theme.*
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
    
    val activityLevels = listOf(
        "Sedentary" to 1.2,
        "Light" to 1.375,
        "Moderate" to 1.55,
        "Active" to 1.725,
        "Athlete" to 1.9
    )
    var selectedActivityIndex by remember { mutableIntStateOf(0) }
    
    var bmiResult by remember { mutableDoubleStateOf(0.0) }
    var maintenanceCals by remember { mutableIntStateOf(0) }
    var fatLossCals by remember { mutableIntStateOf(0) }
    var muscleGainCals by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    fun hapticFeedback() { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }

    fun calculateAge(dob: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dob) ?: return 25
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var a = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) a--
            a
        } catch (_: Exception) { 25 }
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
        hapticFeedback()
        val w = weight.toDoubleOrNull() ?: return
        val hFeet = heightFeet.toDoubleOrNull() ?: return
        val hInches = heightInches.toDoubleOrNull() ?: 0.0
        
        val totalInches = (hFeet * 12) + hInches
        val heightInCm = totalInches * 2.54
        val heightInMeters = heightInCm / 100.0
        val bmi = w / (heightInMeters * heightInMeters)
        bmiResult = round(bmi * 10.0) / 10.0

        val bmr = if (gender == "Male") {
            (10 * w) + (6.25 * heightInCm) - (5 * age) + 5
        } else {
            (10 * w) + (6.25 * heightInCm) - (5 * age) - 161
        }
        
        val multiplier = activityLevels[selectedActivityIndex].second
        maintenanceCals = (bmr * multiplier).toInt()
        fatLossCals = maintenanceCals - 500
        muscleGainCals = maintenanceCals + 400

        showResult = true
        
        userRef.child("bmi").setValue(bmiResult)
        userRef.child("weight").setValue(weight)
        userRef.child("height").setValue("$heightFeet.$heightInches")
        userRef.child("gender").setValue(gender)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("BODY METRICS", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 18.sp, 
                        color = PureWhite,
                        letterSpacing = 2.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DeepSlate)
            )
        },
        containerColor = DeepSlate
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MutedSlate,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, PureWhite.copy(0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("BIOMETRIC INPUT", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Male", "Female").forEach { g ->
                            val isSelected = gender == g
                            Surface(
                                onClick = { gender = g; hapticFeedback() },
                                modifier = Modifier.weight(1f).height(52.dp),
                                color = if (isSelected) EmeraldPrimary else DeepSlate,
                                shape = RoundedCornerShape(14.dp),
                                border = if (!isSelected) BorderStroke(1.dp, PureWhite.copy(0.1f)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(g.uppercase(), color = if(isSelected) DeepSlate else PureWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("WEIGHT (KG)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite, 
                            unfocusedTextColor = PureWhite, 
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = PureWhite.copy(0.1f),
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = PureWhite.copy(0.4f)
                        )
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = heightFeet, onValueChange = { heightFeet = it },
                            label = { Text("FEET") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = PureWhite.copy(0.1f), focusedLabelColor = EmeraldPrimary)
                        )
                        OutlinedTextField(
                            value = heightInches, onValueChange = { heightInches = it },
                            label = { Text("INCHES") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = PureWhite.copy(0.1f), focusedLabelColor = EmeraldPrimary)
                        )
                    }

                    Text("ACTIVITY LEVEL", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(activityLevels) { index, level ->
                            FilterChip(
                                selected = selectedActivityIndex == index,
                                onClick = { selectedActivityIndex = index; hapticFeedback() },
                                label = { Text(level.first.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = DeepSlate,
                                    containerColor = DeepSlate,
                                    labelColor = PureWhite.copy(0.6f)
                                ),
                                border = if (selectedActivityIndex != index) BorderStroke(1.dp, PureWhite.copy(0.1f)) else null
                            )
                        }
                    }
                    
                    Button(
                        onClick = { runCalculations() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("CALCULATE RESULTS", color = DeepSlate, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showResult || bmiResult > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                    
                    // BMI Score Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MutedSlate,
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, PureWhite.copy(0.05f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BMI SCORE", color = PureWhite.copy(0.4f), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(bmiResult.toString(), color = EmeraldPrimary, fontSize = 72.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // BMI Progress Bar
                            val bmiProgress = ((bmiResult - 15) / (35 - 15)).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { bmiProgress },
                                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                                color = SkyAccent,
                                trackColor = PureWhite.copy(0.1f),
                                strokeCap = StrokeCap.Round
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("15", color = PureWhite.copy(0.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("NORMAL RANGE (18.5 - 24.9)", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                Text("35+", color = PureWhite.copy(0.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fuel Requirements Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MutedSlate,
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, PureWhite.copy(0.05f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("DAILY FUEL REQUIREMENTS", color = PureWhite.copy(0.4f), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(24.dp))
                            
                            FuelRow("Maintenance", "Stay as you are", "$maintenanceCals", EmeraldPrimary)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = PureWhite.copy(0.05f))
                            FuelRow("Fat Loss", "Targeted deficit", "$fatLossCals", SkyAccent)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = PureWhite.copy(0.05f))
                            FuelRow("Muscle Gain", "Anabolic surplus", "$muscleGainCals", AmberAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Health Categories Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MutedSlate,
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, PureWhite.copy(0.05f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("HEALTH CATEGORIES", color = PureWhite.copy(0.4f), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(24.dp))
                            
                            CategoryRow("Underweight", "< 18.5", AmberAccent)
                            CategoryRow("Healthy", "18.5 - 24.9", EmeraldPrimary)
                            CategoryRow("Overweight", "25.0 - 29.9", SkyAccent)
                            CategoryRow("Obese", "> 30.0", RoseAccent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun FuelRow(label: String, desc: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(desc, color = PureWhite.copy(0.3f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(value, color = PureWhite, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("kcal", color = PureWhite.copy(0.3f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CategoryRow(label: String, range: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
        Spacer(Modifier.width(16.dp))
        Text(label, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(range, color = PureWhite.copy(0.4f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
