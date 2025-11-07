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
                Log.d("CourseViewModel", "📥 Got courses response: ${coursesResponse.code()}")

                if (coursesResponse.isSuccessful && coursesResponse.body() != null) {
                    val courses = coursesResponse.body()!!
                    Log.d("CourseViewModel", "✅ Found ${courses.size} courses")
                    val coursesWithRatings = mutableListOf<CourseWithRatings>()

                    // Per ogni corso, ottieni il docente e le votazioni
                    for (course in courses) {
                        try {
                            Log.d("CourseViewModel", "🔄 Processing course ID: ${course.id}")

                            // Ottieni il docente
                            val teacherResponse = ApiClient.instance.getCourseTeacher(course.id)
                            Log.d("CourseViewModel", "👨‍🏫 Teacher response for course ${course.id}: ${teacherResponse.code()}")
                            val teacherName = if (teacherResponse.isSuccessful) {
                                (teacherResponse.body()?.get("name") as? String) ?: "N/A"
                            } else "N/A"

                            // Ottieni le votazioni medie
                            val ratingsResponse = ApiClient.instance.getCourseRatings(course.id)
                            Log.d("CourseViewModel", "⭐ Ratings response for course ${course.id}: ${ratingsResponse.code()}")

                            val (avgClarity, avgFeasibility, avgAvailability) = if (ratingsResponse.isSuccessful) {
                                val ratings = ratingsResponse.body()
                                Log.d("CourseViewModel", "📊 Ratings body: $ratings")
                                Triple(
                                    (ratings?.get("average_clarity") as? Number)?.toDouble() ?: 0.0,
                                    (ratings?.get("average_feasibility") as? Number)?.toDouble() ?: 0.0,
                                    (ratings?.get("average_availability") as? Number)?.toDouble() ?: 0.0
                                )
                            } else {
                                Log.d("CourseViewModel", "⚠️ No ratings for course ${course.id}, using defaults")
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
                            Log.d("CourseViewModel", "✅ Added course ${course.id} to list")

                        } catch (e: Exception) {
                            Log.e("CourseViewModel", "❌ Error processing course ${course.id}: ${e.message}", e)
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

                    Log.d("CourseViewModel", "🎉 All courses processed! Total: ${coursesWithRatings.size}")
                    _courseListState.value = CourseListState.Success(coursesWithRatings)
                    Log.d("CourseViewModel", "✅ CourseListState updated to Success")

                } else {
                    Log.e("CourseViewModel", "❌ Courses response failed: ${coursesResponse.code()}")
                    _courseListState.value = CourseListState.Error("Errore nel caricamento dei corsi: ${coursesResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "💥 EXCEPTION in loadCoursesByFaculty: ${e.message}", e)
                e.printStackTrace()
                _courseListState.value = CourseListState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}