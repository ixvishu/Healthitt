package com.example.healthitt.data

data class Todo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val task: String = "",
    val isCompleted: Boolean = false,
    val category: String = "General", // General, Health, Workout, Diet
    val priority: Int = 1, // 1: Low, 2: Medium, 3: High
    val timestamp: Long = System.currentTimeMillis()
)
