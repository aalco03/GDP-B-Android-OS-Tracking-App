package com.example.usagestatisticsapp.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.usagestatisticsapp.data.UserUsageStats
import com.example.usagestatisticsapp.data.UserUsageStatsRepository
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * Service for synchronizing local usage data with the GDP-B web dashboard
 */
class SyncService(
    private val context: Context,
    private val repository: UserUsageStatsRepository,
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
            Log.i(TAG, "Study ID set successfully: $studyId")
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
        val studyId = getStoredStudyId()
        if (studyId == null) {
            return Result.failure(Exception("No Study ID set. Please enter your Study ID first."))
        }
        
        return try {
            // Get usage data from local database
            val allUsageStats = repository.getUserUsageStats(userId).first()
            
            // Filter data for sync
            val filteredStats = DataMapper.filterForSync(
                allUsageStats, 
                includeActive, 
                maxAgeHours
            )
            
            if (filteredStats.isEmpty()) {
                Log.i(TAG, "No data to sync")
                return Result.success(0)
            }
            
            // Convert to API format with Study ID as tenant
            val usageDataRequests = DataMapper.toUsageDataRequestList(filteredStats, studyId)
            
            // Send to backend without authentication
            val result = apiRepository.submitUsageDataAnonymous(usageDataRequests)
            
            result.onSuccess { response ->
                // Update last sync timestamp
                prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()
                Log.i(TAG, "Successfully synced ${response.size} usage records for Study ID: $studyId")
            }.onFailure { error ->
                Log.e(TAG, "Sync failed: ${error.message}", error)
            }
            
            result.map { it.size }
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check backend connectivity
     */
    suspend fun checkConnectivity(): Result<Boolean> {
        return try {
            val result = apiRepository.healthCheck()
            result.map { it.status == "UP" }
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
