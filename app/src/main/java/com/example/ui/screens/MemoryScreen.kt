package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.JarvisApplication
import com.example.db.Memory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(app: JarvisApplication) {
    val memories by app.memoryRepository.allMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val filters = listOf("All", "Calls", "Notes", "Files", "Web", "Log")
    var selectedFilter by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var newNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Memory") },
            text = {
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    label = { Text("Memory Content") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNote.isNotBlank()) {
                        scope.launch { app.memoryRepository.insert(Memory(type = "NOTE", content = newNote)) }
                        Toast.makeText(context, "Memory Added", Toast.LENGTH_SHORT).show()
                    }
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Memory Center", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search in memory...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                filter,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add New Memory") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            val filteredList = memories.filter { 
                (selectedFilter == "All" || it.type == selectedFilter.uppercase()) &&
                (searchQuery.isBlank() || it.content.contains(searchQuery, ignoreCase = true))
            }
            if (filteredList.isEmpty() && memories.isNotEmpty()) {
               item { Text("No memories match filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            
            // Mock static items to match UI if empty
            if (memories.isEmpty()) {
                item { MemoryItem(Icons.Default.Person, "Saved Contact", "Rakib Hasan - 017XXX....", "Today, 10:30 AM", Color(0xFF3B82F6)) {} }
                item { MemoryItem(Icons.Default.Note, "Note", "Meeting with team at 5 PM", "Today, 09:15 AM", Color(0xFFEAB308)) {} }
                item { MemoryItem(Icons.Default.Alarm, "Reminder", "Buy groceries", "Today, 08:00 AM", Color(0xFFFF453A)) {} }
                item { MemoryItem(Icons.Default.InsertDriveFile, "File", "Project_Report.pdf", "Yesterday, 11:45 PM", Color(0xFFEC4899)) {} }
                item { MemoryItem(Icons.Default.Language, "Website", "Flutter Documentation", "Yesterday, 09:30 PM", Color(0xFF3B82F6)) {} }
            }

            items(filteredList, key = { it.id }) { memo ->
                MemoryItem(
                    icon = Icons.Default.Memory,
                    title = memo.type,
                    subtitle = memo.content,
                    time = "ID: ${memo.id}",
                    iconColor = MaterialTheme.colorScheme.primary,
                    onDelete = {
                        scope.launch { app.memoryRepository.delete(memo.id) }
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, time: String, iconColor: Color, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
