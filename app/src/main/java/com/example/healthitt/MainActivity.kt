package com.example.healthitt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthitt.ui.auth.LoginScreen
import com.example.healthitt.ui.auth.RegisterScreen
import com.example.healthitt.ui.homescreen.WelcomeScreen
import com.example.healthitt.ui.theme.HealthittTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthittTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HealthittApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HealthittApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = modifier
    ) {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginClicked = { email, password ->
                    // Handle login logic here
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterClicked = { name, age, email, phone, password ->
                    // Handle registration logic here
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
