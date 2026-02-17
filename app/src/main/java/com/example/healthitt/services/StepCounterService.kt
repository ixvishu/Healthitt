package com.example.healthitt.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class StepCounterService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var userEmailKey: String? = null
    
    private var lastSensorValue = -1f
    private var todaySteps = 0
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var lastUpdateDate: String = ""

    override fun onCreate() {
        super.onCreate()
        lastUpdateDate = sdf.format(Date())
        showServiceNotification("Initializing...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val email = intent?.getStringExtra("user_email")
        if (email != null) {
            val newKey = email.replace(".", "_")
            if (userEmailKey != newKey) {
                userEmailKey = newKey
                lastSensorValue = -1f // Reset sensor baseline for new user
                loadInitialData()
            }
            showServiceNotification("Tracking your movement.")
            initializeSensors()
        } else if (userEmailKey == null) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun showServiceNotification(message: String) {
        val channelId = "step_counter_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Healthitt Active")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
    }

    private fun initializeSensors() {
        if (sensorManager == null) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            
            if (stepSensor == null) {
                Log.e("StepCounter", "Step Counter sensor not available on this device")
                showServiceNotification("Step sensor unavailable on this device.")
            } else {
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    private fun loadInitialData() {
        val key = userEmailKey ?: return
        val todayDate = sdf.format(Date())
        val userRef = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users").child(key)
        
        userRef.child("daily_history").child(todayDate).get().addOnSuccessListener {
            todaySteps = it.getValue(Int::class.java) ?: 0
            lastUpdateDate = todayDate
        }
        
        userRef.child("last_sensor_value").get().addOnSuccessListener {
            lastSensorValue = it.getValue(Float::class.java) ?: -1f
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val currentSensorValue = event.values[0]
        val key = userEmailKey ?: return
        val todayDate = sdf.format(Date())
        
        // Handle date change (reset steps at midnight)
        if (todayDate != lastUpdateDate) {
            todaySteps = 0
            lastUpdateDate = todayDate
        }

        val userRef = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users").child(key)

        // Initial baseline setup
        if (lastSensorValue == -1f || currentSensorValue < lastSensorValue) {
            lastSensorValue = currentSensorValue
            userRef.child("last_sensor_value").setValue(lastSensorValue)
            return
        }

        val delta = (currentSensorValue - lastSensorValue).toInt()
        
        if (delta > 0) {
            todaySteps += delta
            lastSensorValue = currentSensorValue
            
            // Batch update to Firebase
            userRef.child("daily_history").child(todayDate).setValue(todaySteps)
            userRef.child("currentSteps").setValue(todaySteps)
            userRef.child("last_sensor_value").setValue(lastSensorValue)
            
            Log.d("StepCounter", "Steps updated: $todaySteps (Delta: $delta)")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }
}
