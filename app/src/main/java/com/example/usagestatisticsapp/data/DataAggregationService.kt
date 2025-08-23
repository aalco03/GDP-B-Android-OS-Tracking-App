package com.example.usagestatisticsapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class DataAggregationService(
    private val userUsageStatsRepository: UserUsageStatsRepository,
    private val masterUsageStatsRepository: MasterUsageStatsRepository
) {
    private val aggregationScope = CoroutineScope(Dispatchers.IO)
    
    fun aggregateUserData(userId: String, studyPeriod: String) {
        aggregationScope.launch {
            try {
                // Get all user usage stats for the period
                val fromDate = getPeriodStartDate(studyPeriod)
                val userStatsFlow = userUsageStatsRepository.getUsageStatsFromDate(userId, fromDate)
                val userStats = userStatsFlow.first() // Get the first value from the flow
                
                if (userStats.isNotEmpty()) {
                    // Group by app package name
                    val appGroups = userStats.groupBy { it.appPackageName }
                    
                    // Create aggregated data for each app
                    appGroups.forEach { (packageName, stats) ->
                        val appName = stats.firstOrNull()?.appName ?: packageName
                        val totalDuration = stats.sumOf { it.duration }
                        val averageDuration = if (stats.isNotEmpty()) totalDuration / stats.size else 0L
                        val totalSessions = stats.map { it.sessionId }.distinct().size
                        
                        val masterStats = MasterUsageStats(
                            appPackageName = packageName,
                            appName = appName,
                            totalUsers = 1, // Will be updated when aggregating across users
                            averageUsageTime = averageDuration,
                            totalUsageTime = totalDuration,
                            totalSessions = totalSessions,
                            studyPeriod = studyPeriod
                        )
                        
                        masterUsageStatsRepository.insertMasterUsageStats(masterStats)
                    }
                }
            } catch (e: Exception) {
                // Handle aggregation errors
                e.printStackTrace()
            }
        }
    }
    
    fun aggregateAllUsersData(studyPeriod: String) {
        aggregationScope.launch {
            try {
                // This would typically be called from a background service
                // that has access to all user databases
                // For now, we'll just update existing master stats
                
                val masterStatsFlow = masterUsageStatsRepository.getMasterUsageStatsByPeriod(studyPeriod)
                val masterStatsList = masterStatsFlow.first() // Get the first value from the flow
                
                // Update total users count and recalculate averages
                // This is a simplified version - in practice, you'd aggregate across all users
                masterStatsList.forEach { stats ->
                    // Update with aggregated data from all users
                    masterUsageStatsRepository.insertMasterUsageStats(stats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun getPeriodStartDate(studyPeriod: String): Date {
        val calendar = Calendar.getInstance()
        
        when (studyPeriod.lowercase()) {
            "week 1" -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            "month 1" -> calendar.add(Calendar.MONTH, -1)
            "month 3" -> calendar.add(Calendar.MONTH, -3)
            "month 6" -> calendar.add(Calendar.MONTH, -6)
            "year 1" -> calendar.add(Calendar.YEAR, -1)
            else -> calendar.add(Calendar.DAY_OF_YEAR, -7) // Default to last week
        }
        
        return calendar.time
    }
    
    fun getStudyPeriods(): List<String> {
        return listOf("Week 1", "Month 1", "Month 3", "Month 6", "Year 1")
    }
}
