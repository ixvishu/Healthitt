package com.example.healthitt.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@IgnoreExtraProperties
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) 
    @get:Exclude @set:Exclude var id: Int = 0,
    
    var name: String = "",
    var dob: String = "2000-01-01",
    var weight: String = "70",
    var height: String = "5.8",
    var gender: String = "Male",
    var email: String = "",
    var phone: String = "",
    var password: String = "",
    var profilePicUrl: String = "",
    var currentSteps: Int = 0,
    var calorieGoal: Int = 2000,
    var bmi: Double = 0.0,
    var appVersion: String = "1.0.0",
    var isDarkMode: Boolean = true,
    
    // Vault Fields
    var emergencyContact: String = "",
    var bloodGroup: String = "",
    var allergies: String = "",
    
    /**
     * Medications field for Firebase. 
     * Marked as @Ignore for Room because Room cannot handle Any?.
     */
    @Ignore
    var medications: Any? = null,

    /**
     * Medications field for Room.
     * Stored as a JSON string.
     * Marked as @Exclude for Firebase to avoid duplicate data.
     */
    @get:Exclude @set:Exclude
    var medicationsJson: String? = null
) {
    /**
     * Safely returns medications as a list.
     * Handles cases where medications might be stored in 'medications' (Firebase)
     * or 'medicationsJson' (Room).
     */
    @get:Exclude
    val medicationsList: List<Medication>
        get() {
            // First check medicationsJson (local/Room data)
            if (medicationsJson != null) {
                try {
                    val type = object : TypeToken<List<Medication>>() {}.type
                    return Gson().fromJson(medicationsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    // Fall through to medications
                }
            }

            // Then check medications (Firebase data)
            if (medications == null) return emptyList()
            
            if (medications is List<*>) {
                return (medications as List<*>).mapNotNull { item ->
                    if (item is Map<*, *>) {
                        mapToMedication(item as Map<String, Any>)
                    } else null
                }
            }
            
            if (medications is Map<*, *>) {
                return (medications as Map<*, *>).values.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        mapToMedication(item as Map<String, Any>)
                    } else null
                }
            }
            
            return emptyList()
        }

    private fun mapToMedication(map: Map<String, Any>): Medication {
        return Medication(
            id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
            name = map["name"] as? String ?: "",
            time = map["time"] as? String ?: "08:00",
            isEnabled = map["isEnabled"] as? Boolean ?: true
        )
    }
}
