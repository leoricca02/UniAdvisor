package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Handles raw sensor data to calculate phone orientation (Azimuth).
 * Uses Accelerometer and Magnetometer.
 */
class CompassSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Returns a Flow of the Azimuth (angle from North in degrees, 0-360)
    val azimuth: Flow<Float> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values
                }
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values
                }

                if (gravity != null && geomagnetic != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    // Calculate Rotation Matrix
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        // orientation[0] is azimuth in radians
                        val azimuthRad = orientation[0]
                        // Convert to degrees and normalize to 0-360
                        val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360) % 360
                        trySend(azimuthDeg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for this implementation
            }
        }

        // Register listeners. SENSOR_DELAY_UI is suitable for visual updates.
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        // Clean up when the Flow is cancelled
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}