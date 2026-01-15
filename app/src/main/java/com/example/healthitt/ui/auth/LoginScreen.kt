package com.example.healthitt.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthitt.ui.theme.HealthittTheme

@Composable
fun LoginScreen(
    onLoginClicked: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color(0xFF4A148C))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Email or Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onLoginClicked(emailOrPhone, password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
            ) {
                Text("Login", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateToRegister) {
                Text("Don't have an account? Register", color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HealthittTheme {
        LoginScreen(onLoginClicked = { _, _ -> }, onNavigateToRegister = {})
    }
}