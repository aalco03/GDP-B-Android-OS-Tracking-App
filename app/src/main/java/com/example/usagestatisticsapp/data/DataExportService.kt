package com.example.usagestatisticsapp.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataExportService(
    private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val userUsageStatsRepository: UserUsageStatsRepository,
    private val masterUsageStatsRepository: MasterUsageStatsRepository
) {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    data class ExportData(
        val exportDate: String,
        val userProfiles: List<UserProfile>,
        val userUsageStats: List<UserUsageStats>,
        val masterUsageStats: List<MasterUsageStats>,
        val summary: ExportSummary
    )
    
    data class ExportSummary(
        val totalUsers: Int,
        val totalUsageSessions: Int,
        val totalMasterRecords: Int,
        val dateRange: String,
        val exportFormat: String
    )
    
    suspend fun exportToJson(): Uri = withContext(Dispatchers.IO) {
        try {
            // Collect all data
            val userProfiles = userProfileRepository.getAllUserProfiles().first()
            val userUsageStats = userUsageStatsRepository.getAllUsageStats().first()
            val masterUsageStats = masterUsageStatsRepository.getAllMasterUsageStats().first()
            
            // Create export data object
            val exportData = ExportData(
                exportDate = dateFormat.format(Date()),
                userProfiles = userProfiles,
                userUsageStats = userUsageStats,
                masterUsageStats = masterUsageStats,
                summary = ExportSummary(
                    totalUsers = userProfiles.size,
                    totalUsageSessions = userUsageStats.size,
                    totalMasterRecords = masterUsageStats.size,
                    dateRange = getDateRange(userUsageStats),
                    exportFormat = "JSON"
                )
            )
            
            // Convert to JSON
            val jsonData = gson.toJson(exportData)
            
            // Write to file
            val fileName = "usage_stats_export_${dateFormat.format(Date())}.json"
            val file = File(context.cacheDir, fileName)
            FileWriter(file).use { writer ->
                writer.write(jsonData)
            }
            
            // Return file URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            throw DataExportException("Failed to export JSON data: ${e.message}")
        }
    }
    
    suspend fun exportToCsv(): Uri = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val csvFile = File(context.cacheDir, "usage_stats_export_$timestamp.csv")
            
            FileWriter(csvFile).use { writer ->
                // Write CSV header
                writer.write("Table,Field,Value\n")
                
                // Export user profiles
                val userProfiles = userProfileRepository.getAllUserProfiles().first()
                userProfiles.forEach { profile ->
                    writer.write("user_profile,userId,${profile.userId}\n")
                    writer.write("user_profile,registrationDate,${profile.registrationDate}\n")
                    writer.write("user_profile,deviceId,${profile.deviceId}\n")
                    writer.write("user_profile,studyGroup,${profile.studyGroup ?: ""}\n")
                    writer.write("user_profile,lastActiveDate,${profile.lastActiveDate}\n")
                }
                
                // Export user usage stats
                val userUsageStats = userUsageStatsRepository.getAllUsageStats().first()
                userUsageStats.forEach { stat ->
                    writer.write("user_usage_stats,id,${stat.id}\n")
                    writer.write("user_usage_stats,userId,${stat.userId}\n")
                    writer.write("user_usage_stats,sessionId,${stat.sessionId}\n")
                    writer.write("user_usage_stats,appPackageName,${stat.appPackageName}\n")
                    writer.write("user_usage_stats,appName,${stat.appName}\n")
                    writer.write("user_usage_stats,startTime,${stat.startTime}\n")
                    writer.write("user_usage_stats,endTime,${stat.endTime}\n")
                    writer.write("user_usage_stats,duration,${stat.duration}\n")
                    writer.write("user_usage_stats,interactionCount,${stat.interactionCount}\n")
                    writer.write("user_usage_stats,timestamp,${stat.timestamp}\n")
                    writer.write("user_usage_stats,isActive,${stat.isActive}\n")
                    writer.write("user_usage_stats,appCategory,${stat.appCategory ?: ""}\n")
                    writer.write("user_usage_stats,deviceOrientation,${stat.deviceOrientation}\n")
                    writer.write("user_usage_stats,batteryLevel,${stat.batteryLevel ?: ""}\n")
                    writer.write("user_usage_stats,networkType,${stat.networkType ?: ""}\n")
                }
                
                // Export master usage stats
                val masterUsageStats = masterUsageStatsRepository.getAllMasterUsageStats().first()
                masterUsageStats.forEach { stat ->
                    writer.write("master_usage_stats,id,${stat.id}\n")
                    writer.write("master_usage_stats,appPackageName,${stat.appPackageName}\n")
                    writer.write("master_usage_stats,appName,${stat.appName}\n")
                    writer.write("master_usage_stats,totalUsers,${stat.totalUsers}\n")
                    writer.write("master_usage_stats,averageUsageTime,${stat.averageUsageTime}\n")
                    writer.write("master_usage_stats,totalUsageTime,${stat.totalUsageTime}\n")
                    writer.write("master_usage_stats,totalSessions,${stat.totalSessions}\n")
                    writer.write("master_usage_stats,studyPeriod,${stat.studyPeriod}\n")
                    writer.write("master_usage_stats,lastUpdated,${stat.lastUpdated}\n")
                }
            }
            
            // Return file URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
        } catch (e: Exception) {
            throw DataExportException("Failed to export CSV data: ${e.message}")
        }
    }
    
    suspend fun exportDatabaseFile(): Uri = withContext(Dispatchers.IO) {
        try {
            // Get the database file
            val databaseFile = context.getDatabasePath("usage_stats_database")
            
            if (!databaseFile.exists()) {
                throw DataExportException("Database file does not exist")
            }
            
            // Create a copy in cache directory for sharing
            val timestamp = dateFormat.format(Date())
            val exportFile = File(context.cacheDir, "usage_stats_database_$timestamp.db")
            databaseFile.copyTo(exportFile, overwrite = true)
            
            // Return file URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
        } catch (e: Exception) {
            throw DataExportException("Failed to export database file: ${e.message}")
        }
    }
    
    suspend fun getExportSummary(): ExportSummary = withContext(Dispatchers.IO) {
        val userProfiles = userProfileRepository.getAllUserProfiles().first()
        val userUsageStats = userUsageStatsRepository.getAllUsageStats().first()
        val masterUsageStats = masterUsageStatsRepository.getAllMasterUsageStats().first()
        
        ExportSummary(
            totalUsers = userProfiles.size,
            totalUsageSessions = userUsageStats.size,
            totalMasterRecords = masterUsageStats.size,
            dateRange = getDateRange(userUsageStats),
            exportFormat = "Summary"
        )
    }
    
    private fun getDateRange(userUsageStats: List<UserUsageStats>): String {
        if (userUsageStats.isEmpty()) return "No data"
        
        val dates = userUsageStats.map { it.startTime }.sorted()
        val firstDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dates.first())
        val lastDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dates.last())
        
        return if (firstDate == lastDate) firstDate else "$firstDate to $lastDate"
    }
    
    class DataExportException(message: String) : Exception(message)
}
