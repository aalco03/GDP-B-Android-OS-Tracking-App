package com.example.usagestatisticsapp.network

import android.os.Build
import com.example.usagestatisticsapp.data.UserUsageStats
import java.util.*

/**
 * Utility class for mapping between local UserUsageStats and API UsageDataRequest
 */
object DataMapper {
    
    /**
     * Convert UserUsageStats to UsageDataRequest for API submission
     */
    fun toUsageDataRequest(userUsageStats: UserUsageStats, studyId: String): UsageDataRequest {
        val startCal = Calendar.getInstance().apply { time = userUsageStats.startTime }
        val endCal = Calendar.getInstance().apply { time = userUsageStats.endTime }
        
        val timestampArray = arrayOf(
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            startCal.get(Calendar.DAY_OF_MONTH),
            startCal.get(Calendar.HOUR_OF_DAY),
            startCal.get(Calendar.MINUTE),
            startCal.get(Calendar.SECOND)
        )
        
        val lastUsedArray = arrayOf(
            endCal.get(Calendar.YEAR),
            endCal.get(Calendar.MONTH) + 1,
            endCal.get(Calendar.DAY_OF_MONTH),
            endCal.get(Calendar.HOUR_OF_DAY),
            endCal.get(Calendar.MINUTE),
            endCal.get(Calendar.SECOND)
        )
        
        return UsageDataRequest(
            tenantId = studyId, // Use Study ID as tenant identifier
            userId = 1L, // Required for anonymous users
            deviceId = getDeviceId(),
            appPackageName = userUsageStats.appPackageName,
            appName = userUsageStats.appName,
            category = userUsageStats.appCategory,
            usageTimeMs = userUsageStats.duration,
            timestamp = timestampArray,
            lastTimeUsed = lastUsedArray,
            firstTimeStamp = timestampArray,
            launchCount = userUsageStats.interactionCount,
            totalTimeInForeground = userUsageStats.duration,
            sessionId = userUsageStats.sessionId,
            interactionType = if (userUsageStats.isActive) "active" else "completed",
            screenTimeMinutes = userUsageStats.duration / 60000.0, // Convert ms to minutes
            productivityScore = calculateProductivityScore(userUsageStats),
            economicValue = calculateEconomicValue(userUsageStats)
        )
    }
    
    /**
     * Convert list of UserUsageStats to list of UsageDataRequest
     */
    fun toUsageDataRequestList(userUsageStatsList: List<UserUsageStats>, studyId: String): List<UsageDataRequest> {
        return userUsageStatsList.map { toUsageDataRequest(it, studyId) }
    }
    
    /**
     * Get unique device identifier
     */
    private fun getDeviceId(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}".replace(" ", "_")
    }
    
    /**
     * Calculate productivity score based on app category and usage patterns
     */
    private fun calculateProductivityScore(userUsageStats: UserUsageStats): Double {
        val category = userUsageStats.appCategory?.lowercase() ?: "unknown"
        val durationMinutes = userUsageStats.duration / 60000.0
        
        return when {
            category.contains("productivity") || category.contains("work") -> {
                // Higher score for productive apps, with diminishing returns after 2 hours
                minOf(1.0, durationMinutes / 120.0)
            }
            category.contains("social") || category.contains("entertainment") -> {
                // Lower score for entertainment, negative after 1 hour
                maxOf(-0.5, 0.3 - (durationMinutes / 60.0) * 0.1)
            }
            category.contains("education") || category.contains("news") -> {
                // Moderate positive score for educational content
                minOf(0.8, durationMinutes / 90.0)
            }
            else -> {
                // Neutral score for unknown categories
                0.0
            }
        }
    }
    
    /**
     * Calculate economic value based on app usage and productivity
     */
    private fun calculateEconomicValue(userUsageStats: UserUsageStats): Double {
        val productivityScore = calculateProductivityScore(userUsageStats)
        val durationHours = userUsageStats.duration / 3600000.0
        
        // Simplified economic value calculation
        // Positive productivity contributes to economic value, negative detracts
        return productivityScore * durationHours * 25.0 // Assuming $25/hour baseline
    }
    
    /**
     * Filter usage stats for sync (e.g., only completed sessions, recent data)
     */
    fun filterForSync(
        userUsageStatsList: List<UserUsageStats>,
        includeActive: Boolean = false,
        maxAgeHours: Int = 24
    ): List<UserUsageStats> {
        val cutoffTime = Date(System.currentTimeMillis() - (maxAgeHours * 3600000L))
        
        return userUsageStatsList.filter { stats ->
            // Filter by age
            stats.timestamp.after(cutoffTime) &&
            // Filter by active status if needed
            (includeActive || !stats.isActive)
        }
    }
}
