package com.spacetec.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.R
import com.spacetec.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var darkTheme by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var crashReportsEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedUnitSystem by remember { mutableStateOf("Metric") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            // App Settings
            SettingsSectionHeader(title = "Appearance")
            
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Theme",
                action = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it }
                    )
                }
            )
            
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = selectedLanguage,
                onClick = { /* Show language picker */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Speed,
                title = "Unit System",
                subtitle = selectedUnitSystem,
                onClick = { /* Show unit system picker */ }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Notifications
            SettingsSectionHeader(title = "Notifications")
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Enable Notifications",
                action = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Privacy
            SettingsSectionHeader(title = "Privacy")
            
            SettingsItem(
                icon = Icons.Default.Security,
                title = "Crash Reports",
                subtitle = "Help improve the app by sending crash reports",
                action = {
                    Switch(
                        checked = crashReportsEnabled,
                        onCheckedChange = { crashReportsEnabled = it }
                    )
                }
            )
            
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                onClick = { /* Open privacy policy */ }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // About
            SettingsSectionHeader(title = "About")
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0"
            )
            
            SettingsItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                onClick = { /* Open help */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Email,
                title = "Contact Us",
                onClick = { /* Open email */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Info
            Text(
                text = " 2023 SpaceTec",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (action != null) {
                action()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
