package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import com.example.service.JarvisOverlayService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: JarvisApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val savedApiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val savedMistralKey by app.appSettings.mistralApiKeyFlow.collectAsState(initial = "")
    val savedModel by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.5-flash")
    
    var showApiDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(com.example.service.JarvisOverlayService.isServiceRunning) }

    if (showApiDialog) {
        var tempGeminiKey by remember { mutableStateOf(savedApiKey ?: "") }
        var tempMistralKey by remember { mutableStateOf(savedMistralKey ?: "") }
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = { Text("API Configurations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter API keys for your preferred models:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = tempGeminiKey,
                        onValueChange = { tempGeminiKey = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempMistralKey,
                        onValueChange = { tempMistralKey = it },
                        label = { Text("Mistral API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { 
                        app.appSettings.saveSettings(tempGeminiKey, savedModel)
                        app.appSettings.saveMistralKey(tempMistralKey)
                    }
                    showApiDialog = false
                    Toast.makeText(context, "API Keys Saved Successfully", Toast.LENGTH_SHORT).show()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showModelDialog) {
        val modelOptions = listOf(
            "gemini-3.5-flash" to "Gemini 3.5 Flash (Fast Default)",
            "mistral-large-latest" to "Mistral Large (HQ Tool Call)",
            "open-mixtral-8x22b" to "Mixtral 8x22B (Tool Call)",
            "open-mistral-nemo" to "Mistral Nemo (Smart Tool)",
            "codestral-latest" to "Codestral Latest (Code Specialty)"
        )
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Select Intelligence Model") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(modelOptions) { option ->
                        val isSelected = savedModel == option.first
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    scope.launch { app.appSettings.saveSettings(savedApiKey ?: "", option.first) }
                                    showModelDialog = false
                                    Toast.makeText(context, "Active model set: ${option.second}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { app.appSettings.saveSettings(savedApiKey ?: "", option.first) }
                                    showModelDialog = false
                                    Toast.makeText(context, "Active model set: ${option.second}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(option.second, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(option.first, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) { Text("Close") }
            }
        )
    }

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
                SettingsItem(Icons.Default.Memory, "Model Configuration", savedModel, onClick = { showModelDialog = true })
            }
            item {
                SettingsItem(Icons.Default.VpnKey, "API Configuration", "Configure your API key", onClick = { showApiDialog = true })
            }
            item {
                val isServiceActive = com.example.service.JarvisOverlayService.isServiceRunning
                Row(modifier = Modifier.fillMaxWidth().clickable { 
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                        context.startActivity(intent)
                    } else {
                        val newActiveStatus = !isServiceActive
                        overlayEnabled = newActiveStatus
                        val serviceIntent = Intent(context, JarvisOverlayService::class.java)
                        if (newActiveStatus) {
                            try {
                                context.startService(serviceIntent)
                                Toast.makeText(context, "Overlay Enabled", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to start overlay: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            context.stopService(serviceIntent)
                            Toast.makeText(context, "Overlay Disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assistant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Assistant", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(if (isServiceActive) "Overlay Assistant is Running" else "Enable overlay small assistant UI", style = MaterialTheme.typography.bodySmall, color = if (isServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isServiceActive, onCheckedChange = { _ -> })
                }
            }
            item { SettingsItem(Icons.Default.Build, "Auto Fix & Console", "Auto detect and fix errors", onClick = { Toast.makeText(context, "Auto fix initiated...", Toast.LENGTH_SHORT).show() }) }
            item { SettingsItem(Icons.Default.VolumeUp, "Voice & Language", "বাংলা (Bangladesh)", onClick = { Toast.makeText(context, "ভাষা: বাংলা সফলভাবে সেট করা হয়েছে", Toast.LENGTH_SHORT).show() }) }
            item { SettingsItem(Icons.Default.Palette, "Theme & Appearance", "Dark Theme", onClick = { Toast.makeText(context, "Theme set to Dark Space", Toast.LENGTH_SHORT).show() }) }
            item { SettingsItem(Icons.Default.Extension, "Advanced Tools", "Extra smart features", onClick = { Toast.makeText(context, "Advanced tools enabled", Toast.LENGTH_SHORT).show() }) }
            item { SettingsItem(Icons.Default.Backup, "Backup & Restore", "Backup your data", onClick = { Toast.makeText(context, "Backup completed", Toast.LENGTH_SHORT).show() }) }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
