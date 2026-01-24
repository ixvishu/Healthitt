package com.example.healthitt.ui.todo

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.data.Todo
import com.example.healthitt.ui.theme.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(userEmail: String, onBack: () -> Unit) {
    val userEmailKey = remember { userEmail.replace(".", "_") }
    val database = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference
    val todoRef = database.child("users").child(userEmailKey).child("todos")

    var todos by remember { mutableStateOf<List<Todo>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        todoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Todo>()
                snapshot.children.forEach {
                    it.getValue(Todo::class.java)?.let { todo -> list.add(todo) }
                }
                todos = list.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        containerColor = DeepSlate,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Protocol Tasks", fontWeight = FontWeight.Black, color = PureWhite, fontSize = 20.sp)
                        Text("Optimize your daily performance", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Rounded.Add, "Add Task", tint = DeepSlate, modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            
            Spacer(Modifier.height(12.dp))
            
            // Progress Header
            val completedCount = todos.count { it.isCompleted }
            val totalCount = todos.size
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MutedSlate,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                        CircularProgressIndicator(progress = { 1f }, color = PureWhite.copy(0.05f), strokeWidth = 6.dp)
                        CircularProgressIndicator(progress = { progress }, color = EmeraldPrimary, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
                        Text("${(progress * 100).toInt()}%", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Task Completion", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$completedCount of $totalCount protocols secured", color = PureWhite.copy(0.5f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Analytics Graph
            ProtocolAnalyticsGraph(todos)

            Spacer(Modifier.height(24.dp))

            if (todos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.TaskAlt, null, tint = PureWhite.copy(0.1f), modifier = Modifier.size(100.dp))
                        Text("No active protocols.", color = PureWhite.copy(0.3f), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(todos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = { todoRef.child(todo.id).child("completed").setValue(!todo.isCompleted) },
                            onDelete = { todoRef.child(todo.id).removeValue() }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { task, category, priority ->
                val newTodo = Todo(task = task, category = category, priority = priority)
                todoRef.child(newTodo.id).setValue(newTodo)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProtocolAnalyticsGraph(todos: List<Todo>) {
    val categories = listOf("Health", "Workout", "Diet", "General")
    val stats = categories.map { cat ->
        val total = todos.count { it.category == cat }
        val completed = todos.count { it.category == cat && it.isCompleted }
        Pair(total, completed)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(24.dp),
        color = MutedSlate.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("EFFICIENCY MATRIX", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                stats.forEachIndexed { index, pair ->
                    val total = pair.first
                    val completed = pair.second
                    val progress = if (total > 0) completed.toFloat() / total else 0f
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).width(12.dp), contentAlignment = Alignment.BottomCenter) {
                            // Background Bar
                            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(PureWhite.copy(0.05f)))
                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress.coerceAtLeast(0.05f))
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(SkyAccent, EmeraldPrimary)
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(categories[index].take(1).uppercase(), color = PureWhite.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    val categoryColor = when(todo.category) {
        "Health" -> EmeraldPrimary
        "Workout" -> SkyAccent
        "Diet" -> AmberAccent
        else -> PureWhite
    }

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MutedSlate,
        border = BorderStroke(1.dp, if(todo.isCompleted) EmeraldPrimary.copy(0.2f) else PureWhite.copy(0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (todo.isCompleted) EmeraldPrimary else PureWhite.copy(0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = todo.task,
                    color = if (todo.isCompleted) PureWhite.copy(0.4f) else PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(categoryColor))
                    Spacer(Modifier.width(6.dp))
                    Text(todo.category, color = categoryColor.copy(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    
                    if (todo.priority > 1) {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Rounded.PriorityHigh, null, tint = RoseAccent, modifier = Modifier.size(12.dp))
                        Text("HIGH PRIORITY", color = RoseAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = RoseAccent.copy(0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddTodoDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var task by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var priority by remember { mutableIntStateOf(1) }
    val categories = listOf("General", "Health", "Workout", "Diet")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlate,
        shape = RoundedCornerShape(28.dp),
        title = { Text("New Protocol", color = PureWhite, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    label = { Text("Task Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = PureWhite.copy(0.1f)
                    )
                )

                Text("Category", color = PureWhite.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            onClick = { category = cat },
                            modifier = Modifier.weight(1f).height(40.dp),
                            color = if (isSelected) EmeraldPrimary.copy(0.2f) else PureWhite.copy(0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(cat, color = if (isSelected) EmeraldPrimary else PureWhite.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("High Priority", color = PureWhite, modifier = Modifier.weight(1f))
                    Switch(
                        checked = priority == 3,
                        onCheckedChange = { priority = if(it) 3 else 1 },
                        colors = SwitchDefaults.colors(checkedThumbColor = RoseAccent)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if(task.isNotBlank()) onAdd(task, category, priority) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = task.isNotBlank()
            ) {
                Text("Initialize", color = DeepSlate, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abort", color = PureWhite.copy(0.5f))
            }
        }
    )
}
