@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.healthitt.ui.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.ui.theme.EmeraldPrimary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class CommunityPost(
    val id: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

@Composable
fun CommunityScreen(userName: String) {
    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var showPostDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("community_posts")

    LaunchedEffect(Unit) {
        database.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CommunityPost>()
                snapshot.children.forEach {
                    val post = it.getValue(CommunityPost::class.java)
                    if (post != null) list.add(post)
                }
                posts = list.reversed()
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Community Feed", fontWeight = FontWeight.Black)
                        Text("Connect with fellow athletes", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPostDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New Post", fontWeight = FontWeight.Bold) },
                shape = CircleShape
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostItem(post)
                }
            }
        }
    }

    if (showPostDialog) {
        NewPostDialog(
            userName = userName,
            onDismiss = { showPostDialog = false },
            onPost = { content ->
                val id = database.push().key ?: return@NewPostDialog
                val newPost = CommunityPost(id, userName, content, System.currentTimeMillis())
                database.child(id).setValue(newPost)
                showPostDialog = false
            }
        )
    }
}

@Composable
fun PostItem(post: CommunityPost) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(post.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = post.userName.take(1).uppercase(),
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                post.content, 
                fontSize = 15.sp, 
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun NewPostDialog(userName: String, onDismiss: () -> Unit, onPost: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New Post", fontWeight = FontWeight.Black, fontSize = 24.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("Share your training progress or health tips...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { if(text.isNotBlank()) onPost(text) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                enabled = text.isNotBlank()
            ) {
                Text("Share Protocol", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
