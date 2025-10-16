// File: repository/UserRepository.kt
package com.riccaturrini.uniadvisor.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val apiService = ApiClient.apiService

    /**
     * Recupera il token Firebase in modo asincrono.
     */
    private suspend fun getAuthToken(): String {
        val user = Firebase.auth.currentUser
            ?: throw Exception("User not logged in")

        val token = user.getIdToken(false).await()?.token
            ?: throw Exception("Unable to retrieve Firebase token")

        return "Bearer $token"
    }

    /**
     * Crea un profilo utente nel backend.
     */
    suspend fun createUserProfile(profileData: UserProfileCreate): UserResponse {
        val token = getAuthToken()
        return apiService.createUserProfile(token, profileData)
    }

    /**
     * Ottiene il profilo dell’utente corrente.
     * Restituisce null se il profilo non esiste.
     */
    suspend fun getMyProfile(): UserResponse? {
        return try {
            val token = getAuthToken()
            apiService.getMyProfile(token)
        } catch (e: Exception) {
            null
        }
    }
}
