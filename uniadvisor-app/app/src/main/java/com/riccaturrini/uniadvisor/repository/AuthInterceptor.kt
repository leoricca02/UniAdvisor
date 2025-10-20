package com.riccaturrini.uniadvisor.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Otteniamo il token di autenticazione di Firebase
        // Usiamo runBlocking perché l'interceptor non è una suspend function
        val token = runBlocking {
            try {
                Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
            } catch (e: Exception) {
                null // Se c'è un errore (es. utente non loggato), non inviamo il token
            }
        }

        // Creiamo una nuova richiesta aggiungendo l'header "Authorization"
        val newRequest = chain.request().newBuilder().apply {
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        // Eseguiamo la nuova richiesta autenticata
        return chain.proceed(newRequest)
    }
}