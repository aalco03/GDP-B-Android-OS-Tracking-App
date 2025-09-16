package com.example.usagestatisticsapp.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.usagestatisticsapp.data.UserUsageStats
import com.example.usagestatisticsapp.data.UserUsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Service for synchronizing local usage data with the GDP-B web dashboard
 */
class SyncService(
    private val context: Context,
    private val repository: UserUsageStatsRepository? = null,
    private val apiRepository: ApiRepository = ApiRepository()
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("gdpb_sync", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SyncService"
        private const val PREF_STUDY_ID = "study_id"
        private const val PREF_LAST_SYNC = "last_sync_timestamp"
        private const val PREF_AUTO_SYNC = "auto_sync_enabled"
    }
    
    /**
     * Check if user has entered Study ID
     */
    fun hasStudyId(): Boolean {
        return getStoredStudyId() != null
    }
    
    /**
     * Get stored Study ID
     */
    fun getStoredStudyId(): String? {
        return prefs.getString(PREF_STUDY_ID, null)
    }
    
    /**
     * Set Study ID for the user
     */
    fun setStudyId(studyId: String): Result<Unit> {
        return try {
            prefs.edit().putString(PREF_STUDY_ID, studyId.trim()).apply()
            Log.i(TAG, "Study ID set successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Study ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear Study ID and reset app
     */
    fun clearStudyId() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Study ID cleared")
    }
    
    /**
     * Sync usage data to backend
     */
    suspend fun syncUsageData(
        userId: String = "default_user",
        maxAgeHours: Int = 24,
        includeActive: Boolean = false
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val studyId = getStoredStudyId()
                if (studyId == null) {
                    Result.failure(Exception("Study ID not set. Please enter your Study ID first."))
                } else if (repository == null) {
                    // Repository not available - this should not happen in production
                    Log.e(TAG, "Repository is null - cannot sync real usage data")
                    Result.failure(Exception("Repository not initialized - cannot sync usage data"))
                } else {
                    // Get all usage data from repository with proper coroutine context
                    try {
                        // Get only unsynced records to avoid duplicates
                        val unsyncedData = repository.getUnsyncedUsageStats("default_user").first()
                        
                        Log.d(TAG, "🔍 SYNC DEBUG: Repository returned ${unsyncedData.size} unsynced usage records")
                        unsyncedData.forEach { data ->
                            Log.d(TAG, "📱 Unsynced: ${data.appName} (${data.appPackageName}) - Duration: ${data.duration}ms (${data.duration/1000.0}s) - ID: ${data.id} - isSynced: ${data.isSynced}")
                            Log.d(TAG, "   ⏰ Times: Start=${data.startTime}, End=${data.endTime}")
                            Log.d(TAG, "   🔄 Status: isActive=${data.isActive}, interactionCount=${data.interactionCount}")
                        }
                        
                        if (unsyncedData.isEmpty()) {
                            Log.i(TAG, "No unsynced usage data found - all data is already synced")
                            Result.success(0)
                        } else {
                            // Convert to API format and submit
                            val apiData = unsyncedData.map { DataMapper.toUsageDataRequest(it, studyId) }
                            
                            val result = apiRepository.submitUsageDataWithStudyId(studyId, apiData)
                            result.fold(
                                onSuccess = { response ->
                                    // CRITICAL FIX: Mark records as synced first, then delete them to prevent duplicates
                                    val recordIds = unsyncedData.map { it.id }
                                    Log.d(TAG, "Marking ${recordIds.size} records as synced: $recordIds")
                                    repository.markRecordsAsSynced(recordIds)
                                    
                                    // Now delete the synced records
                                    repository.deleteSyncedRecords("default_user")
                                    
                                    // Update last sync timestamp
                                    prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()
                                    Log.d(TAG, "Sync successful for Study ID $studyId: ${response.size} records marked as synced and deleted from local DB")
                                    Result.success(response.size)
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Sync failed for Study ID: $studyId", error)
                                    Result.failure(error)
                                }
                            )
                        }
                    } catch (flowException: Exception) {
                        Log.e(TAG, "Repository flow error - NOT falling back to test data", flowException)
                        Result.failure(flowException)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create test usage data for sync testing
     */
    private fun createTestUsageData(studyId: String): UsageDataRequest {
        val now = Date()
        val cal = Calendar.getInstance().apply { time = now }
        val timestampArray = arrayOf(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
        
        return UsageDataRequest(
            tenantId = studyId,
            userId = null, // Null for participant data - backend will assign participantId
            deviceId = "test-device-001",
            appPackageName = "com.android.chrome",
            appName = "Chrome",
            category = "PRODUCTIVITY",
            usageTimeMs = 60000L, // 1 minute
            timestamp = timestampArray,
            lastTimeUsed = timestampArray,
            launchCount = 1,
            totalTimeInForeground = 60000L,
            productivityScore = 0.8,
            economicValue = 10.0
        )
    }
    
    /**
     * Check backend connectivity
     */
    suspend fun checkConnectivity(): Result<Boolean> {
        return try {
            val result = apiRepository.healthCheck()
            result.fold(
                onSuccess = { healthResponse ->
                    val isHealthy = healthResponse.status == "UP" || healthResponse.status == "healthy"
                    Log.d(TAG, "Health check result: ${healthResponse.status}, isHealthy: $isHealthy")
                    Result.success(isHealthy)
                },
                onFailure = { error ->
                    Log.e(TAG, "Health check failed", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Connectivity check failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get sync status information
     */
    fun getSyncStatus(): SyncStatus {
        return SyncStatus(
            hasStudyId = hasStudyId(),
            studyId = getStoredStudyId(),
            lastSyncTime = prefs.getLong(PREF_LAST_SYNC, 0L).let { 
                if (it > 0) Date(it) else null 
            },
            autoSyncEnabled = prefs.getBoolean(PREF_AUTO_SYNC, false)
        )
    }
    
    /**
     * Enable/disable auto sync
     */
    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_SYNC, enabled).apply()
        Log.i(TAG, "Auto sync ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Perform full sync (all available data)
     */
    suspend fun performFullSync(userId: String = "default_user"): Result<Int> {
        return syncUsageData(userId, maxAgeHours = 24 * 7, includeActive = true) // Last 7 days
    }
}

/**
 * Data class for sync status information
 */
data class SyncStatus(
    val hasStudyId: Boolean,
    val studyId: String?,
    val lastSyncTime: Date?,
    val autoSyncEnabled: Boolean
)
