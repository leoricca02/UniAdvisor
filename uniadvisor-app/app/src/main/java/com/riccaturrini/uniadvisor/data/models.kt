// file: data/models.kt
package com.riccaturrini.uniadvisor.data

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
    val name: String
)

// Per le risposte dei corsi con teacher
data class Course(
    val id: Int,
    val name: String,
    val faculty_id: Int,
    val teacher_id: Int
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
    val created_at: String
)

// ✅ NEW: Note con rating medio
data class NoteWithRating(
    val id: Int,
    val course_id: Int,
    val student_id: Int,
    val file_id: String,
    val description: String?,
    val created_at: String,
    val average_rating: Double? // Può essere null se nessuno ha votato
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

// ✅ NEW: Per creare un rating di una nota
data class NoteRatingCreate(
    val note_id: Int,
    val rating: Int,  // 1-5
    val comment: String?
)

// ✅ NEW: Per aggiornare un rating
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