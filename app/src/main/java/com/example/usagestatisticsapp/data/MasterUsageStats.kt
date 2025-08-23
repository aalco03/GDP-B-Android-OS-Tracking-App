package com.example.usagestatisticsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "master_usage_stats")
data class MasterUsageStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appPackageName: String,
    val appName: String,
    val totalUsers: Int,
    val averageUsageTime: Long, // Average duration in milliseconds
    val totalUsageTime: Long,   // Sum of all usage times
    val totalSessions: Int,
    val studyPeriod: String,    // e.g., "Week 1", "Month 1"
    val lastUpdated: Date = Date()
)
