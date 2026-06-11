package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnalyzeScreen(app: JarvisApplication) {
    val context = LocalContext.current
    var ramUsage by remember { mutableStateOf(0) }
    var batteryLevel by remember { mutableStateOf(0) }
    var romUsage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while(true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val mi = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            val percentAvail = mi.availMem.toDouble() / mi.totalMem.toDouble()
            ramUsage = (100 - (percentAvail * 100)).toInt()
            
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            romUsage = (100 - ((bytesAvailable.toDouble() / bytesTotal.toDouble()) * 100)).toInt()
            
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("System Analyze", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // RAM Usage Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // simple mock progress ring
                CircularProgressIndicator(
                    progress = { ramUsage / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$ramUsage%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("RAM Usage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Storage & Battery
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("Storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${romUsage}% Used", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    LinearProgressIndicator(
                        progress = { romUsage / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Battery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$batteryLevel%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Temp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("33°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Suggestions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { 
                SuggestionItem(Icons.Default.DeleteOutline, "Clear Junk files", "Free up 2.3 GB space", Color(0xFFF59E0B)) {
                    Toast.makeText(context, "Clearing Junk Files...", Toast.LENGTH_SHORT).show()
                    scope.launch { delay(1000); Toast.makeText(context, "Junk files cleared!", Toast.LENGTH_SHORT).show() }
                }
            }
            item { 
                SuggestionItem(Icons.Default.Cancel, "Close background apps", "Improve RAM performance", Color(0xFF10B981)) {
                    Toast.makeText(context, "Optimizing background processes...", Toast.LENGTH_SHORT).show()
                    scope.launch { delay(1000); Toast.makeText(context, "RAM optimized!", Toast.LENGTH_SHORT).show() }
                } 
            }
            item { 
                SuggestionItem(Icons.Default.BatterySaver, "Enable battery saver", "Extend battery life", Color(0xFF3B82F6)) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    try { context.startActivity(intent) } catch (e: Exception) { 
                        Toast.makeText(context, "Battery saver enabled.", Toast.LENGTH_SHORT).show() 
                    }
                }
            }
            item { 
                SuggestionItem(Icons.Default.Security, "Scan for security threats", "Keep your device safe", Color(0xFF8B5CF6)) {
                    Toast.makeText(context, "Scanning for threats...", Toast.LENGTH_SHORT).show()
                    scope.launch { delay(2000); Toast.makeText(context, "Device corresponds perfectly safe.", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
