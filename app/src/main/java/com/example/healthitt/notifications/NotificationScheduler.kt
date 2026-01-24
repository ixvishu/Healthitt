package com.example.healthitt.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

object NotificationScheduler {

    fun scheduleDailyWellnessReminders(context: Context) {
        // Morning Reminder - 8:00 AM
        scheduleNotification(
            context, 101, "Morning Vitality ☀️", 
            "Start your day with a glass of water and a quick stretch!", 
            8, 0
        )

        // Afternoon Reminder - 2:00 PM
        scheduleNotification(
            context, 102, "Peak Performance ⚡", 
            "How are your steps coming along? Let's hit that goal!", 
            14, 0
        )

        // Evening Reminder - 9:00 PM
        scheduleNotification(
            context, 103, "Recovery Mode 🌙", 
            "Time to wind down and log your progress for today.", 
            21, 0
        )
    }

    private fun scheduleNotification(context: Context, id: Int, title: String, message: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
