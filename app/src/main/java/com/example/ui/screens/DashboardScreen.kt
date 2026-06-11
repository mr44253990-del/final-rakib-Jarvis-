package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.JarvisApplication
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(app: JarvisApplication) {
    val context = LocalContext.current
    var ramUsage by remember { mutableStateOf(0) }
    var batteryLevel by remember { mutableStateOf(0) }
    var romUsage by remember { mutableStateOf(0) }
    
    val memories by app.memoryRepository.allMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentLogs = memories.filter { it.type == "LOG" }.take(10)

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
        Text("System Status", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatusCard(title = "RAM", value = "$ramUsage%")
            StatusCard(title = "ROM", value = "$romUsage%")
            StatusCard(title = "Battery", value = "$batteryLevel%")
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Recent Activity Logs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        
        LazyColumn {
            items(recentLogs) { log ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(log.content, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatusCard(title: String, value: String) {
    Card(modifier = Modifier.size(100.dp, 80.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
