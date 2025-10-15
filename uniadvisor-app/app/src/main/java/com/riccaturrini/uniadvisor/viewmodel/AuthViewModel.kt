// File: viewmodel/AuthViewModel.kt
package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Suppress("unused")
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@Suppress("unused")
class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signIn(email: String, password: String) {

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email e password non possono essere vuoti")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Errore di login")
            }
        }
    }

    fun createUser(email: String, password: String) {

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email e password non possono essere vuoti")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Errore di registrazione")
            }
        }
    }

    // ... le altre funzioni (signInWithGoogle, resetState) rimangono invariate ...
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _uiState.value = AuthUiState.Success
                Log.d("AuthViewModel", "Google Sign-In successful")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Errore di accesso con Google")
                Log.e("AuthViewModel", "Google Sign-In failed", e)
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}