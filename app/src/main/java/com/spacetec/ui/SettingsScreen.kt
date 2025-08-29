package com.spacetec.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetec.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSelectTheme: () -> Unit = {},
    onSelectLanguage: () -> Unit = {},
    onSelectUnits: () -> Unit = {},
    onSelectOBD: () -> Unit = {},
    onSelectVehicle: () -> Unit = {},
    onSelectAbout: () -> Unit = {},
    onSelectHelp: () -> Unit = {},
    onSelectPrivacy: () -> Unit = {}
) {
    // App settings state
    var darkMode by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoConnect by remember { mutableStateOf(true) }
    var dataSharing by remember { mutableStateOf(false) }
    
    // App version
    val appVersion = "1.0.0"
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // App Settings Section
            SettingsSection(title = "APP SETTINGS") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Enable dark theme",
                    action = {
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { darkMode = it },
                            modifier = Modifier.toggleable(
                                value = darkMode,
                                onValueChange = { darkMode = it },
                                role = Role.Switch
                            )
                        )
                    },
                    onClick = { darkMode = !darkMode }
                )
                
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Service reminders and alerts",
                    action = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            modifier = Modifier.toggleable(
                                value = notificationsEnabled,
                                onValueChange = { notificationsEnabled = it },
                                role = Role.Switch
                            )
                        )
                    },
                    onClick = { notificationsEnabled = !notificationsEnabled }
                )
                
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English (US)",
                    action = {},
                    onClick = onSelectLanguage
                )
                
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Units",
                    subtitle = "Metric (km, °C, L/100km)",
                    action = {},
                    onClick = onSelectUnits
                )
            }
            
            // Connection Settings Section
            SettingsSection(title = "CONNECTION") {
                SettingsItem(
                    icon = Icons.Default.Bluetooth,
                    title = "OBD2 Adapter",
                    subtitle = "Auto-detect",
                    action = {},
                    onClick = onSelectOBD
                )
                
                SettingsItem(
                    icon = Icons.Default.DirectionsCar,
                    title = "Vehicle Profile",
                    subtitle = "2 vehicles",
                    action = {},
                    onClick = onSelectVehicle
                )
                
                SettingsItem(
                    icon = Icons.Default.Link,
                    title = "Auto-connect",
                    subtitle = "Connect to last used adapter",
                    action = {
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = { autoConnect = it },
                            modifier = Modifier.toggleable(
                                value = autoConnect,
                                onValueChange = { autoConnect = it },
                                role = Role.Switch
                            )
                        )
                    },
                    onClick = { autoConnect = !autoConnect }
                )
            }
            
            // Data & Privacy Section
            SettingsSection(title = "DATA & PRIVACY") {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Data Sharing",
                    subtitle = "Help improve SpaceTec",
                    action = {
                        Switch(
                            checked = dataSharing,
                            onCheckedChange = { dataSharing = it },
                            modifier = Modifier.toggleable(
                                value = dataSharing,
                                onValueChange = { dataSharing = it },
                                role = Role.Switch
                            )
                        )
                    },
                    onClick = { dataSharing = !dataSharing }
                )
                
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    action = {},
                    onClick = onSelectPrivacy
                )
            }
            
            // Support Section
            SettingsSection(title = "SUPPORT") {
                SettingsItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & Support",
                    subtitle = "FAQs and contact",
                    action = {},
                    onClick = onSelectHelp
                )
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About SpaceTec",
                    subtitle = "Version $appVersion",
                    action = {},
                    onClick = onSelectAbout
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App version and copyright
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SpaceTec Diagnostics Pro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2023 SpaceTec. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
        )
        
        // Section content
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action (switch, chevron, etc.)
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                action()
            }
        }
    }
    
    // Divider between items
    Divider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}
