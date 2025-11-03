package com.riccaturrini.uniadvisor.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.data.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        android.util.Log.d("AuthInterceptor", "🌐 Intercepting: ${originalRequest.method} ${originalRequest.url}")

        val token = try {
            val currentUser = Firebase.auth.currentUser
            android.util.Log.d("AuthInterceptor", "👤 Current user: ${currentUser?.uid ?: "NULL"}")
            android.util.Log.d("AuthInterceptor", "📧 User email: ${currentUser?.email ?: "NULL"}")

            if (currentUser != null) {
                val tokenTask = currentUser.getIdToken(false)
                val tokenResult = Tasks.await(tokenTask, 10, TimeUnit.SECONDS)
                val tokenString = tokenResult.token
                android.util.Log.d("AuthInterceptor", "✅ Token obtained (length: ${tokenString?.length ?: 0})")
                android.util.Log.d("AuthInterceptor", "🔑 Token (first 50): ${tokenString?.take(50)}")
                tokenString
            } else {
                android.util.Log.e("AuthInterceptor", "❌ Current user is NULL!")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthInterceptor", "💥 Error getting token: ${e.message}", e)
            null
        }

        val request = chain.request().newBuilder()
            .apply {
                if (token != null) {
                    addHeader("Authorization", "Bearer $token")
                    android.util.Log.d("AuthInterceptor", "✅ Authorization header added")
                } else {
                    android.util.Log.e("AuthInterceptor", "⚠️ No token - request sent without auth!")
                }
            }
            .build()

        return chain.proceed(request)
    }
}

interface ApiService {
    // User endpoints
    @GET("users/me")
    suspend fun getMyProfile(): retrofit2.Response<UserResponse>

    @POST("users/profile")
    suspend fun createUserProfile(@Body profileData: UserProfileCreate): retrofit2.Response<UserResponse>

    @PUT("users/me")
    suspend fun updateMyProfile(@Body profileData: UserProfileCreate): retrofit2.Response<UserResponse>

    @DELETE("users/me")
    suspend fun deleteMyAccount(): retrofit2.Response<Unit>

    // Faculty endpoints
    @GET("faculties")
    suspend fun getFaculties(): retrofit2.Response<List<Faculty>>

    @POST("faculties/enroll/{faculty_id}")
    suspend fun enrollInFaculty(@Path("faculty_id") facultyId: Int): retrofit2.Response<Unit>

    @GET("faculties/my-faculty")
    suspend fun getMyFaculty(): retrofit2.Response<Faculty>

    @PUT("faculties/change-faculty/{faculty_id}")
    suspend fun changeFaculty(@Path("faculty_id") facultyId: Int): retrofit2.Response<Unit>

    // Course endpoints
    @GET("courses/faculty/{faculty_id}")
    suspend fun getCoursesByFaculty(@Path("faculty_id") facultyId: Int): retrofit2.Response<List<Course>>

    @GET("courses/{course_id}/details")
    suspend fun getCourseDetail(@Path("course_id") courseId: Int): retrofit2.Response<Course>

    @GET("courses/{course_id}/teacher")
    suspend fun getCourseTeacher(@Path("course_id") courseId: Int): retrofit2.Response<Map<String, Any>>

    @GET("courses/{course_id}/ratings")
    suspend fun getCourseRatings(@Path("course_id") courseId: Int): retrofit2.Response<Map<String, Any>>

    // Review endpoints
    @GET("courses/{course_id}/reviews")
    suspend fun getCourseReviews(@Path("course_id") courseId: Int): retrofit2.Response<List<Review>>

    @POST("courses/{course_id}/reviews")
    suspend fun addReview(@Path("course_id") courseId: Int, @Body review: ReviewCreate): retrofit2.Response<Review>

    @GET("courses/my-reviews")
    suspend fun getMyReviews(): retrofit2.Response<List<Review>>

    @PUT("courses/reviews/{review_id}")
    suspend fun updateReview(@Path("review_id") reviewId: Int, @Body review: ReviewCreate): retrofit2.Response<Review>

    @DELETE("courses/reviews/{review_id}")
    suspend fun deleteReview(@Path("review_id") reviewId: Int): retrofit2.Response<Unit>

    // Notes endpoints
    @GET("notes/usr/my-notes")
    suspend fun getMyNotes(): retrofit2.Response<List<Note>>

    @POST("notes/")
    suspend fun uploadNote(@Body note: NoteCreate): retrofit2.Response<Note>

    @DELETE("notes/{note_id}")
    suspend fun deleteNote(@Path("note_id") noteId: Int): retrofit2.Response<Unit>

    @GET("notes/usr/my-reviews")
    suspend fun getMyNoteRatings(): retrofit2.Response<List<NoteRating>>

    @GET("notes/{course_id}")
    suspend fun getNotesByCourse(@Path("course_id") courseId: Int): retrofit2.Response<List<Note>>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        retrofit.create(ApiService::class.java)
    }
}