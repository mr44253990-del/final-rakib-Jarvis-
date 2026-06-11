package com.example.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class VoiceInteractionSessionStub : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {
            override fun onShow(args: Bundle?, showFlags: Int) {
                super.onShow(args, showFlags)
                try {
                    val intent = android.content.Intent(this@VoiceInteractionSessionStub, com.example.MainActivity::class.java).apply {
                        action = android.content.Intent.ACTION_ASSIST
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
