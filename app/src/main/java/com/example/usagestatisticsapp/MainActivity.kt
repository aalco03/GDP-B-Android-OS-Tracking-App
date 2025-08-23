package com.example.usagestatisticsapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.usagestatisticsapp.data.AppDatabase
import com.example.usagestatisticsapp.data.UserProfileRepository
import com.example.usagestatisticsapp.data.UserUsageStatsRepository
import com.example.usagestatisticsapp.data.MasterUsageStatsRepository
import com.example.usagestatisticsapp.ui.UserSetupScreen
import com.example.usagestatisticsapp.ui.UserManagementViewModel
import com.example.usagestatisticsapp.ui.MainScreen
import com.example.usagestatisticsapp.ui.theme.UsageStatisticsAppTheme
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle export result if needed
    }
    
    private val userManagementViewModel: UserManagementViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val userProfileRepository = UserProfileRepository(database.userProfileDao())
                val userUsageStatsRepository = UserUsageStatsRepository(database.userUsageStatsDao())
                val masterUsageStatsRepository = MasterUsageStatsRepository(database.masterUsageStatsDao())
                return UserManagementViewModel(
                    userProfileRepository,
                    userUsageStatsRepository,
                    masterUsageStatsRepository,
                    applicationContext
                ) as T
            }
        }
    }
    
    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
    
    private fun shareFile(uri: Uri, mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Stanford HAI Study Data Export")
            putExtra(Intent.EXTRA_TEXT, "Research data export from Stanford HAI Study app")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        exportLauncher.launch(Intent.createChooser(intent, "Share Data Export"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsageStatisticsAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MultiTenantUsageStatsApp(
                        modifier = Modifier.padding(innerPadding),
                        userManagementViewModel = userManagementViewModel,
                        onOpenSettings = { openUsageAccessSettings() },
                        onShareFile = { uri, mimeType, fileName -> shareFile(uri, mimeType, fileName) }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        userManagementViewModel.endSession()
    }
}

@Composable
fun MultiTenantUsageStatsApp(
    modifier: Modifier = Modifier,
    userManagementViewModel: UserManagementViewModel,
    onOpenSettings: () -> Unit,
    onShareFile: (Uri, String, String) -> Unit
) {
    val userSetupState by userManagementViewModel.userSetupState.collectAsStateWithLifecycle()
    val userProfile by userManagementViewModel.userProfile.collectAsStateWithLifecycle()
    val isTrackingEnabled by userManagementViewModel.isTrackingEnabled.collectAsStateWithLifecycle()
    
    // Show user setup screen if user is not set up
    if (userProfile == null) {
        UserSetupScreen(
            uiState = userSetupState,
            onUserIdChanged = { userId ->
                userManagementViewModel.onUserIdChanged(userId)
            },
            onSetupComplete = {
                userManagementViewModel.onSetupComplete()
            },
            modifier = modifier
        )
    } else {
        // Show main screen with Stanford HAI logo and tracking toggle
        MainScreen(
            isTrackingEnabled = isTrackingEnabled,
            onTrackingToggle = {
                userManagementViewModel.toggleTracking()
            },
            onExportJson = {
                userManagementViewModel.exportToJson(
                    onSuccess = { uri -> onShareFile(uri, "application/json", "usage_stats_export.json") },
                    onError = { error -> /* Handle error */ }
                )
            },
            onExportCsv = {
                userManagementViewModel.exportToCsv(
                    onSuccess = { uri -> onShareFile(uri, "text/csv", "usage_stats_export.csv") },
                    onError = { error -> /* Handle error */ }
                )
            },
            onExportDatabase = {
                userManagementViewModel.exportDatabaseFile(
                    onSuccess = { uri -> onShareFile(uri, "application/x-sqlite3", "usage_stats_database.db") },
                    onError = { error -> /* Handle error */ }
                )
            },
            modifier = modifier
        )
    }
}