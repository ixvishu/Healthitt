package com.example.healthitt.ui.main

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                AppBottomNavigation(navController = bottomNavController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            NavHost(
                navController = bottomNavController, 
                startDestination = "dashboard"
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
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val view = LocalView.current
    val items = listOf(
        NavigationItem("dashboard", Icons.Rounded.Home, "Home"),
        NavigationItem("leaderboard", Icons.Rounded.BarChart, "Ranking"),
        NavigationItem("community", Icons.Rounded.Groups, "Feed")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 12.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                
                Box(
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
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp
                        )
                        
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(width = 16.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(EmeraldPrimary)
                            )
                        }
                    }
                }
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
