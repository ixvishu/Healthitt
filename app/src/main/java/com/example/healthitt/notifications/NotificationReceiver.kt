package com.example.healthitt.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healthitt.MainActivity
import com.example.healthitt.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Healthitt"
        val message = intent.getStringExtra("message") ?: "Stay healthy!"
        val notificationId = intent.getIntExtra("id", 0)

        showNotification(context, title, message, notificationId)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "healthitt_wellness_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wellness Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pleasant reminders for your health journey"
                enableLights(true)
                enableVibration(true)
                // Note: To use a custom unique sound, the user should place a soft sound file 
                // (e.g., pleasant_chime.mp3) in res/raw/ and use:
                // setSound(Uri.parse("android.resource://${context.packageName}/raw/pleasant_chime"), audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
