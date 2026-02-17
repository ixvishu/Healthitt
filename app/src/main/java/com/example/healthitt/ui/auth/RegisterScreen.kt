package com.example.healthitt.ui.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.healthitt.data.Avatars
import com.example.healthitt.ui.theme.*
import java.util.*

@Composable
fun RegisterScreen(
    onRegisterClicked: (String, String, String, String, String, String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    checkUniqueness: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(Avatars.list[0]) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            dob = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = Modifier.fillMaxSize().background(NightDark)) {
        // Modern Animated Background Orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-150).dp, y = (-100).dp)
                .blur(80.dp)
                .background(NeonCyan.copy(alpha = 0.1f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(300.dp)
                .offset(x = 100.dp, y = 100.dp)
                .blur(60.dp)
                .background(NeonGreen.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(60.dp))
            
            Text(
                text = "Join Us",
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
            
            Spacer(modifier = Modifier.height(30.dp))

            // Avatar Selector
            Text("Choose Your Avatar", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(Avatars.list) { avatarUrl ->
                    val isSelected = selectedAvatar == avatarUrl
                    Surface(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .clickable { selectedAvatar = avatarUrl },
                        shape = CircleShape,
                        color = if (isSelected) NeonGreen.copy(0.2f) else PureWhite.copy(0.1f),
                        border = if (isSelected) BorderStroke(3.dp, NeonGreen) else null
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().padding(if(isSelected) 4.dp else 0.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Glassmorphism Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PureWhite.copy(0.05f), RoundedCornerShape(32.dp)),
                color = GlassCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Your Details", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    
                    RegisterField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Rounded.Person)
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Gender", color = PureWhite.copy(0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            val isSelected = gender == g
                            Surface(
                                onClick = { gender = g },
                                modifier = Modifier.weight(1f).height(44.dp),
                                color = if (isSelected) NeonGreen.copy(0.2f) else PureWhite.copy(0.05f),
                                border = if (isSelected) BorderStroke(1.dp, NeonGreen) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(g, color = if (isSelected) NeonGreen else PureWhite.copy(0.4f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    RegisterField(
                        value = dob, onValueChange = { }, label = "Birth Date", icon = Icons.Rounded.CalendarToday, 
                        readOnly = true, isDate = true, onIconClick = { datePickerDialog.show() }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        RegisterField(value = weight, onValueChange = { weight = it }, label = "Weight (kg)", icon = Icons.Rounded.MonitorWeight, modifier = Modifier.weight(1f))
                        RegisterField(value = height, onValueChange = { height = it }, label = "Height (ft)", icon = Icons.Rounded.Height, modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    RegisterField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Rounded.Email)
                    Spacer(modifier = Modifier.height(20.dp))
                    RegisterField(value = phone, onValueChange = { phone = it }, label = "Phone", icon = Icons.Rounded.Phone)
                    Spacer(modifier = Modifier.height(20.dp))
                    RegisterField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Rounded.Lock, isPassword = true)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    if (name.isNotEmpty() && dob.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
                        checkUniqueness(email, phone) { isUnique, _ ->
                            if (isUnique) onRegisterClicked(name, dob, weight, height, gender, email, phone, password, selectedAvatar)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(ActiveGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Create Profile", color = NightDark, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already a member? Sign In", color = NeonCyan, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun RegisterField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isDate: Boolean = false,
    isPassword: Boolean = false,
    onIconClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        leadingIcon = { Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isDate) { { IconButton(onClick = onIconClick!!) { Icon(Icons.Rounded.Event, null, tint = NeonCyan) } } } else null,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = PureWhite.copy(0.1f),
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = PureWhite.copy(0.4f)
        )
    )
}
