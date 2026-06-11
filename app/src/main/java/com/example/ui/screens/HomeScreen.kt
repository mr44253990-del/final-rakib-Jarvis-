package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import com.example.action.ActionEngine
import com.example.assistant.JarvisVoiceController
import androidx.compose.ui.graphics.asImageBitmap
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(app: JarvisApplication, onNavigateToChat: () -> Unit) {
    var showChatSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val apiKey by app.appSettings.apiKeyFlow.collectAsState(initial = "")
    val modelName by app.appSettings.modelNameFlow.collectAsState(initial = "gemini-3.5-flash")
    
    val actionEngine = remember { ActionEngine(context, app.memoryRepository) }
    
    var assistantResponse by remember { mutableStateOf("") }
    var assistantLoading by remember { mutableStateOf(false) }
    
    // Voice State variables
    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("") }
    
    var voiceControllerInstance by remember { mutableStateOf<JarvisVoiceController?>(null) }
    
    // Voice controller initialization
    val voiceController = remember {
        val controller = JarvisVoiceController(
            context = context,
            onTextRecognized = { text ->
                voiceInputText = text
                if (text.isNotBlank()) {
                    scope.launch {
                        assistantLoading = true
                        assistantResponse = "Analyzing voice command: \"$text\"..."
                        val reply = actionEngine.processUserRequest(text, apiKey ?: "", modelName)
                        assistantResponse = reply
                        assistantLoading = false
                        voiceControllerInstance?.speak(reply) // Speak back!
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
    
    // Keyboard Input State
    var showKeyboardInput by remember { mutableStateOf(false) }
    var typedPrompt by remember { mutableStateOf("") }

    // Camera Capture State
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCameraPromptDialog by remember { mutableStateOf(false) }
    var cameraPromptText by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            cameraPromptText = "Describe this captured image"
            showCameraPromptDialog = true
        }
    }

    if (showCameraPromptDialog && capturedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showCameraPromptDialog = false },
            title = { Text("Analyze Photo with Jarvis") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured snap",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OutlinedTextField(
                        value = cameraPromptText,
                        onValueChange = { cameraPromptText = it },
                        label = { Text("Ask Jarvis about this picture") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prompt = cameraPromptText
                        val bitmap = capturedBitmap
                        showCameraPromptDialog = false
                        if (bitmap != null) {
                            scope.launch {
                                assistantLoading = true
                                assistantResponse = "Uploading captured vision snap..."
                                val reply = actionEngine.processUserRequest(prompt, apiKey ?: "", modelName, bitmap)
                                assistantResponse = reply
                                assistantLoading = false
                                voiceController.speak(reply)
                            }
                        }
                    }
                ) {
                    Text("Submit Vision Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPromptDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            ChatScreen(app = app)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rakib Jarvis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Online • Voice Enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))

        // Large Assistant Orb Clickable
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    if (isVoiceListening) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable {
                    if (isVoiceListening) {
                        voiceController.stopListening()
                    } else {
                        voiceInputText = ""
                        voiceController.startListening()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (isVoiceListening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(110.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 4.dp
                    )
                    Text("Listening...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else if (assistantLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(110.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Assistant active text readout Card
        if (assistantResponse.isNotBlank() || voiceInputText.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (voiceInputText.isNotBlank()) {
                        Text(
                            text = "You said: \"$voiceInputText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (assistantResponse.isNotBlank()) {
                        Text(
                            text = assistantResponse,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val actions = listOf(
            Triple("Call", Icons.Default.Call, Color(0xFF10B981)),
            Triple("Message", Icons.Default.Message, Color(0xFFF59E0B)),
            Triple("File Search", Icons.Default.Search, Color(0xFF3B82F6)),
            Triple("YouTube", Icons.Default.PlayArrow, Color(0xFFFF453A)),
            Triple("Web", Icons.Default.Language, Color(0xFF8B5CF6)),
            Triple("Music", Icons.Default.MusicNote, Color(0xFFEC4899)),
            Triple("Note", Icons.Default.Note, Color(0xFFEAB308)),
            Triple("More", Icons.Default.MoreHoriz, Color(0xFF94A3B8))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(actions.size) { index ->
                val action = actions[index]
                Column(
                    modifier = Modifier
                        .clickable { 
                            when(action.first) {
                                "Call" -> context.startActivity(Intent(Intent.ACTION_DIAL))
                                "Message" -> context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
                                "YouTube" -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                                "Music" -> context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC))
                                "Web" -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                                else -> showChatSheet = true 
                            }
                        }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.second, contentDescription = action.first, tint = action.third, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(action.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Direct Text Input overlay
        if (showKeyboardInput) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedPrompt,
                    onValueChange = { typedPrompt = it },
                    placeholder = { Text("Ask Jarvis anything...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        val prompt = typedPrompt
                        if (prompt.isNotBlank()) {
                            typedPrompt = ""
                            showKeyboardInput = false
                            scope.launch {
                                assistantLoading = true
                                assistantResponse = "Processing command..."
                                val reply = actionEngine.processUserRequest(prompt, apiKey ?: "", modelName)
                                assistantResponse = reply
                                assistantLoading = false
                                voiceController.speak(reply)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Submit", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Keyboard, 
                contentDescription = "Keyboard", 
                tint = if (showKeyboardInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.clickable { showKeyboardInput = !showKeyboardInput }
            )
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isVoiceListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    .clickable { 
                        if (isVoiceListening) {
                            voiceController.stopListening()
                        } else {
                            voiceInputText = ""
                            voiceController.startListening()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic, 
                    contentDescription = "Speak", 
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Icon(
                Icons.Default.CameraAlt, 
                contentDescription = "Camera", 
                tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.clickable { 
                    try {
                        cameraLauncher.launch(null)
                    } catch(e: Exception) {
                        Toast.makeText(context, "Error launching Camera app", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
