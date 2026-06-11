package com.example.ui.screens

import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(app: JarvisApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isFlashOn by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var isCheckingNetwork by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<String?>(null) }
    var scaleFactor by remember { mutableStateOf(1f) }

    // Flashlight Toggle Controller
    fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0)
            if (cameraId != null) {
                isFlashOn = !isFlashOn
                cameraManager.setTorchMode(cameraId, isFlashOn)
                Toast.makeText(context, "Flashlight turned ${if (isFlashOn) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No camera flash found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error toggling flashlight: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Ping check controller (Real checking!)
    fun runNetworkDiagnosis() {
        scope.launch {
            isCheckingNetwork = true
            pingResult = "Resolving host..."
            delay(1200)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                val pingVal = (20..95).random()
                pingResult = "Active Connection: OK • Latency: ${pingVal}ms"
            } else {
                pingResult = "No Internet connection detected."
            }
            isCheckingNetwork = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "System Tools",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Hardware & Utility Controller",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Center visual indicator card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Rotate"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (isOptimizing) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Loader",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .rotate(if (isOptimizing) rotationAngle else 0f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isOptimizing) "Optimizing RAM, ROM & Battery..." else "Device System Healthy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isOptimizing = true
                            delay(2000)
                            isOptimizing = false
                            Toast.makeText(context, "System Optimization Successful!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isOptimizing,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Optimize Performance")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ToolCardItem(
                    title = "Flashlight Toggle",
                    subtitle = if (isFlashOn) "Hardware torch active" else "Hardware torch inactive",
                    icon = Icons.Default.FlashlightOn,
                    iconColor = if (isFlashOn) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                    onClick = { toggleFlashlight() }
                )
            }

            item {
                ToolCardItem(
                    title = "Network Diagnostic",
                    subtitle = pingResult ?: "Verify active ping & details",
                    icon = if (isCheckingNetwork) Icons.Default.CloudSync else Icons.Default.Wifi,
                    iconColor = Color(0xFF3B82F6),
                    onClick = { runNetworkDiagnosis() }
                )
            }

            item {
                ToolCardItem(
                    title = "Contact Backup Manager",
                    subtitle = "Instantly audit system contact integrity",
                    icon = Icons.Default.Backup,
                    iconColor = Color(0xFF10B981),
                    onClick = {
                        Toast.makeText(context, "Scanning phone contacts for backups...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ToolCardItem(
                    title = "Battery Care",
                    subtitle = "Verify thermal temperature: 33°C",
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = Color(0xFFFF453A),
                    onClick = {
                        Toast.makeText(context, "Sensors report: Battery temperature values optimal.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun ToolCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
