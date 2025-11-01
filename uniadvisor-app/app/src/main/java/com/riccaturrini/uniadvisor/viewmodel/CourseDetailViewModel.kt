package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.data.ReviewCreate
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

sealed class AddReviewState {
    object Idle : AddReviewState()
    object Loading : AddReviewState()
    object Success : AddReviewState()
    data class Error(val message: String) : AddReviewState()
}

class CourseDetailViewModel : ViewModel() {

    private val apiService = ApiClient.instance

    private val _courseDetailState = MutableStateFlow<CourseDetailState>(CourseDetailState.Loading)
    val courseDetailState: StateFlow<CourseDetailState> = _courseDetailState

    private val _addReviewState = MutableStateFlow<AddReviewState>(AddReviewState.Idle)
    val addReviewState: StateFlow<AddReviewState> = _addReviewState

    /**
     * Load complete course details including teacher, ratings, and reviews
     */
    fun loadCourseDetail(courseId: Int) {
        viewModelScope.launch {
            _courseDetailState.value = CourseDetailState.Loading
            try {
                Log.d("CourseDetailVM", "📚 Loading course details for ID: $courseId")

                // Get course details
                val courseResponse = apiService.getCourseDetail(courseId)
                if (!courseResponse.isSuccessful || courseResponse.body() == null) {
                    Log.e("CourseDetailVM", "❌ Course not found: ${courseResponse.code()}")
                    _courseDetailState.value = CourseDetailState.Error("Course not found")
                    return@launch
                }
                val course = courseResponse.body()!!
                Log.d("CourseDetailVM", "✅ Course loaded: ${course.name}")

                // Get teacher
                val teacherResponse = apiService.getCourseTeacher(courseId)
                val teacherName = if (teacherResponse.isSuccessful) {
                    (teacherResponse.body()?.get("name") as? String)?.also {
                        Log.d("CourseDetailVM", "✅ Teacher loaded: $it")
                    } ?: "N/A"
                } else {
                    Log.w("CourseDetailVM", "⚠️ Teacher not found")
                    "N/A"
                }

                // Get ratings
                val ratingsResponse = apiService.getCourseRatings(courseId)
                val (avgClarity, avgFeasibility, avgAvailability) = if (ratingsResponse.isSuccessful) {
                    val ratings = ratingsResponse.body()
                    Triple(
                        (ratings?.get("average_clarity") as? Number)?.toDouble() ?: 0.0,
                        (ratings?.get("average_feasibility") as? Number)?.toDouble() ?: 0.0,
                        (ratings?.get("average_availability") as? Number)?.toDouble() ?: 0.0
                    ).also {
                        Log.d("CourseDetailVM", "✅ Ratings loaded: clarity=${it.first}, feasibility=${it.second}, availability=${it.third}")
                    }
                } else {
                    Log.w("CourseDetailVM", "⚠️ No ratings found (${ratingsResponse.code()})")
                    Triple(0.0, 0.0, 0.0)
                }

                // Get reviews
                val reviewsResponse = apiService.getCourseReviews(courseId)
                val reviews = if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                    reviewsResponse.body()!!.also {
                        Log.d("CourseDetailVM", "✅ Loaded ${it.size} reviews")
                    }
                } else {
                    Log.w("CourseDetailVM", "⚠️ No reviews found (${reviewsResponse.code()})")
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

                Log.d("CourseDetailVM", "🎉 Course detail loaded successfully!")

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("CourseDetailVM", "💥 Error loading course detail: $errorMsg", e)
                _courseDetailState.value = CourseDetailState.Error(errorMsg)
            }
        }
    }

    /**
     * Add a new review for the course
     */
    fun addReview(courseId: Int, review: ReviewCreate) {
        viewModelScope.launch {
            _addReviewState.value = AddReviewState.Loading
            try {
                Log.d("CourseDetailVM", "📝 Adding review for course $courseId")

                val response = apiService.addReview(courseId, review)

                if (response.isSuccessful) {
                    Log.d("CourseDetailVM", "✅ Review added successfully")
                    _addReviewState.value = AddReviewState.Success
                    // Reload course details to show new review
                    loadCourseDetail(courseId)
                } else {
                    val errorMsg = "Failed to add review: ${response.code()}"
                    Log.e("CourseDetailVM", "❌ $errorMsg")
                    _addReviewState.value = AddReviewState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("CourseDetailVM", "💥 Error adding review: $errorMsg", e)
                _addReviewState.value = AddReviewState.Error(errorMsg)
            }
        }
    }

    fun resetAddReviewState() {
        _addReviewState.value = AddReviewState.Idle
    }
}