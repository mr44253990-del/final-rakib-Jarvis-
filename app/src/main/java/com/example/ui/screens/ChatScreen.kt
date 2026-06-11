package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.JarvisApplication
import com.example.action.ActionEngine
import kotlinx.coroutines.launch

import com.example.assistant.JarvisVoiceController
import com.example.db.Memory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(app: JarvisApplication) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var localPendingPrompt by remember { mutableStateOf("") }
    var showPreviewPanel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val actionEngine = remember { ActionEngine(context, app.memoryRepository) }
    
    val apiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val modelName by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.5-flash")
    val memories by app.memoryRepository.allMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val chatMessagesSnapshot = remember(memories) {
        memories.filter { it.type == "CHAT_USER" || it.type == "CHAT_JARVIS" }.sortedBy { it.timestamp }
    }
    
    val recentLogs = memories.filter { it.type == "LOG" }.take(15)
    val savedNotes = memories.filter { it.type == "NOTE" }.take(15)

    // Dynamic list that includes persistent memory and active thinking bubbles
    val displayList = remember(chatMessagesSnapshot, localPendingPrompt) {
        val baseList = chatMessagesSnapshot
        if (localPendingPrompt.isNotBlank()) {
            baseList + listOf(
                Memory(type = "CHAT_USER", content = localPendingPrompt),
                Memory(type = "CHAT_JARVIS", content = "জার্ভিস চিন্তা করতেছে...")
            )
        } else {
            baseList
        }
    }

    // Voice Input State inside Chat screen for complete user comfort
    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceControllerInstance by remember { mutableStateOf<JarvisVoiceController?>(null) }

    val voiceController = remember {
        val controller = JarvisVoiceController(
            context = context,
            onTextRecognized = { text ->
                if (text.isNotBlank()) {
                    localPendingPrompt = text
                    scope.launch {
                        val reply = actionEngine.processUserRequest(text, apiKey ?: "", modelName)
                        localPendingPrompt = ""
                        voiceControllerInstance?.speak(reply)
                    }
                }
            },
            onListeningStatusChanged = { listening ->
                isVoiceListening = listening
            }
        )
        voiceControllerInstance = controller
        controller
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceController.destroy()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rakib Jarvis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isVoiceListening) "Listening..." else "Online • Ready", style = MaterialTheme.typography.labelSmall, color = if (isVoiceListening) MaterialTheme.colorScheme.error else Color(0xFF10B981))
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(8.dp).size(32.dp).clip(CircleShape).background(if (isVoiceListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                        /* Simulated Orb */
                        Box(modifier = Modifier.size(16.dp).align(Alignment.Center).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary))
                    }
                },
                actions = {
                    IconButton(onClick = { showPreviewPanel = !showPreviewPanel }) {
                        Icon(Icons.Default.History, contentDescription = "Toggle Action Preview", tint = if (showPreviewPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
            
            val listToDisplay = if (displayList.isEmpty()) {
                listOf(Memory(type = "CHAT_JARVIS", content = "হ্যালো স্যার, আমি রাকিব জার্ভিস। আজ আমি আপনাকে কীভাবে সাহায্য করতে পারি?"))
            } else {
                displayList
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listToDisplay) { memo ->
                    val isYou = memo.type == "CHAT_USER"
                    val align = if (isYou) Alignment.End else Alignment.Start
                    val color = if (isYou) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isYou) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(
                                    topStart = 16.dp, 
                                    topEnd = 16.dp, 
                                    bottomStart = if (isYou) 16.dp else 4.dp, 
                                    bottomEnd = if (isYou) 4.dp else 16.dp
                                ))
                                .background(color)
                                .padding(16.dp)
                        ) {
                            Text(memo.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            if (isVoiceListening) {
                                voiceController.stopListening()
                            } else {
                                voiceController.startListening()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = if (isVoiceListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Jarvis...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            val msg = message
                            if (msg.isNotBlank()) {
                                localPendingPrompt = msg
                                message = ""
                                scope.launch {
                                    val reply = actionEngine.processUserRequest(msg, apiKey ?: "", modelName)
                                    localPendingPrompt = ""
                                    voiceController.speak(reply)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showPreviewPanel,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(240.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Section 1: System Actions
                Text("System Logs (লাইভ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        if (recentLogs.isEmpty()) {
                            Text("No actions taken yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(recentLogs) { log ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                            Text("SYSTEM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(log.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Section 2: Saved Notes Live Preview! Fully responsive when note added/read!
                Text("Saved Notes (নোটসমূহ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(0.5f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        if (savedNotes.isEmpty()) {
                            Text("কোনো নোট সেভ করা নেই।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(savedNotes) { note ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                            Text("NOTE ID: ${note.id}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                            Text(note.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

