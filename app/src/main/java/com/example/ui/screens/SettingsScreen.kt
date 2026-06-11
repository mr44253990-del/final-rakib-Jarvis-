package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: JarvisApplication) {
    val scope = rememberCoroutineScope()
    
    val savedApiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val savedModel by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.1-flash-lite-preview")
    
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey ?: "") }
    var modelName by remember(savedModel) { mutableStateOf(savedModel) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configuration", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    app.appSettings.saveSettings(apiKey, modelName)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}
