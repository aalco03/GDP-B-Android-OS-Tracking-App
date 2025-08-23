package com.example.usagestatisticsapp.data

import kotlinx.coroutines.flow.Flow

class MasterUsageStatsRepository(private val masterUsageStatsDao: MasterUsageStatsDao) {
    
    fun getAllMasterUsageStats(): Flow<List<MasterUsageStats>> {
        return masterUsageStatsDao.getAllMasterUsageStats()
    }
    
    fun getMasterUsageStatsByPeriod(studyPeriod: String): Flow<List<MasterUsageStats>> {
        return masterUsageStatsDao.getMasterUsageStatsByPeriod(studyPeriod)
    }
    
    fun getMasterUsageStatsByApp(packageName: String): Flow<List<MasterUsageStats>> {
        return masterUsageStatsDao.getMasterUsageStatsByApp(packageName)
    }
    
    suspend fun insertMasterUsageStats(masterUsageStats: MasterUsageStats) {
        masterUsageStatsDao.insertMasterUsageStats(masterUsageStats)
    }
    
    suspend fun insertMasterUsageStatsList(masterUsageStatsList: List<MasterUsageStats>) {
        masterUsageStatsDao.insertMasterUsageStatsList(masterUsageStatsList)
    }
    
    suspend fun deleteMasterUsageStatsByPeriod(studyPeriod: String) {
        masterUsageStatsDao.deleteMasterUsageStatsByPeriod(studyPeriod)
    }
    
    suspend fun deleteAllMasterUsageStats() {
        masterUsageStatsDao.deleteAllMasterUsageStats()
    }
}
