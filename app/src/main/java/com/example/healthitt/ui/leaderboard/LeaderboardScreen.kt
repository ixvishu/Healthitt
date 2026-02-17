@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.healthitt.ui.leaderboard

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.healthitt.ui.theme.AmberAccent
import com.example.healthitt.ui.theme.EmeraldPrimary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class LeaderboardEntry(val name: String, val steps: Int, val profilePicUrl: String)

@Composable
fun LeaderboardScreen() {
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users")
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<LeaderboardEntry>()
                snapshot.children.forEach { userSnapshot ->
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Anonymous"
                    val steps = userSnapshot.child("daily_history").child(todayDate).getValue(Int::class.java) ?: 0
                    val picUrl = userSnapshot.child("profilePicUrl").getValue(String::class.java) ?: ""
                    if (steps > 0) {
                        entries.add(LeaderboardEntry(name, steps, picUrl))
                    }
                }
                leaderboard = entries.sortedByDescending { it.steps }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Global Ranking", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            } else if (leaderboard.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Leaderboard is empty. Start walking!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                val top3 = leaderboard.take(3)
                val others = leaderboard.drop(3)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        PodiumSection(top3)
                        Spacer(Modifier.height(24.dp))
                        if (others.isNotEmpty()) {
                            Text(
                                "Full Leaderboard",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    itemsIndexed(others, key = { index, item -> item.name }) {
                        index, entry ->
                        LeaderboardItem(
                            rank = index + 4,
                            entry = entry,
                            modifier = Modifier.animateItemPlacement(tween(300))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(top3: List<LeaderboardEntry>) {
    val gold = top3.getOrNull(0)
    val silver = top3.getOrNull(1)
    val bronze = top3.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        bronze?.let {
            PodiumCard(
                entry = it,
                rank = 3,
                color = Color(0xFFB08D57),
                modifier = Modifier.weight(1f).offset(y = 20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        gold?.let {
            PodiumCard(
                entry = it,
                rank = 1,
                color = AmberAccent,
                modifier = Modifier.weight(1.2f).offset(y = (-10).dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        silver?.let {
            PodiumCard(
                entry = it,
                rank = 2,
                color = Color(0xFFC0C0C0),
                modifier = Modifier.weight(1f).offset(y = 10.dp)
            )
        }
    }
}

@Composable
fun PodiumCard(entry: LeaderboardEntry, rank: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(if (rank == 1) 90.dp else 75.dp),
                border = BorderStroke(3.dp, color)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(entry.profilePicUrl).crossfade(true).build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (rank == 1) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = "Crown",
                    tint = AmberAccent,
                    modifier = Modifier.size(32.dp).offset(y = (-16).dp)
                )
            }
        }
        Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        Text(
            String.format("%,d steps", entry.steps),
            fontWeight = FontWeight.Black,
            color = color,
            fontSize = if (rank == 1) 16.sp else 14.sp
        )
    }
}

@Composable
fun LeaderboardItem(rank: Int, entry: LeaderboardEntry, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$rank",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(40.dp)
            )
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(entry.profilePicUrl).crossfade(true).build(),
                contentDescription = "Profile Picture",
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(16.dp))

            Text(entry.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

            Text(
                String.format("%,d", entry.steps),
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.DirectionsRun, null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        }
    }
}
