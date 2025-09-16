package com.example.usagestatisticsapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "user_usage_stats",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["sessionId"]),
        Index(value = ["appPackageName"]),
        Index(value = ["startTime"]),
        Index(value = ["userId", "startTime"])
    ]
)
data class UserUsageStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val sessionId: String,
    val appPackageName: String,
    val appName: String,
    val startTime: Date,
    val endTime: Date,
    val duration: Long, // Duration in milliseconds
    val interactionCount: Int = 0,
    val timestamp: Date = Date(),
    val isActive: Boolean = true, // Whether this usage session is still active
    val appCategory: String? = null, // App category (e.g., "Social", "Productivity")
    val deviceOrientation: String = "portrait", // Device orientation during usage
    val batteryLevel: Int? = null, // Battery level when session started
    val networkType: String? = null, // Network type (WiFi, Mobile, etc.)
    val isSynced: Boolean = false // Whether this record has been synced to backend
)
