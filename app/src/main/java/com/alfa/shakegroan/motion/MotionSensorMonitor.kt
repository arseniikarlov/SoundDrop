package com.alfa.shakegroan.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MotionSensorMonitor(
    context: Context,
    private val onMotionDetected: (MotionEventType) -> Unit,
) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var detector = MotionEventDetector(DetectorConfig())
    private var isStarted = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            val motionEvent = detector.onSample(values[0], values[1], values[2]) ?: return
            onMotionDetected(motionEvent)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun hasAccelerometer(): Boolean = accelerometer != null

    fun start(config: DetectorConfig) {
        val manager = sensorManager ?: return
        val sensor = accelerometer ?: return

        detector.updateConfig(config)
        if (isStarted) {
            return
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        isStarted = true
    }

    fun stop() {
        val manager = sensorManager ?: return
        if (!isStarted) {
            return
        }

        manager.unregisterListener(listener)
        isStarted = false
    }
}

