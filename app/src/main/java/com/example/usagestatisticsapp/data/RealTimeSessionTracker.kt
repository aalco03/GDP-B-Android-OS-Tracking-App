package com.example.usagestatisticsapp.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Real-time session tracker for monitoring app usage in Stanford HAI Study.
 * 
 * This class handles:
 * - Real-time app usage monitoring with 30-second intervals
 * - Session management with 10+ second minimum duration
 * - Metadata collection (device orientation, battery, network)
 * - Performance-optimized tracking to avoid UI lag
 * 
 * @param userUsageStatsRepository Repository for storing usage statistics
 * @param context Android context for accessing system services
 */
class RealTimeSessionTracker(
    private val userUsageStatsRepository: UserUsageStatsRepository,
    private val context: Context
) {
    private val trackerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
    // Real-time tracking state
    private val _currentAppUsage = MutableStateFlow<CurrentAppUsage?>(null)
    val currentAppUsage: StateFlow<CurrentAppUsage?> = _currentAppUsage.asStateFlow()
    
    // Active sessions tracking
    private val activeSessions = ConcurrentHashMap<String, ActiveSession>()
    private var isTracking = false
    
    // Configuration
    private val MIN_USAGE_DURATION = 10000L // 10 seconds in milliseconds
    private val TRACKING_INTERVAL = 30000L // Check every 30 seconds to reduce performance impact
    private val BATCH_UPDATE_INTERVAL = 60000L // Update database every 60 seconds
    
    data class CurrentAppUsage(
        val userId: String,
        val sessionId: String,
        val appPackageName: String,
        val appName: String,
        val startTime: Date,
        val currentDuration: Long,
        val isActive: Boolean
    )
    
    data class ActiveSession(
        val userId: String,
        val sessionId: String,
        val appPackageName: String,
        val appName: String,
        val startTime: Date,
        val usageId: Long
    )
    
    fun startTracking(userId: String) {
        if (isTracking) return
        
        isTracking = true
        val sessionId = generateSessionId()
        trackerScope.launch {
            while (isTracking) {
                try {
                    trackCurrentAppUsage(userId, sessionId)
                    delay(TRACKING_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun stopTracking() {
        isTracking = false
        trackerScope.launch {
            endAllActiveSessions()
        }
    }
    
    private suspend fun trackCurrentAppUsage(userId: String, sessionId: String) {
        val currentApp = getCurrentForegroundApp()
        if (currentApp == null) {
            endAllActiveSessions()
            _currentAppUsage.value = null
            return
        }
        
        val currentTime = Date()
        val currentAppUsage = _currentAppUsage.value
        
        if (currentAppUsage?.appPackageName == currentApp.packageName) {
            // Same app, update duration
            val updatedDuration = currentTime.time - currentAppUsage.startTime.time
            _currentAppUsage.value = currentAppUsage.copy(
                currentDuration = updatedDuration,
                isActive = true
            )
            
            // Check if we should log this usage (10+ seconds)
            if (updatedDuration >= MIN_USAGE_DURATION && !activeSessions.containsKey(currentApp.packageName)) {
                startNewAppSession(userId, sessionId, currentApp, currentTime)
            }
        } else {
            // Different app, end previous session and start new one
            endAppSession(currentAppUsage?.appPackageName)
            _currentAppUsage.value = CurrentAppUsage(
                userId = userId,
                sessionId = sessionId,
                appPackageName = currentApp.packageName,
                appName = currentApp.appName,
                startTime = currentTime,
                currentDuration = 0,
                isActive = true
            )
        }
    }
    
    private suspend fun startNewAppSession(
        userId: String, 
        sessionId: String, 
        appInfo: AppInfo, 
        startTime: Date
    ) {
        val usageStats = UserUsageStats(
            userId = userId,
            sessionId = sessionId,
            appPackageName = appInfo.packageName,
            appName = appInfo.appName,
            startTime = startTime,
            endTime = startTime, // Will be updated when session ends
            duration = 0,
            isActive = true,
            appCategory = getAppCategory(appInfo.packageName),
            deviceOrientation = getDeviceOrientation(),
            batteryLevel = getBatteryLevel(),
            networkType = getNetworkType()
        )
        
        val usageId = userUsageStatsRepository.insertUsageStatsAndGetId(usageStats)
        
        activeSessions[appInfo.packageName] = ActiveSession(
            userId = userId,
            sessionId = sessionId,
            appPackageName = appInfo.packageName,
            appName = appInfo.appName,
            startTime = startTime,
            usageId = usageId
        )
    }
    
    private suspend fun endAppSession(appPackageName: String?) {
        appPackageName?.let { packageName ->
            val activeSession = activeSessions[packageName]
            if (activeSession != null) {
                val endTime = Date()
                val duration = endTime.time - activeSession.startTime.time
                
                userUsageStatsRepository.endUsageSession(
                    activeSession.usageId,
                    endTime,
                    duration
                )
                
                activeSessions.remove(packageName)
            }
        }
    }
    
    private suspend fun endAllActiveSessions() {
        activeSessions.keys.forEach { packageName ->
            endAppSession(packageName)
        }
        activeSessions.clear()
    }
    
    private fun getCurrentForegroundApp(): AppInfo? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60000 // Look back 1 minute
        
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastPackageName: String? = null
        var lastTimeStamp: Long = 0
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackageName = event.packageName
                lastTimeStamp = event.timeStamp
            }
        }
        
        return lastPackageName?.let { packageName ->
            AppInfo(
                packageName = packageName,
                appName = getAppName(packageName)
            )
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    private fun getAppCategory(packageName: String): String? {
        // Simple categorization - can be enhanced
        return when {
            packageName.contains("social") || packageName.contains("facebook") || 
            packageName.contains("instagram") || packageName.contains("twitter") -> "Social"
            packageName.contains("work") || packageName.contains("office") || 
            packageName.contains("productivity") -> "Productivity"
            packageName.contains("game") || packageName.contains("play") -> "Gaming"
            packageName.contains("browser") || packageName.contains("chrome") || 
            packageName.contains("firefox") -> "Browser"
            else -> null
        }
    }
    
    private fun getDeviceOrientation(): String {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val rotation = windowManager.defaultDisplay.rotation
            when (rotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> "portrait"
                Surface.ROTATION_90, Surface.ROTATION_270 -> "landscape"
                else -> "portrait"
            }
        } catch (e: Exception) {
            "portrait"
        }
    }
    
    private fun getBatteryLevel(): Int? {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getNetworkType(): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            null
        }
    }
    
    data class AppInfo(
        val packageName: String,
        val appName: String
    )
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    fun onDestroy() {
        stopTracking()
        trackerScope.cancel()
    }
}
