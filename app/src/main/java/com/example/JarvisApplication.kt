package com.example

import android.app.Application
import com.example.config.AppSettings
import com.example.db.AppDatabase
import com.example.db.MemoryRepository

class JarvisApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val memoryRepository by lazy { MemoryRepository(database.memoryDao()) }
    val appSettings by lazy { AppSettings(this) }
}
