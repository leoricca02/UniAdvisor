package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.data.ReviewCreate
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MyReviewsUiState {
    object Loading : MyReviewsUiState()
    data class Success(val reviews: List<MyReviewUiModel>) : MyReviewsUiState()
    object Empty : MyReviewsUiState()
    data class Error(val message: String) : MyReviewsUiState()
}


sealed class ReviewActionState {
    object Idle : ReviewActionState()
    object Loading : ReviewActionState()
    object Success : ReviewActionState()
    data class Error(val message: String) : ReviewActionState()
}

class MyReviewsViewModel : ViewModel() {

    private val apiService = ApiClient.instance

    private val _reviewsState = MutableStateFlow<MyReviewsUiState>(MyReviewsUiState.Loading)
    val reviewsState: StateFlow<MyReviewsUiState> = _reviewsState

    private val _actionState = MutableStateFlow<ReviewActionState>(ReviewActionState.Idle)
    val actionState: StateFlow<ReviewActionState> = _actionState

    /**
     * Load user's reviews
     * FIXED: Handle 404 as "Empty" instead of "Error"
     */
    fun loadMyReviews() {
        viewModelScope.launch {
            _reviewsState.value = MyReviewsUiState.Loading
            try {
                Log.d("MyReviewsViewModel", "📡 Fetching my reviews...")
                val response = apiService.getMyReviews()

                when {
                    response.isSuccessful && response.body() != null -> {
                        val reviews = response.body()!!
                        Log.d("MyReviewsViewModel", "✅ Loaded ${reviews.size} reviews")

                        if (reviews.isEmpty()) {
                            _reviewsState.value = MyReviewsUiState.Empty
                        } else {

                            // Prendi i course_id unici
                            val courseIds = reviews.map { it.course_id }.distinct()

                            // Mappa ID corso → nome corso
                            val courseNames = mutableMapOf<Int, String>()

                            for (id in courseIds) {
                                try {
                                    val courseResponse = apiService.getCourseDetail(id)
                                    if (courseResponse.isSuccessful && courseResponse.body() != null) {
                                        courseNames[id] = courseResponse.body()!!.name
                                    } else {
                                        courseNames[id] = "Course #$id"
                                    }
                                } catch (e: Exception) {
                                    Log.e("MyReviewsViewModel", "⚠️ Error loading course $id name: ${e.message}")
                                    courseNames[id] = "Course #$id"
                                }
                            }

                            // Costruisci lista UI model
                            val uiReviews = reviews.map { review ->
                                MyReviewUiModel(
                                    review = review,
                                    courseName = courseNames[review.course_id] ?: "Course #${review.course_id}"
                                )
                            }

                            _reviewsState.value = MyReviewsUiState.Success(uiReviews)
                        }
                    }

                    response.code() == 404 -> {
                        Log.d("MyReviewsViewModel", "ℹ️ No reviews (404) → empty state")
                        _reviewsState.value = MyReviewsUiState.Empty
                    }

                    else -> {
                        val errorMsg = "Error loading reviews: ${response.code()}"
                        Log.e("MyReviewsViewModel", "❌ $errorMsg")
                        _reviewsState.value = MyReviewsUiState.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("MyReviewsViewModel", "💥 Exception loading reviews: $errorMsg", e)
                _reviewsState.value = MyReviewsUiState.Error(errorMsg)
            }
        }
    }


    /**
     * Update an existing review
     */
    fun updateReview(reviewId: Int, updatedReview: ReviewCreate) {
        viewModelScope.launch {
            _actionState.value = ReviewActionState.Loading
            try {
                Log.d("MyReviewsViewModel", "📝 Updating review $reviewId...")
                val response = apiService.updateReview(reviewId, updatedReview)

                if (response.isSuccessful) {
                    Log.d("MyReviewsViewModel", "✅ Review updated successfully")
                    _actionState.value = ReviewActionState.Success
                    loadMyReviews() // Reload reviews
                } else {
                    val errorMsg = "Update failed: ${response.code()}"
                    Log.e("MyReviewsViewModel", "❌ $errorMsg")
                    _actionState.value = ReviewActionState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("MyReviewsViewModel", "💥 Exception updating review: $errorMsg", e)
                _actionState.value = ReviewActionState.Error(errorMsg)
            }
        }
    }

    /**
     * Delete a review
     */
    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            _actionState.value = ReviewActionState.Loading
            try {
                Log.d("MyReviewsViewModel", "🗑️ Deleting review $reviewId...")
                val response = apiService.deleteReview(reviewId)

                if (response.isSuccessful) {
                    Log.d("MyReviewsViewModel", "✅ Review deleted successfully")
                    _actionState.value = ReviewActionState.Success
                    loadMyReviews() // Reload reviews
                } else {
                    val errorMsg = "Delete failed: ${response.code()}"
                    Log.e("MyReviewsViewModel", "❌ $errorMsg")
                    _actionState.value = ReviewActionState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("MyReviewsViewModel", "💥 Exception deleting review: $errorMsg", e)
                _actionState.value = ReviewActionState.Error(errorMsg)
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ReviewActionState.Idle
    }
}

data class MyReviewUiModel(
    val review: Review,
    val courseName: String
)
