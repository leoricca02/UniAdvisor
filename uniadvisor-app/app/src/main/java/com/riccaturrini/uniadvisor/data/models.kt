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
    val faculty_id: Int?
)