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
 * Wrapper class for Android SensorManager to listen to high-frequency accelerometer events.
 */
class TapSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onImpactDetected: ((Float) -> Unit)? = null

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
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate magnitude of acceleration
            val magnitude = sqrt(x * x + y * y + z * z)
            
            // Simple logic to detect sudden physical impact (threshold can be tuned)
            // Gravity is ~9.8 m/s^2, so we look for values significantly higher
            if (magnitude > 15f) {
                onImpactDetected?.invoke(magnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for simple impact detection
    }

    /**
     * Provides a Flow of accelerometer magnitude for Compose/Coroutines usage.
     */
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

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
