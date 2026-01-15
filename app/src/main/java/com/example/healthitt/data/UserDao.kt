package com.example.healthitt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email OR phone = :phone LIMIT 1")
    suspend fun getUserByEmailOrPhone(email: String, phone: String): User?
}
