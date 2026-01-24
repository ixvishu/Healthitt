package com.example.healthitt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.example.healthitt.ui.dashboard.DashboardScreen
import com.example.healthitt.ui.homescreen.WelcomeScreen
import com.example.healthitt.ui.workout.WorkoutScreen
import com.example.healthitt.ui.bmi.BMIScreen
import com.example.healthitt.ui.todo.TodoScreen
import com.example.healthitt.ui.theme.HealthittTheme
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthittTheme {
                HealthittApp()
            }
        }
    }
}

@Composable
fun HealthittApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("healthitt_prefs", Context.MODE_PRIVATE) }
    
    val savedEmail = remember { sharedPrefs.getString("logged_in_email", null) }
    val startDestination = if (savedEmail != null) "dashboard/$savedEmail" else "welcome"

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
            if (!activityGranted) {
                Toast.makeText(context, "Activity permission required for steps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }

        NotificationScheduler.scheduleDailyWellnessReminders(context)

        savedEmail?.let { email ->
            val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                putExtra("user_email", email)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users")
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("welcome") {
            WelcomeScreen(onNavigateToLogin = { navController.navigate("login") })
        }
        composable("login") {
            LoginScreen(
                onLoginClicked = { emailOrPhone, password ->
                    scope.launch {
                        try {
                            val snapshot = database.get().await()
                            var loggedInUser: User? = null
                            for (userSnapshot in snapshot.children) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user != null && (user.email == emailOrPhone || user.phone == emailOrPhone) && user.password == password) {
                                    loggedInUser = user
                                    break
                                }
                            }
                            if (loggedInUser != null) {
                                sharedPrefs.edit().putString("logged_in_email", loggedInUser.email).apply()
                                
                                val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                                    putExtra("user_email", loggedInUser.email)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }

                                navController.navigate("dashboard/${loggedInUser.email}") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Login failed. Check credentials.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                            database.child(userId).setValue(newUser).await()
                            Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                            navController.navigate("login")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
                checkUniqueness = { email, phone, callback ->
                    scope.launch {
                        try {
                            val snapshot = database.get().await()
                            var emailExists = false
                            var phoneExists = false
                            for (userSnapshot in snapshot.children) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user?.email == email) emailExists = true
                                if (user?.phone == phone) phoneExists = true
                            }
                            when {
                                emailExists -> callback(false, "Email already exists")
                                phoneExists -> callback(false, "Phone already exists")
                                else -> callback(true, null)
                            }
                        } catch (e: Exception) {
                            callback(false, "Error: ${e.message}")
                        }
                    }
                }
            )
        }
        composable(
            route = "dashboard/{userEmail}",
            arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            DashboardScreen(
                userEmail = userEmail,
                onLogout = {
                    context.stopService(Intent(context, StepCounterService::class.java))
                    sharedPrefs.edit().remove("logged_in_email").apply()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToWorkouts = { isDark -> navController.navigate("workouts/$isDark") },
                onNavigateToBMI = { navController.navigate("bmi/$userEmail") },
                onNavigateToTodo = { navController.navigate("todo/$userEmail") }
            )
        }
        composable(
            route = "workouts/{isDarkMode}",
            arguments = listOf(navArgument("isDarkMode") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isDark = backStackEntry.arguments?.getBoolean("isDarkMode") ?: true
            WorkoutScreen(isDarkMode = isDark, onBack = { navController.popBackStack() })
        }
        composable(
            route = "bmi/{userEmail}",
            arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            BMIScreen(userEmail = userEmail, onBack = { navController.popBackStack() })
        }
        composable(
            route = "todo/{userEmail}",
            arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("userEmail") ?: ""
            TodoScreen(userEmail = userEmail, onBack = { navController.popBackStack() })
        }
    }
}
