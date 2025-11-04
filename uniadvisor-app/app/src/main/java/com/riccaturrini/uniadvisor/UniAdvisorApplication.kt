package com.riccaturrini.uniadvisor

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Application class per UniAdvisor.
 *
 * VERSIONE CORRETTA: Rimosso il logout automatico quando l'app va in background.
 *
 * Il logout automatico causava problemi durante l'upload delle note perché quando
 * l'utente apriva il file picker, l'app andava in background e faceva logout.
 */
class UniAdvisorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza Firebase
        FirebaseApp.initializeApp(this)
        Log.d("UniAdvisorApp", "Application created - Firebase initialized")

        // RIMOSSO: Il lifecycle observer che faceva logout automatico
        // Questo causava il problema con l'upload delle note

        // Se vuoi mantenere un logout automatico per sicurezza, puoi:
        // 1. Usare un timer più lungo (es. logout dopo 5 minuti di inattività)
        // 2. Fare logout solo quando l'app è completamente terminata
        // 3. Salvare lo stato dell'autenticazione e ripristinarlo quando l'app riprende
    }
}