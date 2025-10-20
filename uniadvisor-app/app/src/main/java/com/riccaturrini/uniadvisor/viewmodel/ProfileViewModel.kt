// File: viewmodel/ProfileViewModel.kt
package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Rinominato per evitare conflitto con AuthViewModel
sealed class EnrollmentState {
    object Idle : EnrollmentState()
    object Loading : EnrollmentState()
    object Success : EnrollmentState()
    data class Error(val message: String) : EnrollmentState()
}

class ProfileViewModel : ViewModel() {
    private val _enrollState = MutableStateFlow<EnrollmentState>(EnrollmentState.Idle)
    val enrollState: StateFlow<EnrollmentState> = _enrollState

    fun enrollInFaculty(facultyId: Int) {
        viewModelScope.launch {
            _enrollState.value = EnrollmentState.Loading
            try {
                val response = ApiClient.instance.enrollInFaculty(facultyId)
                if (response.isSuccessful) {
                    _enrollState.value = EnrollmentState.Success
                } else {
                    _enrollState.value = EnrollmentState.Error("Errore durante l'iscrizione: ${response.code()}")
                }
            } catch (e: Exception) {
                _enrollState.value = EnrollmentState.Error(e.message ?: "Errore di connessione")
            }
        }
    }
}