// file: data/models.kt
package com.riccaturrini.uniadvisor.data

import com.google.gson.annotations.SerializedName
import android.net.Uri


// Corrisponde a schemas/user.py -> UserProfileCreate
data class UserProfileCreate(
    val first_name: String,
    val last_name: String,
    val birth_date: String, // Mandiamo la data come stringa "YYYY-MM-DD"
    val city: String
)

// Corrisponde a schemas/user.py -> UserResponse
data class UserResponse(
    val id: Int,
    val firebase_uid: String,
    val email: String,
    val first_name: String,
    val last_name: String,
    val birth_date: String,
    val city: String,
    val is_admin: Boolean,
    val faculty_id: Int?,
    val faculty_name: String? = null
)

// Corrisponde al modello Faculty del backend
data class Faculty(
    val id: Int,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    @SerializedName("building_name")
    val buildingName: String? = null
)

// Per le risposte dei corsi con teacher
data class Course(
    val id: Int,
    val name: String,
    @SerializedName("faculty_id")
    val facultyId: Int,
    @SerializedName("teacher_id")
    val teacherId: Int?,
    @SerializedName("room_number")
    val roomNumber: String? = null,
    @SerializedName("building_name")
    val buildingName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val floor: Int? = null
)

// Per le recensioni
data class Review(
    val id: Int,
    val course_id: Int,
    val student_id: Int,
    val rating_clarity: Int,
    val rating_feasibility: Int,
    val rating_availability: Int,
    val comment: String?,
    val created_at: String
)

// Per creare una recensione
data class ReviewCreate(
    val rating_clarity: Int,
    val rating_feasibility: Int,
    val rating_availability: Int,
    val comment: String?
)

// Per le note
data class Note(
    val id: Int,
    val course_id: Int,
    val student_id: Int,
    val file_id: String,
    val description: String?,
    val created_at: String,
    val average_rating: Double? = null,
    val course_name: String? = null
)

data class NoteWithRating(
    val id: Int,
    val course_id: Int,
    val student_id: Int,
    val file_id: String,
    val description: String?,
    val created_at: String,
    val average_rating: Double? = null,
    val course_name: String? = null
)

// Per le valutazioni delle note
data class NoteRating(
    val id: Int,
    val note_id: Int,
    val student_id: Int,
    val rating: Int,
    val comment: String?,
    val created_at: String
)

// Per creare un rating di una nota
data class NoteRatingCreate(
    val note_id: Int,
    val rating: Int,  // 1-5
    val comment: String?
)

// Per aggiornare un rating
data class NoteRatingUpdate(
    val rating: Int,
    val comment: String?
)

// For creating a note
data class NoteCreate(
    val course_id: Int,
    val file_id: String, // Firebase Storage download URL
    val description: String?
)


/**
 * Represents a captured image with its URI and extracted text
 */
data class CapturedImage(
    val uri: Uri,
    val extractedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the state of OCR processing
 */
sealed class OcrProcessingState {
    object Idle : OcrProcessingState()
    object Processing : OcrProcessingState()
    data class Success(val text: String) : OcrProcessingState()
    data class Error(val message: String) : OcrProcessingState()
}

/**
 * Represents the state of PDF generation
 */
sealed class PdfGenerationState {
    object Idle : PdfGenerationState()
    data class Processing(val progress: Int) : PdfGenerationState()
    data class Success(val pdfUri: Uri) : PdfGenerationState()
    data class Error(val message: String) : PdfGenerationState()
}

/**
 * Represents the state of uploading a note
 */
sealed class UploadNoteState {
    object Idle : UploadNoteState()
    data class Uploading(val progress: Int) : UploadNoteState()
    object Success : UploadNoteState()
    data class Error(val message: String) : UploadNoteState()
}



data class Lesson(
    val id: Int,
    @SerializedName("day_of_week") val dayOfWeek: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val course: CourseLocationInfo,
    val checkins: Int = 0,

    val last_checkin_date: String? = null
)

data class CourseLocationInfo(
    val id: Int,
    val name: String,
    @SerializedName("teacher_name") val teacherName: String?,
    @SerializedName("room_number") val roomNumber: String?,
    @SerializedName("building_name") val buildingName: String?,
    val floor: Int?,
    val latitude: Double?,
    val longitude: Double?
)