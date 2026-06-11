package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.JarvisApplication
import com.example.action.ActionEngine
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(app: JarvisApplication) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var responses by remember { mutableStateOf(listOf<String>()) }
    var showPreviewPanel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val actionEngine = remember { ActionEngine(context, app.memoryRepository) }
    
    val apiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val modelName by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.1-flash-lite-preview")
    val memories by app.memoryRepository.allMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentLogs = memories.filter { it.type == "LOG" }.take(20)

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Chat", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { showPreviewPanel = !showPreviewPanel }) {
                    Icon(Icons.Default.History, contentDescription = "Toggle Action Preview")
                }
            }
            
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(responses) { resp ->
                    val isYou = resp.startsWith("You:")
                    val align = if (isYou) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
                    val color = if (isYou) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = align) {
                        Card(colors = CardDefaults.cardColors(containerColor = color)) {
                            Text(resp, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                        }
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
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        AnimatedVisibility(
            visible = showPreviewPanel,
            enter = expandHorizontally(expandFrom = androidx.compose.ui.Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = androidx.compose.ui.Alignment.End)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text("Action Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(recentLogs) { log ->
                        Text(
                            "- ${log.content}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

