package com.example.usagestatisticsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String,
    val registrationDate: Date,
    val deviceId: String,
    val studyGroup: String? = null,
    val lastActiveDate: Date = Date()
)
