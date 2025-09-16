package com.example.usagestatisticsapp.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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
import android.app.usage.UsageStats

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
    private val TRACKING_INTERVAL = 10000L // Check every 10 seconds to reduce battery impact
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
        val sessionId = "session_${System.currentTimeMillis()}_${userId.hashCode().toString(16)}"
        
        android.util.Log.i("RealTimeSessionTracker", "🚀 Starting tracking for user: $userId with session: $sessionId")
        
        trackerScope.launch {
            var consecutiveErrors = 0
            var lastHeartbeat = System.currentTimeMillis()
            
            while (isTracking) {
                try {
                    val currentTime = System.currentTimeMillis()
                    
                    // Heartbeat logging every 30 seconds to detect if tracking is alive
                    if (currentTime - lastHeartbeat > 30000) {
                        android.util.Log.i("RealTimeSessionTracker", "💓 Tracking heartbeat - still alive at ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(currentTime))}")
                        lastHeartbeat = currentTime
                    }
                    
                    trackCurrentAppUsage(userId, sessionId)
                    consecutiveErrors = 0 // Reset error counter on success
                    delay(TRACKING_INTERVAL)
                } catch (e: Exception) {
                    consecutiveErrors++
                    android.util.Log.e("RealTimeSessionTracker", "❌ Tracking error #$consecutiveErrors: ${e.message}")
                    e.printStackTrace()
                    
                    // If too many consecutive errors, wait longer before retrying
                    if (consecutiveErrors > 5) {
                        android.util.Log.w("RealTimeSessionTracker", "⚠️ Too many errors, extending delay to 10 seconds")
                        delay(10000)
                    } else {
                        delay(TRACKING_INTERVAL)
                    }
                }
            }
            android.util.Log.i("RealTimeSessionTracker", "🛑 Tracking stopped")
        }
    }
    
    fun stopTracking() {
        isTracking = false
        trackerScope.launch {
            endAllActiveSessions()
        }
    }
    
    /**
     * Ensures tracking is active and restarts it if it has been suspended.
     * This is called when the app resumes to recover from Android battery optimization.
     */
    fun ensureTrackingActive(userId: String) {
        if (!isTracking) {
            android.util.Log.w("RealTimeSessionTracker", "⚠️ Tracking was supposed to be active but isn't - restarting")
            startTracking(userId)
        } else {
            android.util.Log.d("RealTimeSessionTracker", "✅ Tracking is already active")
        }
    }
    
    private suspend fun trackCurrentAppUsage(userId: String, sessionId: String) {
        val currentApp = getCurrentForegroundApp()
        android.util.Log.d("RealTimeSessionTracker", "🔍 Current foreground app: ${currentApp?.appName} (${currentApp?.packageName})")
        
        if (currentApp == null) {
            // No app in foreground, end all active sessions
            android.util.Log.d("RealTimeSessionTracker", "❌ No foreground app detected, ending all active sessions")
            endAllActiveSessions()
            return
        }
        
        // Filter out launcher apps and the tracking app itself
        if (isLauncherApp(currentApp.packageName) || isSelfApp(currentApp.packageName)) {
            android.util.Log.d("RealTimeSessionTracker", "🏠📱 Detected launcher/self app ${currentApp.packageName}, not tracking - ending all active sessions")
            endAllActiveSessions()
            return
        }
        
        val currentTime = Date()
        
        // CRITICAL FIX: Always end sessions for apps that are no longer in foreground
        val otherSessions = activeSessions.keys.filter { it != currentApp.packageName }
        if (otherSessions.isNotEmpty()) {
            android.util.Log.d("RealTimeSessionTracker", "App switched! Ending ${otherSessions.size} other active sessions")
            otherSessions.forEach { packageName ->
                endAppSession(packageName)
            }
        }
        
        if (!activeSessions.containsKey(currentApp.packageName)) {
            // Start new session for this app
            android.util.Log.d("RealTimeSessionTracker", "Starting new session for ${currentApp.appName}")
            startNewAppSession(userId, sessionId, currentApp, currentTime)
        } else {
            // Continue existing session - no action needed as duration is calculated on end
            android.util.Log.d("RealTimeSessionTracker", "Continuing existing session for ${currentApp.appName}")
        }
    }
    
    private suspend fun startNewAppSession(
        userId: String, 
        sessionId: String, 
        appInfo: AppInfo, 
        startTime: Date
    ) {
        android.util.Log.d("RealTimeSessionTracker", "💾 Creating new session in database for ${appInfo.appName} (${appInfo.packageName})")
        
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
            networkType = getNetworkType(),
            isSynced = false // Explicitly set to false for new records
        )
        
        val usageId = userUsageStatsRepository.insertUsageStatsAndGetId(usageStats)
        android.util.Log.d("RealTimeSessionTracker", "Started new session for ${appInfo.appName} (${appInfo.packageName}) with ID: $usageId at ${startTime} (isSynced=false)")
        
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
                
                // Debug logging for duration calculation
                android.util.Log.d("RealTimeSessionTracker", "Ending session for ${activeSession.appName} (${packageName})")
                android.util.Log.d("RealTimeSessionTracker", "  Start time: ${activeSession.startTime} (${activeSession.startTime.time}ms)")
                android.util.Log.d("RealTimeSessionTracker", "  End time: ${endTime} (${endTime.time}ms)")
                android.util.Log.d("RealTimeSessionTracker", "  Calculated duration: ${duration}ms (${duration/1000.0}s)")
                
                if (duration < 0) {
                    android.util.Log.w("RealTimeSessionTracker", "WARNING: Negative duration detected! This should not happen.")
                    return
                }
                
                if (duration == 0L) {
                    android.util.Log.w("RealTimeSessionTracker", "WARNING: Zero duration detected - removing session without updating database")
                    activeSessions.remove(packageName)
                    return
                }
                
                userUsageStatsRepository.endUsageSession(
                    activeSession.usageId,
                    endTime,
                    duration
                )
                
                android.util.Log.d("RealTimeSessionTracker", "✅ Session ended for ${activeSession.appName} - Duration: ${duration}ms (${duration/1000.0}s)")
                activeSessions.remove(packageName)
            } else {
                android.util.Log.w("RealTimeSessionTracker", "No active session found for package: $packageName")
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
        var eventCount = 0
        
        // Debug: Log all foreground events in the time window
        val allEvents = mutableListOf<Pair<String, Long>>()
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            eventCount++
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                allEvents.add(event.packageName to event.timeStamp)
                if (event.timeStamp >= lastTimeStamp) {
                    lastPackageName = event.packageName
                    lastTimeStamp = event.timeStamp
                }
            }
        }
        
        // Debug logging
        android.util.Log.d("RealTimeSessionTracker", "📊 UsageEvents query: found $eventCount total events, ${allEvents.size} foreground events")
        if (allEvents.isNotEmpty()) {
            android.util.Log.d("RealTimeSessionTracker", "📊 Recent foreground apps: ${allEvents.takeLast(3)}")
        }
        android.util.Log.d("RealTimeSessionTracker", "📊 Current foreground app: $lastPackageName (timestamp: $lastTimeStamp)")
        
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
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()
            android.util.Log.d("RealTimeSessionTracker", "📱 App name mapping: $packageName -> $appName")
            appName
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.w("RealTimeSessionTracker", "⚠️ App name not found for package: $packageName, using package name")
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
    
    /**
     * Determines if a package represents a launcher app (home screen).
     * Only filters out actual launcher/home screen apps, allowing all other apps to be tracked.
     */
    private fun isLauncherApp(packageName: String): Boolean {
        // Known launcher package patterns
        val launcherPatterns = listOf(
            "launcher",
            "nexuslauncher", 
            "trebuchet",
            "home"
        )
        
        val isKnownLauncher = launcherPatterns.any { pattern ->
            packageName.contains(pattern, ignoreCase = true)
        }
        
        if (isKnownLauncher) {
            android.util.Log.d("RealTimeSessionTracker", "🏠 Detected launcher app: $packageName")
            return true
        }
        
        // Additional check: apps that are launchers typically have CATEGORY_HOME intent
        return try {
            val packageManager = context.packageManager
            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
            val resolveInfos = packageManager.queryIntentActivities(homeIntent, 0)
            
            val isHomeLauncher = resolveInfos.any { it.activityInfo.packageName == packageName }
            if (isHomeLauncher) {
                android.util.Log.d("RealTimeSessionTracker", "🏠 Detected home launcher via intent: $packageName")
            }
            isHomeLauncher
        } catch (e: Exception) {
            android.util.Log.w("RealTimeSessionTracker", "Error checking launcher for $packageName: ${e.message}")
            false
        }
    }
    
    /**
     * Determines if a package is the tracking app itself.
     * Prevents self-tracking to avoid skewing usage data.
     */
    private fun isSelfApp(packageName: String): Boolean {
        val selfPackage = context.packageName
        val isSelf = packageName == selfPackage
        if (isSelf) {
            android.util.Log.d("RealTimeSessionTracker", "📱 Detected self app: $packageName - not tracking")
        }
        return isSelf
    }
    
    fun onDestroy() {
        stopTracking()
        trackerScope.cancel()
    }
}
