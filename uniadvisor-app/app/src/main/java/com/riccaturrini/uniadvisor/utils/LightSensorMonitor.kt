package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LightSensorMonitor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    /**
     * Emette true se l'ambiente è buio (attiva Dark Mode), false se è luminoso.
     * Usa un meccanismo di 'isteresi' per evitare sfarfallii.
     */
    val isDarkEnvironment: Flow<Boolean> = callbackFlow {
        // Se il dispositivo non ha il sensore, chiudiamo subito
        if (lightSensor == null) {
            close()
            return@callbackFlow
        }

        // Stato iniziale (assumiamo luce)
        var isDark = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    val lux = event.values[0]

                    // --- LOGICA DI ISTERESI (Anti-Flicker) ---
                    // Soglie in LUX (Lumen per metro quadro):
                    // < 10 lux: Buio (Camera scura)
                    // > 50 lux: Luce (Ufficio/Giorno)

                    if (!isDark && lux < 10f) {
                        // Era chiaro, ora è diventato molto buio -> Passa a Dark
                        isDark = true
                        trySend(true)
                    } else if (isDark && lux > 50f) {
                        // Era buio, ora c'è molta luce -> Passa a Light
                        isDark = false
                        trySend(false)
                    }
                    // Se siamo tra 10 e 50 (zona grigia), manteniamo lo stato precedente
                    // per evitare che il tema cambi continuamente.
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Non necessario
            }
        }

        // Registriamo il sensore (DELAY_NORMAL va bene per la luce, consuma meno batteria)
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}