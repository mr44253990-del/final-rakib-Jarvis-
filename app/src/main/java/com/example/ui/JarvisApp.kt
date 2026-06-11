package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.JarvisApplication
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.AnalyzeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ToolsScreen

@Composable
fun JarvisApp(app: JarvisApplication) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "chat") {
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
}
