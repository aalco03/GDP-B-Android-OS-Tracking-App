package com.example.usagestatisticsapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserUsageStatsDao {
    // Basic queries
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId ORDER BY startTime DESC")
    fun getUserUsageStats(userId: String): Flow<List<UserUsageStats>>
    
    // Get unsynced usage statistics (for sync operations) - exclude zero-duration active sessions
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND isSynced = 0 AND (duration > 0 OR isActive = 0) ORDER BY startTime DESC")
    fun getUnsyncedUsageStats(userId: String): Flow<List<UserUsageStats>>
    
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND sessionId = :sessionId ORDER BY startTime DESC")
    fun getUsageStatsBySession(userId: String, sessionId: String): Flow<List<UserUsageStats>>
    
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND appPackageName = :packageName ORDER BY startTime DESC")
    fun getUsageStatsByApp(userId: String, packageName: String): Flow<List<UserUsageStats>>
    
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND startTime >= :fromDate ORDER BY startTime DESC")
    fun getUsageStatsFromDate(userId: String, fromDate: Date): Flow<List<UserUsageStats>>
    
    // Real-time session tracking
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun getCurrentActiveUsage(userId: String): Flow<UserUsageStats?>
    
    @Query("SELECT * FROM user_usage_stats WHERE userId = :userId AND sessionId = :sessionId AND isActive = 1 ORDER BY startTime DESC")
    fun getActiveUsageStatsBySession(userId: String, sessionId: String): Flow<List<UserUsageStats>>
    
    // Detailed analytics queries
    @Query("""
        SELECT appPackageName, appName, 
               SUM(duration) as totalDuration, 
               COUNT(*) as sessionCount,
               AVG(duration) as averageDuration,
               MIN(startTime) as firstUsed,
               MAX(endTime) as lastUsed
        FROM user_usage_stats 
        WHERE userId = :userId 
        GROUP BY appPackageName 
        ORDER BY totalDuration DESC
    """)
    fun getDetailedAppUsageSummary(userId: String): Flow<List<DetailedAppUsageSummary>>
    
    @Query("""
        SELECT appPackageName, appName, 
               SUM(duration) as totalDuration, 
               COUNT(*) as sessionCount
        FROM user_usage_stats 
        WHERE userId = :userId AND startTime >= :fromDate
        GROUP BY appPackageName 
        ORDER BY totalDuration DESC
    """)
    fun getAppUsageSummaryFromDate(userId: String, fromDate: Date): Flow<List<AppUsageSummary>>
    
    // Session-based analytics
    @Query("""
        SELECT sessionId, 
               COUNT(*) as appCount,
               SUM(duration) as totalSessionDuration,
               MIN(startTime) as sessionStart,
               MAX(endTime) as sessionEnd
        FROM user_usage_stats 
        WHERE userId = :userId 
        GROUP BY sessionId 
        ORDER BY sessionStart DESC
    """)
    fun getSessionSummary(userId: String): Flow<List<SessionSummary>>
    
    // Time-based analytics
    @Query("""
        SELECT strftime('%H', datetime(startTime/1000, 'unixepoch')) as hour,
               COUNT(*) as usageCount,
               SUM(duration) as totalDuration
        FROM user_usage_stats 
        WHERE userId = :userId AND startTime >= :fromDate
        GROUP BY hour 
        ORDER BY hour
    """)
    fun getHourlyUsagePattern(userId: String, fromDate: Date): Flow<List<HourlyUsagePattern>>
    
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(usageStats: UserUsageStats)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStatsList(usageStatsList: List<UserUsageStats>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStatsAndGetId(usageStats: UserUsageStats): Long
    
    // Update operations for real-time tracking
    @Query("UPDATE user_usage_stats SET endTime = :endTime, duration = :duration, isActive = 0 WHERE id = :usageId")
    suspend fun endUsageSession(usageId: Long, endTime: Date, duration: Long)
    
    @Query("UPDATE user_usage_stats SET isActive = 0 WHERE userId = :userId AND sessionId = :sessionId AND isActive = 1")
    suspend fun endAllActiveSessionsForUser(userId: String, sessionId: String)
    
    // Mark records as synced
    @Query("UPDATE user_usage_stats SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markRecordsAsSynced(ids: List<Long>)
    
    // Delete synced records
    @Query("DELETE FROM user_usage_stats WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteSyncedRecords(userId: String)
    
    // Count synced records for debugging
    @Query("SELECT COUNT(*) FROM user_usage_stats WHERE userId = :userId AND isSynced = 1")
    suspend fun getSyncedRecordsCount(userId: String): Int
    
    // Cleanup operations
    @Query("DELETE FROM user_usage_stats WHERE userId = :userId AND startTime < :beforeDate")
    suspend fun deleteOldUsageStats(userId: String, beforeDate: Date)
    
    @Query("DELETE FROM user_usage_stats WHERE userId = :userId")
    suspend fun deleteAllUserUsageStats(userId: String)
    
    // Performance queries
    @Query("SELECT COUNT(*) FROM user_usage_stats WHERE userId = :userId")
    suspend fun getUserUsageStatsCount(userId: String): Int
    
    // Export queries
    @Query("SELECT * FROM user_usage_stats ORDER BY startTime DESC")
    fun getAllUsageStats(): Flow<List<UserUsageStats>>
}

// Enhanced data classes for detailed analytics
data class DetailedAppUsageSummary(
    val appPackageName: String,
    val appName: String,
    val totalDuration: Long,
    val sessionCount: Int,
    val averageDuration: Double,
    val firstUsed: Date,
    val lastUsed: Date
)

data class AppUsageSummary(
    val appPackageName: String,
    val appName: String,
    val totalDuration: Long,
    val sessionCount: Int
)

data class SessionSummary(
    val sessionId: String,
    val appCount: Int,
    val totalSessionDuration: Long,
    val sessionStart: Date,
    val sessionEnd: Date
)

data class HourlyUsagePattern(
    val hour: String,
    val usageCount: Int,
    val totalDuration: Long
)
