package com.example.healthitt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val profilePicUrl: String = "",
    val currentSteps: Int = 0
)
