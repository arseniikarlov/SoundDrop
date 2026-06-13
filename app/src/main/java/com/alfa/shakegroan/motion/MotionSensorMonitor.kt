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
    private val accelerometer = sensorManager?.wakeUpOrDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.wakeUpOrDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var detector = MotionEventDetector(DetectorConfig())
    private var isStarted = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val motionEvent = detector.onSample(values[0], values[1], values[2]) ?: return
                    onMotionDetected(motionEvent)
                }

                Sensor.TYPE_GYROSCOPE -> {
                    detector.onGyroscopeSample(values[0], values[1], values[2])
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun hasAccelerometer(): Boolean = accelerometer != null

    fun requiresScreenOffWakeLock(): Boolean {
        return accelerometer?.isWakeUpSensor != true
    }

    fun start(config: DetectorConfig) {
        val manager = sensorManager ?: return
        val sensor = accelerometer ?: return
        if (isStarted) {
            detector.updateConfig(config)
            return
        }

        detector = MotionEventDetector(config)
        manager.registerListener(listener, sensor, SENSOR_SAMPLE_PERIOD_US)
        gyroscope?.let { gyroSensor ->
            manager.registerListener(listener, gyroSensor, SENSOR_SAMPLE_PERIOD_US)
        }
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

    private companion object {
        const val SENSOR_SAMPLE_PERIOD_US = 10_000
    }
}

private fun SensorManager.wakeUpOrDefaultSensor(sensorType: Int): Sensor? {
    return getDefaultSensor(sensorType, true) ?: getDefaultSensor(sensorType)
}
