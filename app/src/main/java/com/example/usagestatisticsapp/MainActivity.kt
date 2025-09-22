package com.example.usagestatisticsapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.usagestatisticsapp.network.SyncService
import com.example.usagestatisticsapp.network.SyncStatus
import com.example.usagestatisticsapp.network.ApiRepository
import com.example.usagestatisticsapp.ui.MainScreen
import com.example.usagestatisticsapp.data.AppDatabase
import com.example.usagestatisticsapp.data.UserUsageStatsRepository
import com.example.usagestatisticsapp.data.RealTimeSessionTracker
import com.example.usagestatisticsapp.ui.theme.UsageStatisticsAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Initialize SyncService
    private lateinit var syncService: SyncService
    private lateinit var realTimeTracker: RealTimeSessionTracker
    private var syncStatus by mutableStateOf(SyncStatus(false, null, null, false))
    private var hasUsagePermission by mutableStateOf(false)
    private var isTrackingEnabled by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = UserUsageStatsRepository(database.userUsageStatsDao())
        
        // Initialize SyncService with real repository
        syncService = SyncService(applicationContext, repository)
        
        // Initialize real-time tracker
        realTimeTracker = RealTimeSessionTracker(repository, applicationContext)
        
        // Initialize sync status and permission
        syncStatus = syncService.getSyncStatus()
        hasUsagePermission = hasUsageStatsPermission()
        isTrackingEnabled = false // Start with tracking disabled
        
        enableEdgeToEdge()
        setContent {
            UsageStatisticsAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        isTrackingEnabled = isTrackingEnabled,
                        hasUsagePermission = hasUsagePermission,
                        onTrackingToggle = { handleTrackingToggle() },
                        onOpenSettings = { openUsageAccessSettings() },
                        syncStatus = syncStatus,
                        onSetStudyId = { studyId -> handleSetStudyId(studyId) },
                        onClearStudyId = { handleClearStudyId() },
                        onSync = { handleSync() },
                        onConnectivityCheck = { handleConnectivityCheck() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permission status when returning from settings
        val newPermissionStatus = hasUsageStatsPermission()
        if (newPermissionStatus != hasUsagePermission) {
            hasUsagePermission = newPermissionStatus
            if (newPermissionStatus) {
                Toast.makeText(this, "✅ Usage Access permission granted! Data collection can now begin.", Toast.LENGTH_LONG).show()
            }
        }
        
        // CRITICAL FIX: Restart tracking if it was enabled but may have been suspended
        if (isTrackingEnabled && hasUsagePermission && ::realTimeTracker.isInitialized) {
            android.util.Log.i("MainActivity", "🔄 App resumed - ensuring tracking is active")
            realTimeTracker.ensureTrackingActive("user_001")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop tracking when app is destroyed
        if (::realTimeTracker.isInitialized) {
            realTimeTracker.stopTracking()
        }
    }
    
    private fun handleTrackingToggle() {
        if (hasUsagePermission) {
            isTrackingEnabled = !isTrackingEnabled
            
            if (isTrackingEnabled) {
                // Start real-time tracking
                realTimeTracker.startTracking("default_user")
                Toast.makeText(this, "Data tracking started", Toast.LENGTH_SHORT).show()
            } else {
                // Stop real-time tracking
                realTimeTracker.stopTracking()
                Toast.makeText(this, "Data tracking stopped", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please grant Usage Access permission first", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleSetStudyId(studyId: String) {
        val result = syncService.setStudyId(studyId)
        result.onSuccess {
            syncStatus = syncService.getSyncStatus()
            
            // Check if demographics have been collected for this Study ID
            val sharedPrefs = getSharedPreferences("demographics_prefs", Context.MODE_PRIVATE)
            val demographicsCollected = sharedPrefs.getBoolean("demographics_collected_$studyId", false)
            
            if (!demographicsCollected) {
                // Launch demographics collection activity
                val intent = Intent(this, DemographicsActivity::class.java)
                intent.putExtra("STUDY_ID", studyId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Study ID set successfully! You can now start tracking.", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { error ->
            Toast.makeText(this, "Failed to set Study ID: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleClearStudyId() {
        syncService.clearStudyId()
        syncStatus = syncService.getSyncStatus()
        Toast.makeText(this, "Study ID cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleSync() {
        lifecycleScope.launch {
            val result = syncService.syncUsageData()
            result.onSuccess { recordCount ->
                syncStatus = syncService.getSyncStatus()
                Toast.makeText(this@MainActivity, "Synced $recordCount records successfully!", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Sync failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun handleConnectivityCheck() {
        lifecycleScope.launch {
            val result = syncService.checkConnectivity()
            result.onSuccess { isConnected ->
                val message = if (isConnected) "Backend is reachable!" else "Backend is not reachable"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Connection test failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            
            // Use the modern unsafeCheckOpNoThrow method for API 29+ or fallback to deprecated method
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            
            val hasPermission = mode == AppOpsManager.MODE_ALLOWED
            
            // Debug logging
            android.util.Log.d("MainActivity", "Usage stats permission check: mode=$mode, hasPermission=$hasPermission")
            
            hasPermission
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking usage stats permission", e)
            false
        }
    }
    

}