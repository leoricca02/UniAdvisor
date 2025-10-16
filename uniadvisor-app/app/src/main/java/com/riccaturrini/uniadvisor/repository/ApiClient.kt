// File: repository/ApiClient.kt
package com.riccaturrini.uniadvisor.repository

import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Definizione degli endpoint API del backend.
 */
interface ApiService {

    @POST("users/profile")
    suspend fun createUserProfile(
        @Header("Authorization") token: String,
        @Body profileData: UserProfileCreate
    ): UserResponse

    @GET("users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): UserResponse
}

/**
 * Configurazione Retrofit + OkHttpClient.
 */
object ApiClient {

    // Sostituisci con il tuo URL backend
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
