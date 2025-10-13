// file: repository/CourseRepository.kt
package com.riccaturrini.uniadvisor.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.riccaturrini.uniadvisor.data.Course   // Assicurati di importare i tuoi modelli
import com.riccaturrini.uniadvisor.data.Review

class CourseRepository {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    // Ottiene i corsi dal database Firestore
    suspend fun getCoursesByFaculty(faculty: String): List<Course> {
        return try {
            val snapshot = firestore.collection("courses")
                .whereEqualTo("faculty", faculty)
                .get()
                .await()
            // Converte i documenti ottenuti in una lista di oggetti Course
            snapshot.toObjects(Course::class.java)
        } catch (e: Exception) {
            // In caso di errore, ritorna una lista vuota
            e.printStackTrace()
            emptyList()
        }
    }

    // Aggiunge una recensione
    suspend fun addReviewToCourse(courseId: String, review: Review) {
        try {
            val user = auth.currentUser ?: return // Serve un utente loggato
            val reviewWithAuthor = review.copy(authorId = user.uid)

            firestore.collection("courses")
                .document(courseId)
                .collection("reviews")
                .add(reviewWithAuthor)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}