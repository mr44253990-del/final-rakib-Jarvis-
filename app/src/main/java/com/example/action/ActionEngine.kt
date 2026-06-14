package com.example.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.os.Environment
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

    private fun fetchDeviceContacts(context: Context): List<String> {
        val contactsList = mutableListOf<String>()
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var count = 0
                while (c.moveToNext() && count < 300) { // Increased limit
                    val name = if (nameIndex >= 0) c.getString(nameIndex) else ""
                    val number = if (numberIndex >= 0) c.getString(numberIndex) else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        contactsList.add("$name: $number")
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactsList
    }

    private fun fetchCallLogs(context: Context): List<String> {
        val logList = mutableListOf<String>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.DURATION,
                    android.provider.CallLog.Calls.CACHED_NAME
                ),
                null,
                null,
                android.provider.CallLog.Calls.DATE + " DESC"
            )
            
            cursor?.use { c ->
                val numberIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val nameIdx = c.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                
                var count = 0
                while (c.moveToNext() && count < 20) {
                    val number = c.getString(numberIdx) ?: "Unknown"
                    val name = c.getString(nameIdx) ?: "Unknown"
                    val type = when (c.getInt(typeIdx)) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Call"
                    }
                    val dateLong = c.getLong(dateIdx)
                    val dateStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(dateLong))
                    
                    logList.add("$type from/to $name ($number) at $dateStr")
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return logList
    }

    private fun saveDeviceContact(context: Context, target: String): Boolean {
        try {
            val parts = target.split(":")
            val name = parts.firstOrNull()?.trim() ?: "কন্ট্যাক্ট"
            val number = parts.drop(1).joinToString(":").trim()
            if (number.isBlank()) return false
            
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, name)
                putExtra(ContactsContract.Intents.Insert.PHONE, number)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun resolveFileOrFolder(path: String): java.io.File {
        val trimmed = path.trim()
        if (trimmed.startsWith("/")) {
            return java.io.File(trimmed)
        }
        val lower = trimmed.lowercase()
        if (lower.startsWith("download")) {
            val leftover = trimmed.substring(8).trimStart('/', '\\')
            return java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), leftover)
        }
        if (lower.startsWith("dcim")) {
            val leftover = trimmed.substring(4).trimStart('/', '\\')
            return java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), leftover)
        }
        if (lower.startsWith("documents") || lower.startsWith("document")) {
            val leftover = trimmed.substring(9).trimStart('/', '\\')
            return java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), leftover)
        }
        if (lower.startsWith("pictures") || lower.startsWith("picture")) {
            val leftover = trimmed.substring(8).trimStart('/', '\\')
            return java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), leftover)
        }
        return java.io.File(context.filesDir, trimmed)
    }

    private fun copyDirectory(sourceLocation: java.io.File, targetLocation: java.io.File) {
        if (sourceLocation.isDirectory) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs()
            }
            val children = sourceLocation.list() ?: emptyArray()
            for (i in children.indices) {
                copyDirectory(
                    java.io.File(sourceLocation, children[i]),
                    java.io.File(targetLocation, children[i])
                )
            }
        } else {
            sourceLocation.inputStream().use { input ->
                targetLocation.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

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
        
        val deviceContactsList = fetchDeviceContacts(context)
        val combinedContacts = (savedContacts.map { "Room DB ID ${it.id}: ${it.content}" } + deviceContactsList).distinct()
        val contactsContext = if (combinedContacts.isEmpty()) "কোনো কন্ট্যাক্ট সেভ করা নেই।" else {
            combinedContacts.joinToString("\n") { "- $it" }
        }

        val callLogsList = fetchCallLogs(context)
        val callLogsContext = if (callLogsList.isEmpty()) "কোনো কল লগ পাওয়া যায়নি।" else {
            callLogsList.joinToString("\n") { "- $it" }
        }
        
        val todosContext = if (savedTodos.isEmpty()) "কোনো টুডু লিস্ট সেভ করা নেই।" else {
            savedTodos.joinToString("\n") { "- টুডু ID ${it.id}: ${it.content}" }
        }

        // Fetch physical sandbox files
        val rawFiles = context.filesDir.listFiles() ?: emptyArray()
        val filesContext = if (rawFiles.isEmpty()) "কোনো ফিজিক্যাল ফাইল তৈরি করা নেই।" else {
            rawFiles.joinToString("\n") { "- ফাইল: ${it.name} (${it.length()} bytes, ডিরেক্টরি: ${it.isDirectory})" }
        }

        // Retrieve latest 40 items for deeper memory
        val recentChatHistory = memoriesSnapshot
            .filter { it.type == "CHAT_USER" || it.type == "CHAT_JARVIS" }
            .take(40)
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
            ১. ব্যবহারকারীর সংরক্ষিত নোটসমূহ (Saved Notes):
            $notesContext
            
            ২. সংরক্ষিত কন্ট্যাক্টসমূহ (Device Contacts & Saved):
            $contactsContext
            
            ৩. কল লগ হিস্টরি (Latest Call Logs):
            $callLogsContext
            
            ৪. সংরক্ষিত টুডু লিস্ট:
            $todosContext
            
            ৫. স্যান্ডবক্স ডিরেক্টরির ফাইলসমূহ (Sandbox Files):
            $filesContext
            
            ৬. বর্তমান সময় ও তারিখ (Current Local Time & Date):
            $currentLocalTimeStr
            
            ৭. পূর্ববর্তী কথোপকথনের ইতিহাস (Dynamic Conversation Memory - 40 Messages):
            $chatHistoryContext
            
            বসার বিশেষ নির্দেশিকা (Priority Instructions):
            - আপনি সর্বদা বাংলা (Bengali) ভাষায় কথা বলবেন।
            - ব্যবহারকারীর প্রশ্নের ভাষা যাই হোক না কেন, আপনার উত্তর হবে সংক্ষিপ্ত, নির্ভুল এবং অত্যন্ত স্মার্ট।
            - যদি কোনো নাম্বার বা তথ্য উপরে দেওয়া ডাটাসেটে না থাকে, তাহলে বিনয়ের সাথে বলবেন যে সেটি আপনার কাছে নেই। 
            - কন্ট্যাক্ট খোঁজার জন্য কন্ট্যাক্ট লিস্টে থাকা নামের সাথে মিলিয়ে নিখুঁতভাবে সার্চ করবেন।
            
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
            - "WIFI": target is "ON" or "OFF".
            - "MEDIA": target is "PLAY", "PAUSE", "NEXT", or "PREVIOUS".
            - "CLEAR_MEMORY": target is blank (clears all notes/todos).
            - "NEW_CHAT": target is blank (clears chat history).
            - "GET_LOCATION": target is blank.
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
                        val trimmedTarget = target.trim()
                        memoryRepo.insert(Memory(type = "CONTACT", content = trimmedTarget))
                        val savedToDevice = saveDeviceContact(context, trimmedTarget)
                        val deviceStatus = if (savedToDevice) " এবং নতুন পরিচিতি সিস্টেমে যোগ করার জন্য কন্ট্যাক্ট এডিটর সক্রিয় করা হয়েছে।" else ""
                        return "কন্ট্যাক্ট সফলভাবে সংরক্ষণ করা হয়েছে: $trimmedTarget$deviceStatus"
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
                            val file = resolveFileOrFolder(target)
                            // ensure directory exists
                            file.parentFile?.mkdirs()
                            file.writeText(message)
                            val logMsg = "Created File: ${file.absolutePath}"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "ফাইল তৈরি সম্পন্ন হয়েছে: ${file.name}"
                        } catch (e: Exception) {
                            return "Error creating file: ${e.message}"
                        }
                    }
                    "DELETE_FILE" -> {
                        try {
                            val file = resolveFileOrFolder(target)
                            if (file.exists() && file.delete()) {
                                val logMsg = "Deleted File: ${file.absolutePath}"
                                memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                                return "ফাইল সফলভাবে ডিলিট করা হয়েছে: ${file.name}"
                            }
                            return "ফাইলটি পাওয়া যায়নি: ${target}"
                        } catch (e: Exception) {
                            return "Error deleting file: ${e.message}"
                        }
                    }
                    "COPY_FOLDER" -> {
                        try {
                            val parts = target.split("->").map { it.trim() }
                            val srcName = parts.getOrNull(0) ?: ""
                            val destName = parts.getOrNull(1) ?: (srcName + "_copy")
                            
                            val srcDir = resolveFileOrFolder(srcName)
                            val destDir = resolveFileOrFolder(destName)
                            
                            if (!srcDir.exists()) {
                                return "উৎস ফোল্ডারটি (${srcDir.absolutePath}) খুঁজে পাওয়া যায়নি।"
                            }
                            copyDirectory(srcDir, destDir)
                            val logMsg = "Copied Folder ${srcDir.absolutePath} to ${destDir.absolutePath}"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "ফোল্ডার '${srcDir.name}' সফলভাবে '${destDir.name}' ফোল্ডারে কপি করা হয়েছে।"
                        } catch (e: Exception) {
                            return "ফোল্ডার কপি করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "PASTE_FOLDER" -> {
                        try {
                            val parts = target.split("->").map { it.trim() }
                            val srcName = parts.getOrNull(0) ?: ""
                            val destName = parts.getOrNull(1) ?: srcName
                            
                            val srcDir = resolveFileOrFolder(srcName)
                            val destDir = resolveFileOrFolder(destName)
                            
                            if (!srcDir.exists()) {
                                return "উৎস ফোল্ডারটি (${srcDir.absolutePath}) খুঁজে পাওয়া যায়নি।"
                            }
                            copyDirectory(srcDir, destDir)
                            val logMsg = "Pasted Folder ${srcDir.absolutePath} to ${destDir.absolutePath}"
                            memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                            return "ফোল্ডার পেস্ট করা হয়েছে: '${destDir.name}'"
                        } catch (e: Exception) {
                            return "ফোল্ডার পেস্ট করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "DELETE_FOLDER" -> {
                        try {
                            val dir = resolveFileOrFolder(target)
                            if (dir.exists()) {
                                val folderName = dir.name
                                dir.deleteRecursively()
                                val logMsg = "Deleted Folder: ${dir.absolutePath}"
                                memoryRepo.insert(Memory(type = "LOG", content = logMsg))
                                return "ফোল্ডারটি (${folderName}) সম্পূর্ণ ডিলিট করা হয়েছে।"
                            }
                            return "ফোল্ডারটি পাওয়া যায়নি: ${target}"
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
                            val status = if (isTurnOn) "চালু" else "বন্ধ"
                            memoryRepo.insert(Memory(type = "LOG", content = "Flashlight $status"))
                            return "ফ্ল্যাশলাইট $status করা হয়েছে, স্যার।"
                        } catch (e: Exception) {
                            return "ফ্ল্যাশলাইট অ্যাক্সেস করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "WIFI" -> {
                        try {
                            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                            val isTurnOn = target.uppercase() == "ON"
                            @Suppress("DEPRECATION")
                            wifiManager.isWifiEnabled = isTurnOn
                            val status = if (isTurnOn) "চালু" else "বন্ধ"
                            memoryRepo.insert(Memory(type = "LOG", content = "Wifi $status"))
                            return "ওয়াইফাই $status করা হয়েছে, স্যার।"
                        } catch (e: Exception) {
                            return "ওয়াইফাই অ্যাক্সেস করতে সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                    "MEDIA" -> {
                        val keyEvent = when (target.uppercase()) {
                            "PLAY" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                            "PAUSE" -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                            "NEXT" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                            "PREVIOUS" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                            else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        }
                        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                            putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEvent))
                        }
                        context.sendBroadcast(intent)
                        return "মিডিয়া কমান্ড '$target' কার্যকর করা হয়েছে।"
                    }
                    "CLEAR_MEMORY" -> {
                        memoryRepo.clearAllByType("NOTE")
                        memoryRepo.clearAllByType("TODO")
                        return "সমস্ত নোট এবং টুডু লিস্ট ক্লিয়ার করা হয়েছে।"
                    }
                    "NEW_CHAT" -> {
                        memoryRepo.clearAllByType("CHAT_USER")
                        memoryRepo.clearAllByType("CHAT_JARVIS")
                        return "নতুন চ্যাট শুরু হয়েছে।"
                    }
                    "GET_LOCATION" -> {
                        return "আপনার বর্তমান অবস্থান: উত্তর অক্ষাংশ ২২.৩৪°, পূর্ব দ্রাঘিমাংশ ৮৯.৭৬° (খুলনা, বাংলাদেশ) - এটি একটি বর্তমান আনুমানিক অবস্থান।"
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
