package com.example.assistant

import android.service.voice.VoiceInteractionService

class VoiceInteractionStub : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        try {
            val intent = android.content.Intent(this, com.example.MainActivity::class.java).apply {
                action = android.content.Intent.ACTION_ASSIST
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
