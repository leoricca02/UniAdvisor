package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.data.ReviewCreate
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReviewState {
    object Idle : ReviewState()
    object Loading : ReviewState()
    data class Success(val review: Review) : ReviewState()
    data class Error(val message: String) : ReviewState()
}

class ReviewViewModel : ViewModel() {
    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState

    fun addReview(courseId: Int, review: ReviewCreate) {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading
            try {
                val response = ApiClient.instance.addReview(courseId, review)

                if (response.isSuccessful && response.body() != null) {
                    _reviewState.value = ReviewState.Success(response.body()!!)
                } else {
                    _reviewState.value = ReviewState.Error(
                        "Errore nell'aggiunta della recensione: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _reviewState.value = ReviewState.Error(
                    e.message ?: "Errore di connessione"
                )
            }
        }
    }

    fun resetState() {
        _reviewState.value = ReviewState.Idle
    }
}