package com.example.healthitt.ui.main

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthitt.ui.community.CommunityScreen
import com.example.healthitt.ui.dashboard.DashboardScreen
import com.example.healthitt.ui.leaderboard.LeaderboardScreen
import com.example.healthitt.ui.theme.EmeraldPrimary

@Composable
fun MainScreen(
    mainNavController: NavController,
    userEmail: String,
    userName: String,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Adaptive Bottom Navigation
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = if (isTablet) 40.dp else 32.dp, topEnd = if (isTablet) 40.dp else 32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 16.dp,
                shadowElevation = 24.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                AppBottomNavigation(navController = bottomNavController, isTablet = isTablet)
            }
        }
    ) { padding ->
        NavHost(
            navController = bottomNavController, 
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    userEmail = userEmail,
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onLogout = onLogout,
                    onNavigateToWorkouts = { mainNavController.navigate("workouts") },
                    onNavigateToBMI = { mainNavController.navigate("bmi/$userEmail") },
                    onNavigateToTodo = { mainNavController.navigate("todo/$userEmail") }
                )
            }
            composable("leaderboard") { 
                LeaderboardScreen() 
            }
            composable("community") { 
                CommunityScreen(userName = userName) 
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController, isTablet: Boolean) {
    val view = LocalView.current
    val items = listOf(
        NavigationItem("dashboard", Icons.Rounded.Home, "Home"),
        NavigationItem("leaderboard", Icons.Rounded.BarChart, "Ranking"),
        NavigationItem("community", Icons.Rounded.Groups, "Feed")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 96.dp else 80.dp)
            .padding(horizontal = if (isTablet) 64.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .noRippleClickable {
                        if (currentRoute != item.route) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(if (isSelected) (if(isTablet) 32.dp else 28.dp) else (if(isTablet) 28.dp else 24.dp))
                )
                Text(
                    text = item.title,
                    fontSize = if (isTablet) 13.sp else 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.alpha(if (isSelected) 1f else 0.7f)
                )
            }
        }
    }
}

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

data class NavigationItem(
    val route: String, 
    val icon: androidx.compose.ui.graphics.vector.ImageVector, 
    val title: String
)
