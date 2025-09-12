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
import com.example.usagestatisticsapp.ui.theme.UsageStatisticsAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Initialize SyncService
    private lateinit var syncService: SyncService
    private var syncStatus by mutableStateOf(SyncStatus(false, null, null, false))
    private var hasUsagePermission by mutableStateOf(false)
    private var isTrackingEnabled by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SyncService with a simple repository
        syncService = SyncService(applicationContext, null) // Simplified for now
        
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
    }
    
    private fun handleTrackingToggle() {
        if (hasUsagePermission) {
            isTrackingEnabled = !isTrackingEnabled
            val status = if (isTrackingEnabled) "started" else "stopped"
            Toast.makeText(this, "Data tracking $status", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please grant Usage Access permission first", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleSetStudyId(studyId: String) {
        val result = syncService.setStudyId(studyId)
        result.onSuccess {
            syncStatus = syncService.getSyncStatus()
            Toast.makeText(this, "Study ID set successfully! You can now start tracking.", Toast.LENGTH_SHORT).show()
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
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        val hasPermission = mode == AppOpsManager.MODE_ALLOWED
        
        // Debug logging
        android.util.Log.d("MainActivity", "Usage stats permission check: mode=$mode, hasPermission=$hasPermission")
        
        return hasPermission
    }
    

}