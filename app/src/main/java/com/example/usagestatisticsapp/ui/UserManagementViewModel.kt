package com.example.usagestatisticsapp.ui

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usagestatisticsapp.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class UserManagementViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val userUsageStatsRepository: UserUsageStatsRepository,
    private val masterUsageStatsRepository: MasterUsageStatsRepository,
    private val context: Context
) : ViewModel() {

    // User setup state
    private val _userSetupState = MutableStateFlow(UserSetupUiState())
    val userSetupState: StateFlow<UserSetupUiState> = _userSetupState.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Usage stats state
    private val _usageStats = MutableStateFlow<List<UserUsageStats>>(emptyList())
    val usageStats: StateFlow<List<UserUsageStats>> = _usageStats.asStateFlow()

    // Current app usage state
    private val _currentAppUsage = MutableStateFlow<String?>(null)
    val currentAppUsage: StateFlow<String?> = _currentAppUsage.asStateFlow()

    // Tracking state
    private val _isTrackingEnabled = MutableStateFlow(true)
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    // Real-time session tracking
    private lateinit var realTimeSessionTracker: RealTimeSessionTracker
    private lateinit var dataAggregationService: DataAggregationService
    private lateinit var dataExportService: DataExportService

    // Usage stats manager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    init {
        initializeServices()
        checkUserProfile()
    }

    private fun initializeServices() {
        realTimeSessionTracker = RealTimeSessionTracker(userUsageStatsRepository, context)
        dataAggregationService = DataAggregationService(userUsageStatsRepository, masterUsageStatsRepository)
        dataExportService = DataExportService(context, userProfileRepository, userUsageStatsRepository, masterUsageStatsRepository)
        
        // Observe current app usage
        viewModelScope.launch {
            realTimeSessionTracker.currentAppUsage.collect { currentUsage ->
                _currentAppUsage.value = currentUsage?.appName
            }
        }
    }

    private fun checkUserProfile() {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                _userProfile.value = profile
                if (profile != null) {
                    // User is already set up, start session tracking
                    startSessionTracking(profile.userId)
                }
            }
        }
    }

    fun onUserIdChanged(userId: String) {
        val isValid = validateUserId(userId)
        _userSetupState.value = _userSetupState.value.copy(
            userId = userId,
            isUserIdValid = isValid,
            error = if (userId.isNotEmpty() && !isValid) "Please enter a valid Study ID" else null
        )
    }

    fun onSetupComplete() {
        val userId = _userSetupState.value.userId
        if (!validateUserId(userId)) return

        viewModelScope.launch {
            try {
                _userSetupState.value = _userSetupState.value.copy(isLoading = true, error = null)

                // Create user profile
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                val userProfile = UserProfile(
                    userId = userId,
                    registrationDate = Date(),
                    deviceId = deviceId
                )

                userProfileRepository.insertUserProfile(userProfile)
                _userProfile.value = userProfile

                // Start session tracking
                startSessionTracking(userId)

                _userSetupState.value = _userSetupState.value.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                _userSetupState.value = _userSetupState.value.copy(
                    isLoading = false,
                    error = "Failed to set up user profile: ${e.message}"
                )
            }
        }
    }

    private fun startSessionTracking(userId: String) {
        viewModelScope.launch {
            // Start real-time session tracking
            realTimeSessionTracker.startTracking(userId)
            
            // Load initial usage stats for aggregation
            loadUserUsageStats(userId)
        }
    }

    fun loadUserUsageStats(userId: String, intervalType: Int = UsageStatsManager.INTERVAL_DAILY) {
        viewModelScope.launch {
            try {
                // Load usage stats from database
                userUsageStatsRepository.getUsageStatsFromDate(userId, Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                    .collect { stats ->
                        _usageStats.value = stats.sortedByDescending { it.startTime }
                    }
                
                // Also load system usage stats for aggregation
                val stats = getUsageStatistics(intervalType)
                
                if (stats.isNotEmpty()) {
                    // Filter and process stats
                    val filteredStats = stats.filter { usageStat ->
                        val hasUsage = usageStat.totalTimeInForeground > 0
                        val notSystemComponent = !isSystemComponent(usageStat.packageName)
                        hasUsage && notSystemComponent
                    }

                    // Aggregate data for master database
                    dataAggregationService.aggregateUserData(userId, "Week 1")
                }
            } catch (e: Exception) {
                // Handle errors
                e.printStackTrace()
            }
        }
    }

    private fun getUsageStatistics(intervalType: Int): List<UsageStats> {
        val calendar = Calendar.getInstance()
        
        when (intervalType) {
            UsageStatsManager.INTERVAL_DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            UsageStatsManager.INTERVAL_WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
            }
            UsageStatsManager.INTERVAL_MONTHLY -> {
                calendar.add(Calendar.MONTH, -1)
            }
            UsageStatsManager.INTERVAL_YEARLY -> {
                calendar.add(Calendar.YEAR, -1)
            }
        }
        
        return usageStatsManager.queryUsageStats(
            intervalType,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
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

    private fun isSystemComponent(packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            
            val hasLauncherIntent = packageManager.resolveActivity(
                android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(packageName),
                0
            ) != null
            
            val shouldFilter = isSystemApp && !isUpdatedSystemApp && !hasLauncherIntent
            shouldFilter
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun validateUserId(userId: String): Boolean {
        // Basic validation - can be enhanced based on your study requirements
        return userId.isNotBlank() && userId.length >= 3 && userId.length <= 20
    }

    fun endSession() {
        realTimeSessionTracker.stopTracking()
    }
    
    fun toggleTracking() {
        val newTrackingState = !_isTrackingEnabled.value
        _isTrackingEnabled.value = newTrackingState
        
        if (newTrackingState) {
            // Resume tracking
            val userId = _userProfile.value?.userId
            if (userId != null) {
                startSessionTracking(userId)
            }
        } else {
            // Pause tracking
            realTimeSessionTracker.stopTracking()
        }
    }
    
    fun refreshUsageStats() {
        val userId = _userProfile.value?.userId
        if (userId != null) {
            loadUserUsageStats(userId)
        }
    }
    
    // Export functions
    fun exportToJson(onSuccess: (android.net.Uri) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = dataExportService.exportToJson()
                onSuccess(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }
    
    fun exportToCsv(onSuccess: (android.net.Uri) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = dataExportService.exportToCsv()
                onSuccess(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }
    
    fun exportDatabaseFile(onSuccess: (android.net.Uri) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = dataExportService.exportDatabaseFile()
                onSuccess(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }
    
    suspend fun getExportSummary(): DataExportService.ExportSummary {
        return dataExportService.getExportSummary()
    }

    override fun onCleared() {
        super.onCleared()
        endSession()
        realTimeSessionTracker.onDestroy()
    }
}
