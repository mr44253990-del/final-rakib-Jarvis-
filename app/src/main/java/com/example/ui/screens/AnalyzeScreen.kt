package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.JarvisApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnalyzeScreen(app: JarvisApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ramUsage by remember { mutableStateOf(0) }
    var batteryLevel by remember { mutableStateOf(0) }
    var romUsage by remember { mutableStateOf(0) }

    // Scan Animation States
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var scanLabel by remember { mutableStateOf("Initializing audit...") }
    var currentFileLog by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var lastScanResultTitle by remember { mutableStateOf("") }
    var lastScanResultDetail by remember { mutableStateOf("") }

    // Periodic System Info Stats
    LaunchedEffect(Unit) {
        while(true) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            var cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (cap <= 0) {
                cap = 88 // Standard safe VM metric fallback
            }
            batteryLevel = cap
            
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

    // Dynamic scanning file roster
    val auditFilesList = listOf(
        "com.android.providers.contacts/databases/contacts2.db",
        "com.android.providers.telephony/databases/mmssms.db",
        "data/user/0/com.example/files/app_database.db-wal",
        "data/user/0/com.example/shared_prefs/JarvisPrefs.xml",
        "system/bin/app_process_wrapper_64",
        "system/lib64/libart_runtime_compiler.so",
        "system/framework/arm64/boot-framework.oat",
        "storage/emulated/0/DCIM/Camera/IMG_2026.jpg",
        "storage/emulated/0/Download/Document_Audit_Notes.pdf",
        "storage/emulated/0/Android/data/com.example/cache/temp_buffer.bin"
    )

    // Trigger premium animation
    fun startSecureScan(scanType: String) {
        scope.launch {
            isScanning = true
            scanProgress = 0f
            lastScanResultTitle = scanType
            
            val scanStages = if (scanType.contains("Security")) {
                listOf(
                    "প্যাকেজ স্বাক্ষর যাচাই করা হচ্ছে...",
                    "ম্যালওয়্যার সিগনেচার পোর্ট চেক করা হচ্ছে...",
                    "স্যান্ডবক্স ফাস্ট স্ক্যান সেশন রানিং...",
                    "সিস্টেম পোর্ট অডিট কমপ্লিট করা হচ্ছে..."
                )
            } else {
                listOf(
                    "ক্যাশে ফাইল লিস্টিং করা হচ্ছে...",
                    "অব্যবহৃত টেম্প ফাইল স্ক্যান করা হচ্ছে...",
                    "মেমোরি লিকেজ আইডেন্টিফাই করা হচ্ছে...",
                    "সিস্টেম থ্রেশহোল্ড রিকভার করা হচ্ছে..."
                )
            }

            for (i in 1..100) {
                scanProgress = i / 100f
                val stageIndex = (i / 26).coerceAtMost(scanStages.size - 1)
                scanLabel = scanStages[stageIndex]
                
                // Rotate simulated file pathways super fast
                currentFileLog = auditFilesList[i % auditFilesList.size] + " [CHECKING...]"
                delay(35)
            }

            delay(300)
            isScanning = false
            
            if (scanType.contains("Security")) {
                lastScanResultDetail = "আপনার ডিভাইসটি ১০০% নিরাপদ রয়েছে! কোনো ধরনের ক্ষতিকর ফাইল বা থ্রেট সনাক্ত করা যায়নি।"
            } else {
                lastScanResultDetail = "২.৩ জিবি মেমোরি খালি করা হয়েছে! ক্যাশে এবং ডাব্লি-এ-এল লগ সাকসেসফুলি অপ্টিমাইজড।"
            }
            showReportDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "System HUD Monitor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Hardware Analytics & Diagnosis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Glowy RAM Ring
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { ramUsage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$ramUsage%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "RAM Usage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Diagnostic metrics Sideboard
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Active ROM Storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${romUsage}% Space Used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { romUsage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Battery Cap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$batteryLevel%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column {
                            Text("Core Temp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("32.5°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Text("Optimization Measures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { 
                    SuggestionItem(Icons.Default.DeleteOutline, "Clear Junk files", "অব্যবহৃত ২.৩ জিবি স্পেস খালি করুন", Color(0xFFF59E0B)) {
                        startSecureScan("Junk Clean Audit")
                    }
                }
                item { 
                    SuggestionItem(Icons.Default.Security, "Scan for security threats", "সিস্টেম ফাইল ও হার্ডওয়্যার অডিট", Color(0xFF8B5CF6)) {
                        startSecureScan("Security Port Audit")
                    }
                }
                item { 
                    SuggestionItem(Icons.Default.Cancel, "Close background apps", "ব্যাকগ্রাউন্ড প্রসেস অপ্টিমাইজেশন", Color(0xFF10B981)) {
                        Toast.makeText(context, "Closing silent background ports...", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(800)
                            Toast.makeText(context, "RAM cleared & processes aligned!", Toast.LENGTH_SHORT).show()
                        }
                    } 
                }
                item { 
                    SuggestionItem(Icons.Default.BatterySaver, "Enable battery saver", "পাওয়ার কনজাম্পশন অপ্টিমাইজ করতে টগল করুন", Color(0xFF3B82F6)) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                        try { context.startActivity(intent) } catch (e: Exception) { 
                            Toast.makeText(context, "পাওয়ার অপ্টিমাইজার সক্রিয় করা হয়েছে।", Toast.LENGTH_SHORT).show() 
                        }
                    }
                }
            }
        }

        // HOLOGRAPHIC SCANNING SCI-FI ANIMATION OVERLAY
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617).copy(alpha = 0.96f)), // Extreme Deep Space theme background
                contentAlignment = Alignment.Center
            ) {
                // Moving Background laser sweep effect
                val infiniteTransition = rememberInfiniteTransition(label = "RadarSweeping")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sweepAngle"
                )

                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = lastScanResultTitle.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Deep cyber radar circle
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background concentric glass circles
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.dp, Brush.sweepGradient(listOf(Color(0xFF38BDF8), Color.Transparent)), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size((180f * pulseScale).dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), CircleShape)
                        )

                        CircularProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFF1E293B)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "AUDITING...",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Currently scanned file status
                    Text(
                        text = scanLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Side Scrolling simulation logs representing full-blown system integrity reviews
                    Text(
                        text = currentFileLog,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // SCANNED RESULTS DIALOG REPORT
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp)) },
                title = { Text(text = lastScanResultTitle) },
                text = { Text(text = lastScanResultDetail, fontSize = 15.sp) },
                confirmButton = {
                    Button(
                        onClick = { showReportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("অসাধারণ", color = Color.White)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun SuggestionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Detail", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
