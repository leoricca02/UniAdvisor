package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Faculty
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class FacultyUiState {
    object Loading : FacultyUiState()
    data class Success(val faculties: List<Faculty>) : FacultyUiState()
    data class Error(val message: String) : FacultyUiState()
}

class FacultyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FacultyUiState>(FacultyUiState.Loading)
    val uiState: StateFlow<FacultyUiState> = _uiState

    init {
        fetchFaculties()
    }

    private fun fetchFaculties() {
        viewModelScope.launch {
            _uiState.value = FacultyUiState.Loading
            try {
                val response = ApiClient.instance.getFaculties()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = FacultyUiState.Success(response.body()!!)
                } else {
                    _uiState.value = FacultyUiState.Error("Errore nel caricamento delle facoltà: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = FacultyUiState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}