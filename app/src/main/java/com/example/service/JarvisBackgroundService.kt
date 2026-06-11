package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JarvisBackgroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "jarvis_service_channel")
            .setContentTitle("Rakib Jarvis")
            .setContentText("ব্যাকগ্রাউন্ডে সিস্টেম মনিটর করতেছে")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
            
        startForeground(1, notification)
        
        scope.launch {
            while(true) {
                delay(300000) // Every 5 minutes
                sendSuggestionNotification()
            }
        }
        
        return START_STICKY
    }
    
    private fun sendSuggestionNotification() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        var batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (batteryLevel <= 0) {
            batteryLevel = 85 // Safe fallback
        }

        val messages = mutableListOf(
            "র‍্যাম খালি করতে ব্যাকগ্রাউন্ডের অব্যবহৃত অ্যাপগুলো বন্ধ করুন।",
            "অব্যবহৃত বড় ফাইলগুলি স্ক্যান করে ডিলিট করুন।",
            "জার্ভিস আপনার কমান্ডের জন্য প্রস্তুত, নির্দ্বিধায় প্রশ্ন করুন!"
        )

        if (batteryLevel < 20) {
            messages.add("আপনার ফোনের চার্জ মাত্র $batteryLevel% এ নেমে এসেছে। দয়া করে পাওয়ার সেভার চালু করুন।")
        } else {
            messages.add("আপনার ফোনের চার্জ বর্তমানে পর্যাপ্ত রয়েছে ($batteryLevel%)। সিস্টেম স্বাভাবিকভাবে কাজ করছে।")
        }
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, "jarvis_service_channel")
            .setContentTitle("Jarvis অতিপ্রয়োজনীয় পরামর্শ")
            .setContentText(messages.random())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        nm.notify((System.currentTimeMillis() % 10000).toInt(), notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "jarvis_service_channel",
                "Jarvis Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
