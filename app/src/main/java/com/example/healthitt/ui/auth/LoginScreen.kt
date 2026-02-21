package com.example.healthitt.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.ui.theme.*

@Composable
fun LoginScreen(
    onLoginClicked: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = configuration.screenWidthDp >= 600

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightDark)
    ) {
        // Adaptive Background Orbs
        Box(
            modifier = Modifier
                .size(screenWidth * 0.8f)
                .offset(x = (-screenWidth * 0.3f), y = (-screenWidth * 0.2f))
                .blur(80.dp)
                .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(screenWidth * 0.6f)
                .offset(x = (screenWidth * 0.2f), y = (screenWidth * 0.2f))
                .blur(60.dp)
                .background(NeonGreen.copy(alpha = 0.1f), CircleShape)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (availableWidth * 0.1f).coerceAtLeast(24.dp))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(availableHeight * 0.05f))

                Text(
                    text = "Welcome Back",
                    color = PureWhite.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Text(
                    text = "Healthitt",
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.horizontalGradient(ActiveGradient),
                        fontSize = (availableWidth.value * 0.12f).coerceIn(40f, 72f).sp
                    ),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                
                Spacer(modifier = Modifier.height(availableHeight * 0.06f))

                // Adaptive Input Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.8f else 1f)
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, PureWhite.copy(0.05f), RoundedCornerShape(32.dp)),
                    color = GlassCard.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(modifier = Modifier.padding(adaptivePadding())) {
                        Text("Enter your details", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        
                        AuthTextField(
                            emailOrPhone,
                            { emailOrPhone = it; errorText = "" },
                            "Email or Phone",
                            Icons.Rounded.Mail
                        )
                        
                        Spacer(Modifier.height(20.dp))
                        
                        AuthTextField(
                            password,
                            { password = it; errorText = "" },
                            "Password",
                            Icons.Rounded.Lock,
                            true,
                            passwordVisible,
                            { passwordVisible = !passwordVisible }
                        )
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = SunsetOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(availableHeight * 0.06f))

                Button(
                    onClick = {
                        if (emailOrPhone.isEmpty() || password.isEmpty()) {
                            errorText = "Please fill in all fields"
                        } else {
                            isLoading = true
                            onLoginClicked(emailOrPhone, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.6f else 1f)
                        .align(Alignment.CenterHorizontally)
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(ActiveGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = NightDark, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            Text("Sign In", color = NightDark, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New here?", color = PureWhite.copy(0.4f))
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Create Account", color = NeonCyan, fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(availableHeight * 0.05f))
            }
        }
    }
}

@Composable
private fun adaptivePadding(): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    return if (configuration.screenWidthDp > 600) 40.dp else 24.dp
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(22.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle!!) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, 
                        null, 
                        tint = PureWhite.copy(0.3f)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = PureWhite.copy(0.1f),
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = PureWhite.copy(0.4f),
            cursorColor = NeonCyan
        ),
        singleLine = true
    )
}
