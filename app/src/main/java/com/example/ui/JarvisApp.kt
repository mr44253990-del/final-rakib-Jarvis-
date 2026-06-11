package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.JarvisApplication
import com.example.action.ActionEngine
import com.example.assistant.JarvisVoiceController
import com.example.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun JarvisApp(
    app: JarvisApplication,
    isAssistTriggered: Boolean = false,
    onDismissAssist: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Config Snapshot
    val apiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val modelName by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.5-flash")
    val actionEngine = remember { ActionEngine(context, app.memoryRepository) }

    // Bottom Assist Voice states
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var voiceResponse by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var voiceControllerInstance by remember { mutableStateOf<JarvisVoiceController?>(null) }

    // Init voice recognition specifically when Assist mode triggers
    LaunchedEffect(isAssistTriggered) {
        if (isAssistTriggered) {
            recognizedText = "বলুন, আমি শুনছি..."
            voiceResponse = ""
            isProcessing = false
            
            // Re-initialize for priority clean capture
            voiceControllerInstance?.destroy()
            voiceControllerInstance = JarvisVoiceController(
                context = context,
                onTextRecognized = { text ->
                    if (text.isNotBlank()) {
                        recognizedText = text
                        scope.launch {
                            isProcessing = true
                            voiceResponse = "প্রসেস করছি..."
                            val reply = actionEngine.processUserRequest(text, apiKey ?: "", modelName)
                            voiceResponse = reply
                            isProcessing = false
                            voiceControllerInstance?.speak(reply)
                        }
                    }
                },
                onListeningStatusChanged = { active ->
                    isListening = active
                }
            )
            // Trigger auto listening instantly to simulate Google Assistant!
            try {
                voiceControllerInstance?.startListening()
            } catch (e: Exception) {
                Toast.makeText(context, "Microphone access setup: " + e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            voiceControllerInstance?.destroy()
            voiceControllerInstance = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute != "chat" && !isAssistTriggered) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = "Memory") },
                            label = { Text("Memory") },
                            selected = currentRoute == "memory",
                            onClick = { navController.navigate("memory") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analyze") },
                            label = { Text("Analyze") },
                            selected = currentRoute == "analyze",
                            onClick = { navController.navigate("analyze") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                            label = { Text("Tools") },
                            selected = currentRoute == "tools",
                            onClick = { navController.navigate("tools") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") { HomeScreen(app, onNavigateToChat = { navController.navigate("chat") }) }
                composable("analyze") { AnalyzeScreen(app) }
                composable("chat") { ChatScreen(app) }
                composable("tools") { ToolsScreen(app) }
                composable("memory") { MemoryScreen(app) }
                composable("settings") { SettingsScreen(app) }
            }
        }

        // SLIDING BOTTOM SHEET FOR HOLD-PRESS DEFAULT ASSISTANT FUNCTIONALITY
        AnimatedVisibility(
            visible = isAssistTriggered,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF0F172A)) // Sleek Twilight Theme Background
                    .padding(24.dp)
            ) {
                // Background Glowing Aura effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Notch / Grab Bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF475569))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RAKIB ASSISTANT IN ACTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        textAlign = TextAlign.Center
                    )

                    // Live Speech Transcription / State Title
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = recognizedText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (voiceResponse.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = voiceResponse,
                                fontSize = 16.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // PULSING GLOWING ORB ANIMATION
                    val infiniteTransition = rememberInfiniteTransition(label = "AssistOrb")
                    val orbScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "GlowOrbScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing waves
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isListening) {
                                            listOf(Color(0xFF10B981), Color(0xFF3B82F6))
                                        } else if (isProcessing) {
                                            listOf(Color(0xFFF59E0B), Color(0xFFEC4899))
                                        } else {
                                            listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                        }
                                    )
                                )
                                .animateContentSize()
                        )

                        // Pulse Ring
                        Box(
                            modifier = Modifier
                                .size((64.dp.value * orbScale).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Center Microphone Icon
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    voiceControllerInstance?.stopListening()
                                } else {
                                    voiceControllerInstance?.startListening()
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "Voice mic trigger",
                                tint = if (isListening) Color(0xFF10B981) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                voiceResponse = ""
                                recognizedText = "বলুন, আমি শুনছি..."
                                voiceControllerInstance?.startListening()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("আবার বলুন", color = Color.White)
                        }

                        Button(
                            onClick = {
                                voiceControllerInstance?.stopListening()
                                onDismissAssist()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("বন্ধ করুন", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
