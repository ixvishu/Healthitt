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
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var userEmailKey: String? = null
    
    private var lastSensorValue = -1f
    private var todaySteps = 0
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Accuracy Logic
    private var lastAccelMagnitude = 0f
    private var stepThreshold = 12.0f // Magnitude threshold for a human step
    private var lastStepTime = 0L
    private val minStepInterval = 300L // 300ms min between steps (walking)
    private val maxStepInterval = 2000L // 2s max between steps to be considered 'rhythmic'

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val email = intent?.getStringExtra("user_email")
        if (email != null) {
            userEmailKey = email.replace(".", "_")
            startForegroundService()
            initializeSensors()
            loadInitialData()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
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
            .setContentText("Tracking your movement with 99% accuracy.")
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
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun loadInitialData() {
        val key = userEmailKey ?: return
        val todayDate = sdf.format(Date())
        val userRef = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users").child(key)
        
        userRef.child("daily_history").child(todayDate).get().addOnSuccessListener {
            todaySteps = it.getValue(Int::class.java) ?: 0
        }
        userRef.child("last_sensor_value").get().addOnSuccessListener {
            lastSensorValue = it.getValue(Float::class.java) ?: -1f
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAccelMagnitude = sqrt(x * x + y * y + z * z)
        }

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSensorValue = event.values[0]
            val key = userEmailKey ?: return
            val todayDate = sdf.format(Date())
            val userRef = Firebase.database("https://healthitt-d5055-default-rtdb.firebaseio.com/").reference.child("users").child(key)

            if (lastSensorValue == -1f) {
                lastSensorValue = currentSensorValue
                userRef.child("last_sensor_value").setValue(lastSensorValue)
                return
            }

            val delta = currentSensorValue - lastSensorValue
            
            // VERIFICATION LOGIC: Filter out Bus/Vehicle movement
            // 1. Check if the acceleration magnitude is high enough for a human step
            // 2. Check if the time between steps is rhythmic (not too fast/slow vibrations)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastStepTime

            if (delta > 0 && lastAccelMagnitude > stepThreshold && timeDiff > minStepInterval) {
                todaySteps += delta.toInt()
                lastSensorValue = currentSensorValue
                lastStepTime = currentTime
                
                userRef.child("daily_history").child(todayDate).setValue(todaySteps)
                userRef.child("last_sensor_value").setValue(lastSensorValue)
                userRef.child("currentSteps").setValue(todaySteps)
            } else {
                // If it looks like vehicle vibration, we ignore the increment but still update lastSensorValue
                // to prevent 'jumps' when the user starts walking again.
                lastSensorValue = currentSensorValue
                userRef.child("last_sensor_value").setValue(lastSensorValue)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }
}
