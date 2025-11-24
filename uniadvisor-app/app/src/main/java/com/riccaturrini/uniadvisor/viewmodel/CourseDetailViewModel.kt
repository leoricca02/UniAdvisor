package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.*
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailData(
    val course: Course,
    val teacherName: String,
    val avgClarity: Double,
    val avgFeasibility: Double,
    val avgAvailability: Double,
    val reviews: List<Review>,
    val notes: List<NoteWithRating>
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

sealed class NoteRatingState {
    object Idle : NoteRatingState()
    object Loading : NoteRatingState()
    object Success : NoteRatingState()
    data class Error(val message: String) : NoteRatingState()
}

class CourseDetailViewModel : ViewModel() {

    private val apiService = ApiClient.instance

    private val _courseDetailState = MutableStateFlow<CourseDetailState>(CourseDetailState.Loading)
    val courseDetailState: StateFlow<CourseDetailState> = _courseDetailState.asStateFlow()

    private val _addReviewState = MutableStateFlow<AddReviewState>(AddReviewState.Idle)
    val addReviewState: StateFlow<AddReviewState> = _addReviewState.asStateFlow()

    // Note rating state
    private val _noteRatingState = MutableStateFlow<NoteRatingState>(NoteRatingState.Idle)
    val noteRatingState: StateFlow<NoteRatingState> = _noteRatingState.asStateFlow()

    // Current user id (null if not loaded yet)
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        // Load current user id once ViewModel is created
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val response = apiService.getMyProfile()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // Se UserResponse ha 'id' come Int
                    _currentUserId.value = user.id
                    Log.d("CourseDetailVM", "Loaded current user id: ${user.id}")
                } else {
                    Log.w("CourseDetailVM", "Could not load current user profile: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "Error loading current user", e)
            }
        }
    }

    /**
     * Load complete course details including teacher, ratings, reviews, and notes with ratings
     */
    fun loadCourseDetail(courseId: Int, noteSortOrder: String = "desc") {
        viewModelScope.launch {
            _courseDetailState.value = CourseDetailState.Loading
            try {
                Log.d("CourseDetailVM", "📚 Loading course details for ID: $courseId with sort order: $noteSortOrder")

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
                    (teacherResponse.body()?.get("name") as? String) ?: "N/A"
                } else {
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
                    )
                } else {
                    Triple(0.0, 0.0, 0.0)
                }

                // Get reviews
                val reviewsResponse = apiService.getCourseReviews(courseId)
                val reviews = if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                    reviewsResponse.body()!!
                } else {
                    emptyList()
                }

                // Get notes WITH ratings
                val notesResponse = apiService.getNotesWithRatings(courseId, order = noteSortOrder)
                val notes = if (notesResponse.isSuccessful && notesResponse.body() != null) {
                    notesResponse.body()!!
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
                        reviews = reviews,
                        notes = notes
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

    // Add rating to a note
    fun addNoteRating(courseId: Int, noteId: Int, rating: Int, comment: String? = null) {
        viewModelScope.launch {
            _noteRatingState.value = NoteRatingState.Loading
            try {
                Log.d("CourseDetailVM", "⭐ Adding rating $rating to note $noteId")

                val ratingCreate = NoteRatingCreate(
                    note_id = noteId,
                    rating = rating,
                    comment = comment
                )

                val response = apiService.addNoteRating(ratingCreate)

                if (response.isSuccessful) {
                    Log.d("CourseDetailVM", "✅ Note rating added successfully")
                    _noteRatingState.value = NoteRatingState.Success
                } else {
                    val errorMsg = when (response.code()) {
                        403 -> "You cannot rate your own note"
                        400 -> "You have already rated this note"
                        else -> "Failed to add rating: ${response.code()}"
                    }
                    Log.e("CourseDetailVM", "❌ $errorMsg")
                    _noteRatingState.value = NoteRatingState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("CourseDetailVM", "💥 Error adding note rating: $errorMsg", e)
                _noteRatingState.value = NoteRatingState.Error(errorMsg)
            }
        }
    }

    /**
     * Update an existing note rating (only allowed by the author)
     */
    fun updateNoteReview(
        ratingId: Int,
        rating: Int,
        comment: String?
    ) {
        viewModelScope.launch {
            _noteRatingState.value = NoteRatingState.Loading
            try {
                Log.d("CourseDetailVM", "✏️ Updating rating $ratingId -> rating=$rating comment=${comment ?: ""}")

                val ratingUpdate = NoteRatingUpdate(
                    rating = rating,
                    comment = comment
                )

                val response = apiService.updateNoteRating(ratingId, ratingUpdate)

                if (response.isSuccessful && response.body() != null) {
                    Log.d("CourseDetailVM", "✅ Note rating updated successfully")
                    _noteRatingState.value = NoteRatingState.Success
                } else {
                    val err = "Failed to update rating: ${response.code()}"
                    Log.e("CourseDetailVM", "❌ $err")
                    _noteRatingState.value = NoteRatingState.Error(err)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("CourseDetailVM", "💥 Error updating note rating: $errorMsg", e)
                _noteRatingState.value = NoteRatingState.Error(errorMsg)
            }
        }
    }

    fun resetAddReviewState() {
        _addReviewState.value = AddReviewState.Idle
    }

    fun resetNoteRatingState() {
        _noteRatingState.value = NoteRatingState.Idle
    }
}
