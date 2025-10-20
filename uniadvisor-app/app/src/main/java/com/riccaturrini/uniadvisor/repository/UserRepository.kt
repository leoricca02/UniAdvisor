package com.riccaturrini.uniadvisor.repository

import android.util.Log
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import com.riccaturrini.uniadvisor.network.ApiClient

class UserRepository {

    private val apiService = ApiClient.instance

    suspend fun getMyProfile(): UserResponse? {
        return try {
            Log.d("UserRepository", "🔍 Calling getMyProfile")
            val response = apiService.getMyProfile()

            if (response.isSuccessful) {
                Log.d("UserRepository", "✅ getMyProfile success: ${response.body()}")
                response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "❌ getMyProfile failed: CODE=${response.code()} ERROR=$errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "💥 getMyProfile exception", e)
            null
        }
    }

    suspend fun createProfile(profileData: UserProfileCreate): UserResponse? {
        return try {
            Log.d("UserRepository", "🟢 Calling createUserProfile with: $profileData")
            val response = apiService.createUserProfile(profileData)

            Log.d("UserRepository", "🟡 Response code: ${response.code()}")

            if (response.isSuccessful) {
                Log.d("UserRepository", "✅ Profile created successfully: ${response.body()}")
                response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "❌ createProfile failed: CODE=${response.code()} ERROR=$errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "💥 createProfile exception", e)
            null
        }
    }
}