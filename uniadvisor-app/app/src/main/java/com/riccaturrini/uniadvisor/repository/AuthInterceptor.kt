package com.riccaturrini.uniadvisor.network

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor per aggiungere automaticamente il token Firebase Auth alle richieste HTTP.
 *
 * VERSIONE FINALE che gestisce correttamente l'autenticazione con Firebase.
 * Usa runBlocking per assicurarsi che il token sia sempre disponibile quando necessario.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Log.d("AuthInterceptor", "🌐 Intercepting: ${originalRequest.method} ${originalRequest.url}")

        // Ottieni il token usando runBlocking per assicurarti che sia sincrono
        val token = runBlocking {
            try {
                val currentUser = Firebase.auth.currentUser

                if (currentUser != null) {
                    Log.d("AuthInterceptor", "👤 Found user: ${currentUser.uid}")
                    Log.d("AuthInterceptor", "📧 User email: ${currentUser.email}")

                    // IMPORTANTE: Usa sempre getIdToken(true) per forzare un refresh
                    // Questo risolve i problemi di token stale o invalid
                    val tokenResult = currentUser.getIdToken(true).await()
                    val tokenString = tokenResult.token

                    if (tokenString != null) {
                        Log.d("AuthInterceptor", "✅ Token obtained successfully")
                        Log.d("AuthInterceptor", "🔑 Token length: ${tokenString.length}")
                        Log.d("AuthInterceptor", "🔑 Token preview: ${tokenString.take(20)}...")
                    } else {
                        Log.e("AuthInterceptor", "⚠️ Token is null despite user being logged in")
                    }

                    tokenString
                } else {
                    Log.w("AuthInterceptor", "⚠️ No current user in Firebase Auth")
                    null
                }
            } catch (e: Exception) {
                Log.e("AuthInterceptor", "💥 Error getting token: ${e.message}", e)
                Log.e("AuthInterceptor", "Stack trace:", e)
                null
            }
        }

        // Costruisci la nuova richiesta
        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "✅ Authorization header added to request")
        } else {
            Log.e("AuthInterceptor", "❌ NO TOKEN AVAILABLE - Request will be sent without authentication")
            Log.e("AuthInterceptor", "💡 This will likely result in 401/403 error from backend")

            // Log additional debug info
            Log.d("AuthInterceptor", "📱 Firebase Auth instance: ${Firebase.auth}")
            Log.d("AuthInterceptor", "📱 Firebase App: ${Firebase.auth.app}")
        }

        val newRequest = requestBuilder.build()

        // Esegui la richiesta
        val response = chain.proceed(newRequest)

        // Log della risposta per debugging
        Log.d("AuthInterceptor", "📨 Response: ${response.code} for ${originalRequest.url.encodedPath}")

        // Se ricevi 401, potrebbe essere necessario fare logout
        if (response.code == 401) {
            Log.e("AuthInterceptor", "⚠️ Received 401 Unauthorized - Token might be invalid")
        }

        return response
    }
}