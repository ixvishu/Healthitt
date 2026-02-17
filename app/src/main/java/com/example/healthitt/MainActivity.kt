package com.example.healthitt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthitt.data.User
import com.example.healthitt.notifications.NotificationScheduler
import com.example.healthitt.services.StepCounterService
import com.example.healthitt.ui.auth.LoginScreen
import com.example.healthitt.ui.auth.RegisterScreen
import com.example.healthitt.ui.theme.HealthittTheme
import com.example.healthitt.ui.main.MainScreen
import com.example.healthitt.ui.workout.WorkoutScreen
import com.example.healthitt.ui.bmi.BMIScreen
import com.example.healthitt.ui.todo.TodoScreen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthittApp()
        }
    }
}

@Composable
fun HealthittApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("healthitt_prefs", Context.MODE_PRIVATE) }
    
    var savedEmail by remember { mutableStateOf(sharedPrefs.getString("logged_in_email", null)) }
    var isDarkMode by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf(sharedPrefs.getString("logged_in_name", "Athlete") ?: "Athlete") }
    
    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference

    fun toggleDarkMode(newThemeValue: Boolean) {
        isDarkMode = newThemeValue
        savedEmail?.let {
            val userEmailKey = it.replace(".", "_")
            database.child("users").child(userEmailKey).child("isDarkMode").setValue(newThemeValue)
        }
    }

    // Sync Theme Preference and Name from Firebase
    DisposableEffect(savedEmail) {
        var userRef: com.google.firebase.database.DatabaseReference? = null
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isDarkMode = snapshot.child("isDarkMode").getValue(Boolean::class.java) ?: true
                userName = snapshot.child("name").getValue(String::class.java) ?: "Athlete"
                sharedPrefs.edit().putString("logged_in_name", userName).apply()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Sync Error: ${error.message}")
            }
        }
        
        if (savedEmail != null) {
            val userEmailKey = savedEmail!!.replace(".", "_")
            userRef = database.child("users").child(userEmailKey)
            userRef.addValueEventListener(listener)
        } else {
            isDarkMode = true 
        }
        
        onDispose {
            userRef?.removeEventListener(listener)
        }
    }

    HealthittTheme(darkTheme = isDarkMode) {
        val navController = rememberNavController()
        val startDestination = if (savedEmail != null) "main/$savedEmail" else "welcome"

        fun startStepService(email: String) {
            val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                putExtra("user_email", email)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
            } else true

            if (activityGranted) {
                savedEmail?.let { startStepService(it) }
            }
        }

        LaunchedEffect(savedEmail) {
            NotificationScheduler.scheduleDailyWellnessReminders(context)
            
            if (savedEmail != null) {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (permissions.isNotEmpty()) {
                    permissionLauncher.launch(permissions.toTypedArray())
                } else {
                    startStepService(savedEmail!!)
                }
            }
        }

        val usersRef = database.child("users")
        
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("welcome") {
                com.example.healthitt.ui.homescreen.WelcomeScreen(onNavigateToLogin = { navController.navigate("login") })
            }
            composable("login") {
                LoginScreen(
                    onLoginClicked = { emailOrPhone, password ->
                        scope.launch {
                            try {
                                val snapshot = usersRef.get().await()
                                var loggedInUser: User? = null
                                for (userSnapshot in snapshot.children) {
                                    val user = try { userSnapshot.getValue(User::class.java) } catch(e: Exception) { null }

                                    if (user != null && (user.email == emailOrPhone || user.phone == emailOrPhone) && user.password == password) {
                                        loggedInUser = user
                                        break
                                    }
                                }
                                if (loggedInUser != null) {
                                    sharedPrefs.edit()
                                        .putString("logged_in_email", loggedInUser.email)
                                        .putString("logged_in_name", loggedInUser.name)
                                        .apply()
                                    savedEmail = loggedInUser.email
                                    userName = loggedInUser.name
                                    navController.navigate("main/${loggedInUser.email}") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Login failed. Check credentials.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                val errorMsg = e.localizedMessage ?: "Unknown error"
                                if (errorMsg.contains("Permission denied", ignoreCase = true)) {
                                    Toast.makeText(context, "Permission Denied: Update Firebase Rules", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Login Error: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterClicked = { name, dob, weight, height, gender, email, phone, password ->
                        scope.launch {
                            val userId = email.replace(".", "_")
                            val newUser = User(
                                name = name, dob = dob, weight = weight, height = height, 
                                gender = gender, email = email, phone = phone, password = password
                            )
                            try {
                                usersRef.child(userId).setValue(newUser).await()
                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            } catch (e: Exception) {
                                val errorMsg = e.localizedMessage ?: "Unknown error"
                                if (errorMsg.contains("Permission denied", ignoreCase = true)) {
                                    Toast.makeText(context, "Cannot Create Profile: Check Firebase Write Rules", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Registration failed: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    checkUniqueness = { email, phone, callback ->
                        scope.launch {
                            try {
                                val snapshot = usersRef.get().await()
                                var emailExists = false
                                var phoneExists = false
                                for (userSnapshot in snapshot.children) {
                                    val user = try { userSnapshot.getValue(User::class.java) } catch(e: Exception) { null }
                                    if (user?.email == email) emailExists = true
                                    if (user?.phone == phone) phoneExists = true
                                }
                                when {
                                    emailExists -> callback(false, "Email already exists")
                                    phoneExists -> callback(false, "Phone already exists")
                                    else -> callback(true, null)
                                }
                            } catch (e: Exception) {
                                callback(false, "Check failed: ${e.message}")
                            }
                        }
                    }
                )
            }
            composable(
                route = "main/{userEmail}",
                arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val userEmailArg = backStackEntry.arguments?.getString("userEmail") ?: ""
                MainScreen(
                    mainNavController = navController,
                    userEmail = userEmailArg,
                    userName = userName,
                    isDarkMode = isDarkMode,
                    onThemeToggle = ::toggleDarkMode,
                    onLogout = {
                        context.stopService(Intent(context, StepCounterService::class.java))
                        sharedPrefs.edit().remove("logged_in_email").remove("logged_in_name").apply()
                        savedEmail = null
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable("workouts") {
                WorkoutScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "bmi/{userEmail}",
                arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val userEmailArg = backStackEntry.arguments?.getString("userEmail") ?: ""
                BMIScreen(userEmail = userEmailArg, onBack = { navController.popBackStack() })
            }
            composable(
                route = "todo/{userEmail}",
                arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
            ) { backStackEntry ->
                val userEmailArg = backStackEntry.arguments?.getString("userEmail") ?: ""
                TodoScreen(userEmail = userEmailArg, onBack = { navController.popBackStack() })
            }
        }
    }
}
