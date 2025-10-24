package com.riccaturrini.uniadvisor.network

import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.data.Faculty
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.data.ReviewCreate
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import com.riccaturrini.uniadvisor.data.Note
import com.riccaturrini.uniadvisor.data.NoteRating
import com.riccaturrini.uniadvisor.repository.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    // User endpoints
    @GET("users/me")
    suspend fun getMyProfile(): Response<UserResponse>

    @POST("users/profile")
    suspend fun createUserProfile(@Body profileData: UserProfileCreate): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateMyProfile(@Body profileData: UserProfileCreate): Response<UserResponse>

    @DELETE("users/me")
    suspend fun deleteMyAccount(): Response<Unit>

    // Faculty endpoints
    @GET("faculties")
    suspend fun getFaculties(): Response<List<Faculty>>

    @POST("faculties/enroll/{faculty_id}")
    suspend fun enrollInFaculty(@Path("faculty_id") facultyId: Int): Response<Unit>

    @GET("faculties/my-faculty")
    suspend fun getMyFaculty(): Response<Faculty>

    @PUT("faculties/change-faculty/{faculty_id}")
    suspend fun changeFaculty(@Path("faculty_id") facultyId: Int): Response<Unit>

    // Course endpoints
    @GET("courses/faculty/{faculty_id}")
    suspend fun getCoursesByFaculty(@Path("faculty_id") facultyId: Int): Response<List<Course>>

    @GET("courses/{course_id}/details")
    suspend fun getCourseDetail(@Path("course_id") courseId: Int): Response<Course>

    @GET("courses/{course_id}/teacher")
    suspend fun getCourseTeacher(@Path("course_id") courseId: Int): Response<Map<String, Any>>

    @GET("courses/{course_id}/ratings")
    suspend fun getCourseRatings(@Path("course_id") courseId: Int): Response<Map<String, Any>>

    // Review endpoints
    @GET("courses/{course_id}/reviews")
    suspend fun getCourseReviews(@Path("course_id") courseId: Int): Response<List<Review>>

    @POST("courses/{course_id}/reviews")
    suspend fun addReview(@Path("course_id") courseId: Int, @Body review: ReviewCreate): Response<Review>

    @GET("courses/my-reviews")
    suspend fun getMyReviews(): Response<List<Review>>

    // Notes endpoints
    @GET("notes/usr/my-notes")
    suspend fun getMyNotes(): Response<List<Note>>

    @GET("notes/usr/my-reviews")
    suspend fun getMyNoteRatings(): Response<List<NoteRating>>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
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