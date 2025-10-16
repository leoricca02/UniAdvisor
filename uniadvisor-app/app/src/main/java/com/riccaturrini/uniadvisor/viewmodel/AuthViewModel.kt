// File: viewmodel/AuthViewModel.kt
package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    object ProfileCreationRequired : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Login con email e password.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    Firebase.auth.signInWithEmailAndPassword(email, password).await()
                }
                checkUserProfile()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Login failed: ${e.message}")
            }
        }
    }

    /**
     * Login tramite Google ID Token.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                withContext(Dispatchers.IO) {
                    Firebase.auth.signInWithCredential(credential).await()
                }
                checkUserProfile()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Google sign-in failed: ${e.message}")
            }
        }
    }

    /**
     * Registrazione con email e password.
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                }
                _uiState.value = AuthUiState.ProfileCreationRequired
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign-up failed: ${e.message}")
            }
        }
    }

    /**
     * Funzione combinata per creare utente Firebase e profilo backend.
     */
    fun createUserAndProfile(email: String, password: String, profileData: UserProfileCreate) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                    repository.createUserProfile(profileData)
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign-up or profile creation failed: ${e.message}")
            }
        }
    }

    /**
     * Crea solo il profilo utente dopo la registrazione.
     */
    fun createProfile(profileData: UserProfileCreate) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    repository.createUserProfile(profileData)
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Profile creation failed: ${e.message}")
            }
        }
    }

    /**
     * Controlla se esiste un profilo utente nel backend.
     */
    private fun checkUserProfile() {
        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    repository.getMyProfile()
                }
                _uiState.value = if (profile == null) {
                    AuthUiState.ProfileCreationRequired
                } else {
                    AuthUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Profile check failed: ${e.message}")
            }
        }
    }
}
