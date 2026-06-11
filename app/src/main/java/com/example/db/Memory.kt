package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "NOTE", "CONTACT", "TODO", "LOG"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
