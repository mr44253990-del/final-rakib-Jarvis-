package com.example.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import com.example.db.Memory
import com.example.db.MemoryRepository
import com.example.gemini.Content
import com.example.gemini.GenerateContentRequest
import com.example.gemini.Part
import com.example.gemini.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ActionEngine(
    private val context: Context,
    private val memoryRepo: MemoryRepository
) {

    suspend fun processUserRequest(prompt: String, apiKey: String, modelName: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext "Please configure your Gemini API Key in Settings."

        val systemPrompt = """
            You are an advanced Android AI Assistant named "Rakib Jarvis".
            You have access to the user's phone.
            If the user asks to perform an action, output a JSON object in this exact format, wrapped in ```json ... ```:
            {
               "action": "ACTION_NAME",
               "target": "TARGET_INFO",
               "message": "Information to show the user"
            }
            
            Supported ACTIONS:
            - "CALL": target is the phone number.
            - "OPEN_APP": target is the app name (e.g., youtube).
            - "SET_ALARM": target is time in HH:MM format.
            - "SAVE_NOTE": target is the note text to save.
            - "CREATE_FILE": target is the file name, message is the file content.
            - "DELETE_FILE": target is the file name.
            - "GET_INFO": No specific action, just chatting.
            
            If it's just a conversation, just output text, NO JSON. 
            Keep your conversational text short, smart, and helpful.
        """.trimIndent()

        val url = "v1beta/models/${modelName.trim()}:generateContent"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(url, apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Jarvis."
            
            return@withContext executeParsedAction(responseText)
            
        } catch (e: Exception) {
            "Connection error or invalid configuration: ${e.message}"
        }
    }

    private suspend fun executeParsedAction(response: String): String {
        // Extract JSON if exists
        val jsonRegex = "```json([\\s\\S]*?)```".toRegex()
        val matchResult = jsonRegex.find(response)
        
        if (matchResult != null) {
            val jsonStr = matchResult.groupValues[1]
            try {
                val json = JSONObject(jsonStr)
                val action = json.getString("action")
                val target = json.getString("target")
                val message = json.optString("message", "Executing action...")
                
                when (action) {
                    "CALL" -> {
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$target")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                            memoryRepo.insert(Memory(type = "LOG", content = "Called $target"))
                        } catch (e: SecurityException) {
                            return "Missing CALL_PHONE permission."
                        }
                    }
                    "OPEN_APP" -> {
                        // simple fake resolution for youtube
                        val finalMsg = if (target.lowercase().contains("youtube")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                            "Opening YouTube..."
                        } else {
                            "Cannot find app: $target"
                        }
                        memoryRepo.insert(Memory(type = "LOG", content = "Tried to open app $target"))
                        return finalMsg
                    }
                    "SET_ALARM" -> {
                        val parts = target.split(":")
                        if (parts.size == 2) {
                            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                putExtra(AlarmClock.EXTRA_HOUR, parts[0].toIntOrNull() ?: 0)
                                putExtra(AlarmClock.EXTRA_MINUTES, parts[1].toIntOrNull() ?: 0)
                                putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis Alarm")
                                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            memoryRepo.insert(Memory(type = "LOG", content = "Set Alarm for $target"))
                        }
                    }
                    "SAVE_NOTE" -> {
                        memoryRepo.insert(Memory(type = "NOTE", content = target))
                        return "Note saved: $target"
                    }
                    "CREATE_FILE" -> {
                        try {
                            val file = java.io.File(context.filesDir, target)
                            file.writeText(message)
                            val logMsg = "Created File: $target"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return logMsg
                        } catch (e: Exception) {
                            return "Error creating file: ${e.message}"
                        }
                    }
                    "DELETE_FILE" -> {
                        try {
                            val file = java.io.File(context.filesDir, target)
                            if (file.exists() && file.delete()) {
                                val logMsg = "Deleted File: $target"
                                memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                                return logMsg
                            }
                            return "File not found or could not delete: $target"
                        } catch (e: Exception) {
                            return "Error deleting file: ${e.message}"
                        }
                    }
                }
                
                return message
            } catch (e: Exception) {
               return "Error parsing Jarvis action."
            }
        }
        
        return response
    }
}
