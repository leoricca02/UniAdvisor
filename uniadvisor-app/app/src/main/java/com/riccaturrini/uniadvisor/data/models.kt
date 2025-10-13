// file: data/models.kt
package com.riccaturrini.uniadvisor.data

data class User(
    val uid: String = "",
    val email: String = "",
    val faculty: String = ""
)

data class Course(
    val id: String = "",
    val name: String = "",
    val professor: String = "",
    val faculty: String = ""
)

data class Review(
    val authorId: String = "",
    val text: String = "",
    val feasibility: Int = 0, // Voto da 1 a 5
    val difficulty: Int = 0, // Voto da 1 a 5
    val availability: Int = 0  // Voto da 1 a 5
)