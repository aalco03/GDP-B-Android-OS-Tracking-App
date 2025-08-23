package com.example.usagestatisticsapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterUsageStatsDao {
    @Query("SELECT * FROM master_usage_stats ORDER BY totalUsageTime DESC")
    fun getAllMasterUsageStats(): Flow<List<MasterUsageStats>>
    
    @Query("SELECT * FROM master_usage_stats WHERE studyPeriod = :studyPeriod ORDER BY totalUsageTime DESC")
    fun getMasterUsageStatsByPeriod(studyPeriod: String): Flow<List<MasterUsageStats>>
    
    @Query("SELECT * FROM master_usage_stats WHERE appPackageName = :packageName ORDER BY lastUpdated DESC")
    fun getMasterUsageStatsByApp(packageName: String): Flow<List<MasterUsageStats>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterUsageStats(masterUsageStats: MasterUsageStats)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterUsageStatsList(masterUsageStatsList: List<MasterUsageStats>)
    
    @Query("DELETE FROM master_usage_stats WHERE studyPeriod = :studyPeriod")
    suspend fun deleteMasterUsageStatsByPeriod(studyPeriod: String)
    
    @Query("DELETE FROM master_usage_stats")
    suspend fun deleteAllMasterUsageStats()
}
