package com.example.usagestatisticsapp.data

import kotlinx.coroutines.flow.Flow
import java.util.Date

class UserUsageStatsRepository(private val userUsageStatsDao: UserUsageStatsDao) {
    
    fun getUserUsageStats(userId: String): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getUserUsageStats(userId)
    }
    
    fun getUsageStatsBySession(userId: String, sessionId: String): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getUsageStatsBySession(userId, sessionId)
    }
    
    fun getUsageStatsByApp(userId: String, packageName: String): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getUsageStatsByApp(userId, packageName)
    }
    
    fun getUsageStatsFromDate(userId: String, fromDate: Date): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getUsageStatsFromDate(userId, fromDate)
    }
    
    // Real-time tracking methods
    fun getCurrentActiveUsage(userId: String): Flow<UserUsageStats?> {
        return userUsageStatsDao.getCurrentActiveUsage(userId)
    }
    
    fun getActiveUsageStatsBySession(userId: String, sessionId: String): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getActiveUsageStatsBySession(userId, sessionId)
    }
    
    // Analytics methods
    fun getDetailedAppUsageSummary(userId: String): Flow<List<DetailedAppUsageSummary>> {
        return userUsageStatsDao.getDetailedAppUsageSummary(userId)
    }
    
    fun getAppUsageSummaryFromDate(userId: String, fromDate: Date): Flow<List<AppUsageSummary>> {
        return userUsageStatsDao.getAppUsageSummaryFromDate(userId, fromDate)
    }
    
    fun getSessionSummary(userId: String): Flow<List<SessionSummary>> {
        return userUsageStatsDao.getSessionSummary(userId)
    }
    
    fun getHourlyUsagePattern(userId: String, fromDate: Date): Flow<List<HourlyUsagePattern>> {
        return userUsageStatsDao.getHourlyUsagePattern(userId, fromDate)
    }
    
    // Insert operations
    suspend fun insertUsageStats(usageStats: UserUsageStats) {
        userUsageStatsDao.insertUsageStats(usageStats)
    }
    
    suspend fun insertUsageStatsList(usageStatsList: List<UserUsageStats>) {
        userUsageStatsDao.insertUsageStatsList(usageStatsList)
    }
    
    // Real-time tracking operations
    suspend fun insertUsageStatsAndGetId(usageStats: UserUsageStats): Long {
        return userUsageStatsDao.insertUsageStatsAndGetId(usageStats)
    }
    
    suspend fun endUsageSession(usageId: Long, endTime: Date, duration: Long) {
        userUsageStatsDao.endUsageSession(usageId, endTime, duration)
    }
    
    suspend fun endAllActiveSessionsForUser(userId: String, sessionId: String) {
        userUsageStatsDao.endAllActiveSessionsForUser(userId, sessionId)
    }
    
    // Cleanup operations
    suspend fun deleteOldUsageStats(userId: String, beforeDate: Date) {
        userUsageStatsDao.deleteOldUsageStats(userId, beforeDate)
    }
    
    suspend fun deleteAllUserUsageStats(userId: String) {
        userUsageStatsDao.deleteAllUserUsageStats(userId)
    }
    
    // Performance queries
    suspend fun getUserUsageStatsCount(userId: String): Int {
        return userUsageStatsDao.getUserUsageStatsCount(userId)
    }
    
    // Export methods
    suspend fun getAllUsageStats(): Flow<List<UserUsageStats>> {
        return userUsageStatsDao.getAllUsageStats()
    }
}
