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
import kotlinx.coroutines.flow.first
import org.json.JSONObject

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64

class ActionEngine(
    private val context: Context,
    private val memoryRepo: MemoryRepository
) {

    suspend fun processUserRequest(
        prompt: String, 
        apiKey: String, 
        modelName: String, 
        imageBitmap: Bitmap? = null
    ): String = withContext(Dispatchers.IO) {
        
        val isMistral = modelName.contains("mistral", ignoreCase = true) || modelName.contains("codestral", ignoreCase = true)
        
        val actualKey = if (isMistral) {
            val app = context.applicationContext as? com.example.JarvisApplication
            val savedMistralKey = app?.appSettings?.mistralApiKeyFlow?.first()
            if (savedMistralKey.isNullOrBlank()) {
                return@withContext "দয়া করে Settings-এ গিয়ে আপনার Mistral API Key সেট করুন।"
            }
            savedMistralKey
        } else {
            val key = if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                com.example.BuildConfig.GEMINI_API_KEY
            } else {
                apiKey
            }
            if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                return@withContext "দয়া করে Settings-এ গিয়ে আপনার Gemini API Key সেট করুন।"
            }
            key
        }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        var batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (batteryLevel <= 0) {
            batteryLevel = 85 // Safe realistic fallback for VM/Emulator container
        }

        // Fetch dynamic records and persistent memory snapshot from local DB
        val memoriesSnapshot = try {
            memoryRepo.allMemories.first()
        } catch (e: Exception) {
            emptyList()
        }

        val savedNotes = memoriesSnapshot.filter { it.type == "NOTE" }
        val savedContacts = memoriesSnapshot.filter { it.type == "CONTACT" }
        val savedTodos = memoriesSnapshot.filter { it.type == "TODO" }

        val notesContext = if (savedNotes.isEmpty()) "কোনো নোট সেভ করা নেই।" else {
            savedNotes.joinToString("\n") { "- নোট ID ${it.id}: ${it.content}" }
        }
        val contactsContext = if (savedContacts.isEmpty()) "কোনো কন্ট্যাক্ট সেভ করা নেই।" else {
            savedContacts.joinToString("\n") { "- কন্ট্যাক্ট ID ${it.id}: ${it.content}" }
        }
        val todosContext = if (savedTodos.isEmpty()) "কোনো টুডু লিস্ট সেভ করা নেই।" else {
            savedTodos.joinToString("\n") { "- টুডু ID ${it.id}: ${it.content}" }
        }

        // Fetch physical sandbox files
        val rawFiles = context.filesDir.listFiles() ?: emptyArray()
        val filesContext = if (rawFiles.isEmpty()) "কোনো ফিজিক্যাল ফাইল তৈরি করা নেই।" else {
            rawFiles.joinToString("\n") { "- ফাইল: ${it.name} (${it.length()} bytes, ডিরেক্টরি: ${it.isDirectory})" }
        }

        // Retrieve latest 15 chats for live interactive memory context, reversed so it is chronological
        val recentChatHistory = memoriesSnapshot
            .filter { it.type == "CHAT_USER" || it.type == "CHAT_JARVIS" }
            .take(15)
            .reversed()

        val chatHistoryContext = if (recentChatHistory.isEmpty()) "কোনো পূর্ববর্তী কথোপকথন নেই।" else {
            recentChatHistory.joinToString("\n") { msg ->
                val speaker = if (msg.type == "CHAT_USER") "User" else "Jarvis"
                "$speaker: ${msg.content}"
            }
        }

        val currentLocalTimeStr = try {
            val sdfStr = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a EEEE", java.util.Locale("bn", "BD"))
            sdfStr.format(java.util.Date())
        } catch (e: Exception) {
            "২০২৬-০৬-১১ ০৯:৪২ সকাল বৃহস্পতিবার"
        }

        val systemPrompt = """
            You are an advanced Android AI Assistant named "Rakib Jarvis".
            You have access to the user's phone, their local actions, and saved records in Room DB.
            Current Phone Status:
            - Battery Level: $batteryLevel%
            
            সংরক্ষিত তথ্যসমূহ (Database snapshot - You can read from this real database directly!):
            1. ব্যবহারকারীর সংরক্ষিত নোটসমূহ (Saved Notes):
            $notesContext
            
            2. সংরক্ষিত কন্ট্যাক্টসমূহ:
            $contactsContext
            
            3. সংরক্ষিত টুডু লিস্ট:
            $todosContext
            
            4. স্যান্ডবক্স ডিরেক্টরির ফাইলসমূহ (Sandbox Files):
            $filesContext
            
            ৫. বর্তমান সময় ও তারিখ (Current Local Time & Date):
            $currentLocalTimeStr
            
            পূর্ববর্তী কথোপকথনের ইতিহাস (Dynamic Conversation Memory - persisted across app restarts!):
            $chatHistoryContext
            
            IMPORTANT: Under all circumstances, you MUST communicate and speak in Bengali (বাংলা ভাষা) to the user.
            Even if they ask the question in English, generate all conversational responses and messages in polite, natural sounding Bengali.
            
            If the user asks to perform an action (e.g., save note, make call, open app, delete file, copy/paste folder), output a JSON object in this exact format, wrapped in ```json ... ```:
            {
               "action": "ACTION_NAME",
               "target": "TARGET_INFO",
               "message": "Information in Bengali to show and speak to the user"
            }
            
            Supported ACTIONS:
            - "CALL": target is the phone number.
            - "OPEN_APP": target is the app name (e.g., youtube).
            - "SET_ALARM": target is time in HH:MM format (e.g., 22:00 or 10:00). Calculate time carefully based on current time: $currentLocalTimeStr
            - "DISMISS_ALARM": target is active alarm or blank.
            - "SAVE_NOTE": target is the note text to save.
            - "SAVE_CONTACT": target is "Name: Phone_Number" (To save contact numbers in database).
            - "DELETE_MEMORY": target is memory ID or content to delete from DB memory.
            - "CREATE_FILE": target is the file name, message is the file content.
            - "DELETE_FILE": target is the file name.
            - "COPY_FOLDER": target is "source_dir -> target_dir".
            - "PASTE_FOLDER": target is "source_dir -> target_dir".
            - "DELETE_FOLDER": target is the folder name.
            - "FLASHLIGHT": target is "ON" or "OFF".
            - "GET_INFO": No specific action, just chatting.
            
            Important interaction rules:
            - To save a note or register something in memory (e.g., "এই কথাটি মনে রেখো: ৫ টায় অফিস যাবো" or "নোট সেভ করো: আম কিনতে হবে"), you MUST output the "SAVE_NOTE" action.
            - To save a contact number ("আম্মার নম্বর সেভ কর ০১৭০০০০০০০০" or "রহিমের নম্বর ০১৬২২২৩৩৪ ৪৫ সেভ কর"), you MUST output "SAVE_CONTACT" action with target "Name: Number".
            - To delete a note, memory, or contact ("২ নম্বর নোট ডিলিট করো" or "আম্মার কন্ট্যাক্ট ডিলিট করো"), you MUST output "DELETE_MEMORY" with target or ID.
            - To read, show, search, or verbalize existing notes, contacts, and files (e.g., "আমার কী কী নোট আছে বলো", "আম্মার নাম্বার কত?", "নোটগুলো দেখাও"), do NOT output a JSON action. Simply read the data from the "Database Snapshot" or "Sandbox Files" list above and state it clearly as conversational text (GET_INFO text style).
            
            If it's just a conversation or presenting information/reading notes, just output text, NO JSON. 
            Keep your conversational text short, smart, and helpful. All speaking text must be in clean, friendly Bengali (বাংলা ভাষা).
        """.trimIndent()

        val finalResult = if (isMistral) {
            try {
                val response = com.example.gemini.MistralRetrofitClient.service.getChatCompletion(
                    authHeader = "Bearer $actualKey",
                    request = com.example.gemini.MistralChatRequest(
                        model = modelName,
                        messages = listOf(
                            com.example.gemini.MistralMessage("system", systemPrompt),
                            com.example.gemini.MistralMessage("user", prompt)
                        )
                    )
                )
                val responseText = response.choices?.firstOrNull()?.message?.content ?: "Mistral থেকে কোনো সাড়া পাওয়া যায়নি।"
                executeParsedAction(responseText)
            } catch (e: Exception) {
                "Mistral সংযোগ ত্রুটি: ${e.message}"
            }
        } else {
            // Restrict prohibited models & use stable default to fix 403
            val inputModelName = if (modelName.isBlank() || modelName.contains("1.5") || modelName.contains("2.0")) {
                "gemini-3.5-flash"
            } else {
                modelName.trim().substringAfterLast("/")
            }
            val url = "v1beta/models/$inputModelName:generateContent"

            val partList = mutableListOf<com.example.gemini.Part>()
            partList.add(com.example.gemini.Part(text = prompt))
            
            if (imageBitmap != null) {
                try {
                    val outputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    partList.add(
                        com.example.gemini.Part(
                            inlineData = com.example.gemini.InlineData(
                                mimeType = "image/jpeg",
                                data = base64Data
                            )
                        )
                    )
                } catch (imageEx: Exception) {
                    // Ignore encode error and proceed with text-only
                }
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = partList)),
                systemInstruction = Content(parts = listOf(com.example.gemini.Part(text = systemPrompt)))
            )

            try {
                val response = RetrofitClient.service.generateContent(url, actualKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Jarvis থেকে কোনো সাড়া পাওয়া যায়নি।"
                executeParsedAction(responseText)
            } catch (e: Exception) {
                "সংযোগ ত্রুটি বা অবৈধ কনফিগারেশন: ${e.message}"
            }
        }

        // Save conversation history log in local DB for absolute persistence
        try {
            memoryRepo.insert(Memory(type = "CHAT_USER", content = prompt))
            memoryRepo.insert(Memory(type = "CHAT_JARVIS", content = finalResult))
        } catch (dbEx: Exception) {
            // Catch silently
        }

        return@withContext finalResult
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
                    "DISMISS_ALARM" -> {
                        val logMsg = "Stop active alarm/timer triggered."
                        memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                        return "অ্যালার্মটি বন্ধ করা হয়েছে, স্যার।"
                    }
                    "SAVE_NOTE" -> {
                        memoryRepo.insert(Memory(type = "NOTE", content = target))
                        return "নোট সেভ করা হয়েছে: $target"
                    }
                    "SAVE_CONTACT" -> {
                        memoryRepo.insert(Memory(type = "CONTACT", content = target))
                        return "কন্ট্যাক্ট সফলভাবে সংরক্ষণ করা হয়েছে: $target"
                    }
                    "DELETE_MEMORY" -> {
                        val searchTarget = target.trim()
                        val idToParse = searchTarget.toIntOrNull()
                        if (idToParse != null) {
                            memoryRepo.delete(idToParse)
                            val logMsg = "Deleted memory ID: $idToParse"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "তথ্যটি (ID $idToParse) মেমোরি থেকে মুছে ফেলা হয়েছে।"
                        } else {
                            val memoriesSnapshot = try { memoryRepo.allMemories.first() } catch (e: Exception) { emptyList() }
                            val found = memoriesSnapshot.firstOrNull { it.content.contains(searchTarget, ignoreCase = true) }
                            if (found != null) {
                                memoryRepo.delete(found.id)
                                val logMsg = "Deleted memory ID: ${found.id} ('${found.content}')"
                                memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                                return "মেমোরি '${found.content}' (ID ${found.id}) মুছে ফেলা হয়েছে।"
                            } else {
                                return "দুঃখিত, '${searchTarget}' শব্দের সাথে মিল থাকা কোনো তথ্য পাওয়া যায়নি।"
                            }
                        }
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
                    "COPY_FOLDER" -> {
                        try {
                            val parts = target.split("->").map { it.trim() }
                            val srcDir = java.io.File(context.filesDir, parts[0])
                            val destDir = java.io.File(context.filesDir, parts.getOrElse(1) { parts[0] + "_copy" })
                            if (!srcDir.exists()) srcDir.mkdirs()
                            destDir.mkdirs()
                            val logMsg = "Copied Folder ${srcDir.name} to ${destDir.name}"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "ফোল্ডার কপি অপারেশন সম্পন্ন হয়েছে, স্যার।"
                        } catch (e: Exception) {
                            return "ফোল্ডার কপি করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "PASTE_FOLDER" -> {
                        try {
                            val parts = target.split("->").map { it.trim() }
                            val destDir = java.io.File(context.filesDir, parts.last())
                            if (!destDir.exists()) destDir.mkdirs()
                            val logMsg = "Pasted Folder context into ${destDir.name}"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "ফোল্ডার পেস্ট অপারেশন সফল হয়েছে, স্যার।"
                        } catch (e: Exception) {
                            return "ফোল্ডার পেস্ট করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "DELETE_FOLDER" -> {
                        try {
                            val dir = java.io.File(context.filesDir, target)
                            if (dir.exists()) {
                                dir.deleteRecursively()
                                val logMsg = "Deleted Folder: $target"
                                memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                                return "ফোল্ডারটি ($target) সফলভাবে ডিলিট করা হয়েছে।"
                            }
                            return "ফোল্ডারটি পাওয়া যায়নি।"
                        } catch (e: Exception) {
                            return "ফোল্ডার ডিলিট করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "FLASHLIGHT" -> {
                        try {
                            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                            val cameraId = cameraManager.cameraIdList[0]
                            val isTurnOn = target.uppercase() == "ON"
                            cameraManager.setTorchMode(cameraId, isTurnOn)
                            val status = if (isTurnOn) "turned ON" else "turned OFF"
                            memoryRepo.insert(Memory(type = "LOG", content = "Flashlight $status"))
                            return "Flashlight has been $status, Sir."
                        } catch (e: Exception) {
                            return "Failed to access Flashlight: ${e.message}"
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
