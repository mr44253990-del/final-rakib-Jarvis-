package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import com.example.action.ActionEngine
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(app: JarvisApplication) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var responses by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    val actionEngine = remember { ActionEngine(context, app.memoryRepository) }
    
    val apiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val modelName by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.1-flash-lite-preview")

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
            items(responses) { resp ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(resp, modifier = Modifier.padding(12.dp))
                }
            }
        }
        
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Jarvis...") }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val msg = message
                    if (msg.isNotBlank()) {
                        responses = responses + "You: $msg"
                        message = ""
                        scope.launch {
                            val reply = actionEngine.processUserRequest(msg, apiKey ?: "", modelName)
                            responses = responses + "Jarvis: $reply"
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
