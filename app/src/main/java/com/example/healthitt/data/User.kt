package com.example.healthitt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val dob: String = "2000-01-01",
    val weight: String = "70",
    val height: String = "5.8",
    val gender: String = "Male",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val profilePicUrl: String = "",
    val currentSteps: Int = 0,
    val calorieGoal: Int = 2000,
    val bmi: Double = 0.0,
    val appVersion: String = "1.0.0",
    val isDarkMode: Boolean = true,
    // Vault Fields
    val emergencyContact: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    // Dynamic Medications
    val medications: List<Medication> = emptyList()
)
