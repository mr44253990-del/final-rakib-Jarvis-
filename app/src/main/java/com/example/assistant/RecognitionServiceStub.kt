package com.example.assistant

import android.content.Intent
import android.speech.RecognitionService

class RecognitionServiceStub : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Minimal stub callback trigger
    }

    override fun onCancel(listener: Callback?) {
        // Minimal stub cancellation trigger
    }

    override fun onStopListening(listener: Callback?) {
        // Minimal stub stop listening trigger
    }
}
