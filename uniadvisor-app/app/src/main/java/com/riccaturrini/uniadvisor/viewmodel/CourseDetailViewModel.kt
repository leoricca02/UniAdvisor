package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CourseDetailData(
    val course: Course,
    val teacherName: String,
    val avgClarity: Double,
    val avgFeasibility: Double,
    val avgAvailability: Double,
    val reviews: List<Review>
)

sealed class CourseDetailState {
    object Loading : CourseDetailState()
    data class Success(val data: CourseDetailData) : CourseDetailState()
    data class Error(val message: String) : CourseDetailState()
}

class CourseDetailViewModel : ViewModel() {
    private val _courseDetailState = MutableStateFlow<CourseDetailState>(CourseDetailState.Loading)
    val courseDetailState: StateFlow<CourseDetailState> = _courseDetailState

    fun loadCourseDetail(courseId: Int) {
        viewModelScope.launch {
            _courseDetailState.value = CourseDetailState.Loading
            try {
                // Get course details
                val courseResponse = ApiClient.instance.getCourseDetail(courseId)
                if (!courseResponse.isSuccessful || courseResponse.body() == null) {
                    _courseDetailState.value = CourseDetailState.Error("Corso non trovato")
                    return@launch
                }
                val course = courseResponse.body()!!

                // Get teacher
                val teacherResponse = ApiClient.instance.getCourseTeacher(courseId)
                val teacherName = if (teacherResponse.isSuccessful) {
                    (teacherResponse.body()?.get("name") as? String) ?: "N/A"
                } else "N/A"

                // Get ratings
                val ratingsResponse = ApiClient.instance.getCourseRatings(courseId)
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

                // Get reviews
                val reviewsResponse = ApiClient.instance.getCourseReviews(courseId)
                val reviews = if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                    reviewsResponse.body()!!
                } else {
                    emptyList()
                }

                _courseDetailState.value = CourseDetailState.Success(
                    CourseDetailData(
                        course = course,
                        teacherName = teacherName,
                        avgClarity = avgClarity,
                        avgFeasibility = avgFeasibility,
                        avgAvailability = avgAvailability,
                        reviews = reviews
                    )
                )
            } catch (e: Exception) {
                _courseDetailState.value = CourseDetailState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}