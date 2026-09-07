package com.example.taptect.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * SensorManager wrapper with physical impact detection logic.
 */
class TapSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onImpactDetected: ((Float) -> Unit)? = null
    
    private var lastTriggerTime = 0L
    private val cooldownMs = 300L
    private var gForceThreshold = 18f 

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

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)

            // Focus on sudden impacts (high G-force)
            if (magnitude > gForceThreshold) {
                lastTriggerTime = now
                onImpactDetected?.invoke(magnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
