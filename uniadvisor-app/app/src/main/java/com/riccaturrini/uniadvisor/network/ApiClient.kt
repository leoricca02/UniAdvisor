package com.riccaturrini.uniadvisor.network

import com.riccaturrini.uniadvisor.data.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

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

    @GET("notes/{course_id}")
    suspend fun getNotesByCourse(@Path("course_id") courseId: Int): retrofit2.Response<List<Note>>

    // ✅ NEW: Note rating endpoints
    @GET("notes/{course_id}/notes-sorted")
    suspend fun getNotesWithRatings(
        @Path("course_id") courseId: Int,
        @Query("order") order: String = "desc"
    ): retrofit2.Response<List<NoteWithRating>>

    @POST("notes/ratings")
    suspend fun addNoteRating(@Body rating: NoteRatingCreate): retrofit2.Response<NoteRating>

    @PUT("notes/ratings/{rating_id}")
    suspend fun updateNoteRating(
        @Path("rating_id") ratingId: Int,
        @Body rating: NoteRatingUpdate
    ): retrofit2.Response<NoteRating>

    @DELETE("notes/ratings/{rating_id}")
    suspend fun deleteNoteRating(@Path("rating_id") ratingId: Int): retrofit2.Response<Unit>

    @GET("notes/usr/my-reviews")
    suspend fun getMyNoteRatings(): retrofit2.Response<List<NoteRating>>

    @GET("notes/notes/{note_id}/average-rating")
    suspend fun getNoteAverageRating(@Path("note_id") noteId: Int): retrofit2.Response<Map<String, Any>>

    @GET("notes/notes/{note_id}/reviews")
    suspend fun getNoteReviews(@Path("note_id") noteId: Int): retrofit2.Response<List<NoteRating>>
}

/**
 * Singleton per l'istanza di ApiService.
 * Configurato con timeout appropriati e l'AuthInterceptor corretto.
 */
object ApiClient {
    // URL del backend - usa 10.0.2.2 per l'emulatore Android
    private const val BASE_URL = "https://uniadvisor-backend-5mop.onrender.com/"
    //private const val BASE_URL = "http://10.0.2.2:8000"

    // Configurazione OkHttpClient con timeout estesi e AuthInterceptor
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor()) // Usa l'AuthInterceptor corretto
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Aggiungi retry per connessioni fallite
        .retryOnConnectionFailure(true)
        .build()

    // Istanza singleton di ApiService
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        retrofit.create(ApiService::class.java)
    }
}