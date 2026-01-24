package com.example.healthitt.data

data class Medication(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val time: String = "08:00", // Format HH:mm
    val isEnabled: Boolean = true
)
