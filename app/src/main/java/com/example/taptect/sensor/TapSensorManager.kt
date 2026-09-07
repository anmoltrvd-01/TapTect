package com.example.taptect.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Enhanced SensorManager with dual-trigger logic and cooldown periods.
 */
class TapSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onImpactDetected: ((Float) -> Unit)? = null
    
    private var lastTriggerTime = 0L
    private val cooldownMs = 300L
    private var gForceThreshold = 18f // Adjustable threshold

    fun startListening(onImpact: (Float) -> Unit) {
        onImpactDetected = onImpact
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        onImpactDetected = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < cooldownMs) return

            val z = event.values[2] // Focus on Z-axis for physical impacts
            val x = event.values[0]
            val y = event.values[1]
            val magnitude = sqrt(x * x + y * y + z * z)

            // Sharp Z-axis movement often indicates a surface tap
            if (magnitude > gForceThreshold) {
                lastTriggerTime = now
                onImpactDetected?.invoke(magnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getAccelerometerFlow(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    trySend(sqrt(x * x + y * y + z * z))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
