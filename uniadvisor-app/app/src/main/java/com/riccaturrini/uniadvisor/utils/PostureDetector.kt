package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.sqrt

class PostureDetector(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Emits true if the device is effectively "flat" (lying down), false otherwise.
     */
    val isLyingFlat: Flow<Boolean> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate total acceleration (should be close to 9.81)
                    val magnitude = sqrt(x * x + y * y + z * z)

                    // Normalize Z (value between -1 and 1) representing the angle with gravity
                    // If |z| / magnitude is close to 1, the device is flat.
                    val zTilt = abs(z) / magnitude

                    // Threshold: 0.8 corresponds to roughly ~35 degrees tilt from completely flat.
                    // If zTilt > 0.8, the device is mostly flat (User lying down or phone on table).
                    val isFlat = zTilt > 0.8

                    trySend(isFlat)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}