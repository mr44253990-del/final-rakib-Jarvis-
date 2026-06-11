package com.example.db

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()

    suspend fun insert(memory: Memory) {
        memoryDao.insertMemory(memory)
    }

    suspend fun delete(id: Int) {
        memoryDao.deleteMemory(id)
    }
}
