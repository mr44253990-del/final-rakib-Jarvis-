package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.JarvisApplication

@Composable
fun HomeScreen(app: JarvisApplication, onNavigateToChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rakib Jarvis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Online", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(32.dp))

        // Center Orb / Face
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // A pseudo glowing orb (just a bordered circle for now)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Hello, Rakib \uD83D\uDC4B", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("How can I help you today?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        // Actions Grid
        val actions = listOf(
            Triple("Call", Icons.Default.Call, Color(0xFF10B981)),
            Triple("Message", Icons.Default.Message, Color(0xFFF59E0B)),
            Triple("File Search", Icons.Default.Search, Color(0xFF3B82F6)),
            Triple("YouTube", Icons.Default.PlayArrow, Color(0xFFFF453A)),
            Triple("Open App", Icons.Default.Apps, Color(0xFF8B5CF6)),
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
                        .clickable(onClick = onNavigateToChat)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.second, contentDescription = action.first, tint = action.third, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(action.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Assistant Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = "Keyboard", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onNavigateToChat))
            
            // Microphone Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onNavigateToChat),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Speak", tint = MaterialTheme.colorScheme.onPrimary)
            }
            
            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onNavigateToChat))
        }
    }
}
