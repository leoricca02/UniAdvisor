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
    data class Success(val reviews: List<Review>) : MyReviewsUiState()
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
                            _reviewsState.value = MyReviewsUiState.Success(reviews)
                        }
                    }
                    response.code() == 404 -> {
                        // 404 means no reviews yet - this is NOT an error!
                        Log.d("MyReviewsViewModel", "ℹ️ No reviews found (404) - showing empty state")
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