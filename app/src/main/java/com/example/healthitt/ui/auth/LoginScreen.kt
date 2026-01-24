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
        // Modern Animated Background
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-150).dp, y = (-100).dp)
                .blur(80.dp)
                .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(300.dp)
                .offset(x = 100.dp, y = 100.dp)
                .blur(60.dp)
                .background(NeonGreen.copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
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
                    brush = Brush.horizontalGradient(ActiveGradient)
                ),
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Glassmorphism Input Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PureWhite.copy(0.05f), RoundedCornerShape(32.dp)),
                color = GlassCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Enter your details", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    
                    AuthTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it; errorText = "" },
                        label = "Email or Phone",
                        icon = Icons.Rounded.Mail
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; errorText = "" },
                        label = "Password",
                        icon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
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

            Spacer(modifier = Modifier.height(48.dp))

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
                    .fillMaxWidth()
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
        }
    }
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
