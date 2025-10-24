package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CourseWithRatings(
    val course: Course,
    val teacherName: String,
    val avgClarity: Double,
    val avgFeasibility: Double,
    val avgAvailability: Double
)

sealed class CourseListState {
    object Loading : CourseListState()
    data class Success(val courses: List<CourseWithRatings>) : CourseListState()
    data class Error(val message: String) : CourseListState()
}

class CourseViewModel : ViewModel() {
    private val _courseListState = MutableStateFlow<CourseListState>(CourseListState.Loading)
    val courseListState: StateFlow<CourseListState> = _courseListState

    fun loadCoursesByFaculty(facultyId: Int) {
        Log.d("CourseViewModel", "📚 loadCoursesByFaculty called with ID: $facultyId")
        viewModelScope.launch {
            _courseListState.value = CourseListState.Loading
            try {
                val coursesResponse = ApiClient.instance.getCoursesByFaculty(facultyId)

                if (coursesResponse.isSuccessful && coursesResponse.body() != null) {
                    val courses = coursesResponse.body()!!
                    val coursesWithRatings = mutableListOf<CourseWithRatings>()

                    // Per ogni corso, ottieni il docente e le votazioni
                    for (course in courses) {
                        try {
                            // Ottieni il docente
                            val teacherResponse = ApiClient.instance.getCourseTeacher(course.id)
                            val teacherName = if (teacherResponse.isSuccessful) {
                                (teacherResponse.body()?.get("name") as? String) ?: "N/A"
                            } else "N/A"

                            // Ottieni le votazioni medie
                            val ratingsResponse = ApiClient.instance.getCourseRatings(course.id)
                            val (avgClarity, avgFeasibility, avgAvailability) = if (ratingsResponse.isSuccessful) {
                                val ratings = ratingsResponse.body()
                                Triple(
                                    (ratings?.get("average_clarity") as? Number)?.toDouble() ?: 0.0,
                                    (ratings?.get("average_feasibility") as? Number)?.toDouble() ?: 0.0,
                                    (ratings?.get("average_availability") as? Number)?.toDouble() ?: 0.0
                                )
                            } else {
                                Triple(0.0, 0.0, 0.0)
                            }

                            coursesWithRatings.add(
                                CourseWithRatings(
                                    course = course,
                                    teacherName = teacherName,
                                    avgClarity = avgClarity,
                                    avgFeasibility = avgFeasibility,
                                    avgAvailability = avgAvailability
                                )
                            )
                        } catch (e: Exception) {
                            // Se c'è un errore per un singolo corso, aggiungi comunque con valori default
                            coursesWithRatings.add(
                                CourseWithRatings(
                                    course = course,
                                    teacherName = "N/A",
                                    avgClarity = 0.0,
                                    avgFeasibility = 0.0,
                                    avgAvailability = 0.0
                                )
                            )
                        }
                    }

                    _courseListState.value = CourseListState.Success(coursesWithRatings)
                } else {
                    _courseListState.value = CourseListState.Error("Errore nel caricamento dei corsi: ${coursesResponse.code()}")
                }
            } catch (e: Exception) {
                _courseListState.value = CourseListState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}