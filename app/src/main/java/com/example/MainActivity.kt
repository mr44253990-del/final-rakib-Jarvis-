package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.JarvisApp
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val app = application as JarvisApplication
                val firstLaunchCompleted by app.appSettings.firstLaunchCompletedFlow.collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                
                val permissionList = mutableListOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
                    permissionList.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                
                val permissionsState = rememberMultiplePermissionsState(permissions = permissionList)

                if (permissionsState.allPermissionsGranted || firstLaunchCompleted) {
                    JarvisApp(app)
                } else {
                    PermissionScreen(
                        permissionsState = permissionsState,
                        onContinue = {
                            scope.launch {
                                app.appSettings.completeFirstLaunch()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(permissionsState: MultiplePermissionsState, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(com.example.ui.theme.JarvisBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Permissions", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(color = com.example.ui.theme.JarvisSurfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = com.example.ui.theme.JarvisPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Rakib Jarvis needs following\npermissions to work properly", 
            textAlign = TextAlign.Center, 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val items = listOf("Storage", "Microphone", "Contacts", "Phone", "SMS", "Camera", "Location", "Notifications")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items.size) { i ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(items[i], color = MaterialTheme.colorScheme.onBackground)
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Skip", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { 
                    permissionsState.launchMultiplePermissionRequest()
                    onContinue() 
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.JarvisPrimary)
            ) {
                Text("Grant All", color = com.example.ui.theme.JarvisBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}
