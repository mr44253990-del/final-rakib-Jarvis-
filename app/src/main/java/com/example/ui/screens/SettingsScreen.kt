package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: JarvisApplication) {
    val scope = rememberCoroutineScope()
    
    val savedApiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val savedModel by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.1-flash-lite")
    
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey ?: "") }
    var modelName by remember(savedModel) { mutableStateOf(savedModel) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(24.dp))
        
        // Profile Profile
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Rakib Jarvis", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Premium User", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SettingsItem(Icons.Default.Memory, "Model Configuration", modelName)
            }
            item {
                SettingsItem(Icons.Default.VpnKey, "API Configuration", "Configure your API key")
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assistant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Assistant", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Enable overlay assistant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }
            }
            item { SettingsItem(Icons.Default.Build, "Auto Fix & Console", "Auto detect and fix errors") }
            item { SettingsItem(Icons.Default.VolumeUp, "Voice & Language", "English (United States)") }
            item { SettingsItem(Icons.Default.Palette, "Theme & Appearance", "Dark Theme") }
            item { SettingsItem(Icons.Default.Extension, "Advanced Tools", "Extra smart features") }
            item { SettingsItem(Icons.Default.Backup, "Backup & Restore", "Backup your data") }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
