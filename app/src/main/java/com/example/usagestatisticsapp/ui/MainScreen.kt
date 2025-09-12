package com.example.usagestatisticsapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usagestatisticsapp.R
import com.example.usagestatisticsapp.network.SyncStatus

@Composable
fun MainScreen(
    isTrackingEnabled: Boolean,
    hasUsagePermission: Boolean,
    onTrackingToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportDatabase: () -> Unit,
    syncStatus: SyncStatus,
    onSetStudyId: (studyId: String) -> Unit,
    onClearStudyId: () -> Unit,
    onSync: () -> Unit,
    onConnectivityCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStudyIdDialog by remember { mutableStateOf(false) }
    var studyId by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Permission Status
        if (!hasUsagePermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "This app needs Usage Access permission to collect app usage statistics for the study.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Grant Usage Access Permission")
                    }
                }
            }
        }

        // Tracking Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTrackingEnabled) "Data Collection Active" else "Data Collection Paused",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isTrackingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = if (isTrackingEnabled)
                        "Your app usage data is being collected for research purposes."
                    else
                        "Data collection is currently paused. Enable to participate in the study.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (!hasUsagePermission) {
                    Text(
                        text = "Grant permission above to enable tracking",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = onTrackingToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = hasUsagePermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTrackingEnabled)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isTrackingEnabled) "Stop Tracking" else "Start Tracking",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Study Participation Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Study Participation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Study ID Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (syncStatus.hasStudyId)
                            "Study ID: ${syncStatus.studyId}"
                        else
                            "No Study ID entered",
                        fontSize = 14.sp,
                        color = if (syncStatus.hasStudyId)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (syncStatus.hasStudyId) {
                        TextButton(onClick = onClearStudyId) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                }

                // Last Sync Time
                if (syncStatus.lastSyncTime != null) {
                    Text(
                        text = "Last sync: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(syncStatus.lastSyncTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Sync Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!syncStatus.hasStudyId) {
                        Button(
                            onClick = { showStudyIdDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Enter Study ID", fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = onSync,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Sync Data", fontSize = 14.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onConnectivityCheck,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Connection", fontSize = 12.sp)
                    }
                }
            }
        }

        // Data Export Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Data Export",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Export your research data for analysis:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportJson,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("JSON", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onExportCsv,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("CSV", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onExportDatabase,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("DB", fontSize = 12.sp)
                    }
                }
            }
        }

        // Study Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "About This Study",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "This application collects granular user data to assess the impact and relevance that digital goods have on our evaluation of the economy. With your consent, your usage data will be collected to better understand consumer behaviors and the value placed upon distinct digital technologies. This research falls under the Stanford Digital Economy Lab's GDP-B project, which seeks to establish a new method of economic assessment that considers the effects of contemporary technologies.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }

    // Study ID Dialog
    if (showStudyIdDialog) {
        AlertDialog(
            onDismissRequest = { showStudyIdDialog = false },
            title = { Text("Enter Study ID") },
            text = {
                Column {
                    Text(
                        text = "Please enter the Study ID provided by the research team:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = studyId,
                        onValueChange = { studyId = it },
                        label = { Text("Study ID") },
                        placeholder = { Text("e.g., GDP-B-2025-001") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (studyId.isNotBlank()) {
                            onSetStudyId(studyId.trim())
                            showStudyIdDialog = false
                            studyId = ""
                        }
                    },
                    enabled = studyId.isNotBlank()
                ) {
                    Text("Join Study")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showStudyIdDialog = false
                    studyId = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
