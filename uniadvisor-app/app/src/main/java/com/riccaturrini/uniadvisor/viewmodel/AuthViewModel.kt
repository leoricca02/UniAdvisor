package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import com.riccaturrini.uniadvisor.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object Success : AuthUiState()
    object ProfileCreationRequired : AuthUiState()
}

sealed class ProfileCreationState {
    object Idle : ProfileCreationState()
    object Loading : ProfileCreationState()
    object Success : ProfileCreationState()
    data class Error(val message: String) : ProfileCreationState()
}

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val userRepository = UserRepository()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState

    private val _profileState = MutableStateFlow<ProfileCreationState>(ProfileCreationState.Idle)
    val profileState: StateFlow<ProfileCreationState> = _profileState

    private val _currentUserData = MutableStateFlow<UserResponse?>(null)
    val currentUserData: StateFlow<UserResponse?> = _currentUserData

    fun checkUserProfile() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔍 Checking user profile...")
            val firebaseUser = auth.currentUser
            Log.d("AuthViewModel", "Firebase user: ${firebaseUser?.uid}")
            if (firebaseUser == null) {
                _authUiState.value = AuthUiState.Error("No user logged in")
                return@launch
            }

            try {
                val token = firebaseUser.getIdToken(true).await().token
                if (token == null) {
                    _authUiState.value = AuthUiState.Error("Failed to get auth token")
                    return@launch
                }
                val profileResponse = userRepository.getMyProfile()
                if (profileResponse != null) {
                    Log.d("AuthViewModel", "✅ Profile loaded: $profileResponse")
                    _currentUserData.value = profileResponse
                    _authUiState.value = AuthUiState.Success
                } else {
                    _authUiState.value = AuthUiState.ProfileCreationRequired
                }
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "Error checking profile")
            }
        }
    }

    fun createUserAndProfile(profileData: UserProfileCreate) {
        viewModelScope.launch {
            _profileState.value = ProfileCreationState.Loading
            try {
                val token = auth.currentUser?.getIdToken(false)?.await()?.token
                if (token == null) {
                    Log.e("AuthViewModel", "Token is null")
                    _profileState.value = ProfileCreationState.Error("User not authenticated")
                    return@launch
                }

                Log.d("AuthViewModel", "Creating profile with data: $profileData")
                val response = userRepository.createProfile(profileData)
                if (response != null) {
                    Log.d("AuthViewModel", "Profile created successfully: $response")
                    _currentUserData.value = response
                    _profileState.value = ProfileCreationState.Success
                } else {
                    Log.e("AuthViewModel", "Profile creation returned null")
                    _profileState.value = ProfileCreationState.Error("Profile creation failed: Server returned empty response")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile creation exception: ${e.message}", e)
                _profileState.value = ProfileCreationState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun resetProfileState() {
        _profileState.value = ProfileCreationState.Idle
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                Firebase.auth.signInWithEmailAndPassword(email, password).await()
                checkUserProfile()
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "Login fallito")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                Firebase.auth.signInWithCredential(credential).await()
                checkUserProfile()
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "Login con Google fallito")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                _authUiState.value = AuthUiState.ProfileCreationRequired
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "Registrazione fallita")
            }
        }
    }

    fun signUpWithProfile(email: String, password: String, profileData: UserProfileCreate) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                Firebase.auth.createUserWithEmailAndPassword(email, password).await()

                val token = auth.currentUser?.getIdToken(false)?.await()?.token
                if (token == null) {
                    Log.e("AuthViewModel", "Token is null after signup")
                    _authUiState.value = AuthUiState.Error("Authentication failed")
                    return@launch
                }

                Log.d("AuthViewModel", "Creating profile with data: $profileData")
                val response = userRepository.createProfile(profileData)

                if (response != null) {
                    Log.d("AuthViewModel", "Profile created successfully: $response")
                    _currentUserData.value = response
                    _authUiState.value = AuthUiState.Success
                } else {
                    Log.e("AuthViewModel", "Profile creation returned null - possibly 500 error")
                    _authUiState.value = AuthUiState.Error("Errore nel creare il profilo. Riprova più tardi.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "SignUp with profile exception: ${e.message}", e)
                _authUiState.value = AuthUiState.Error(e.message ?: "Registrazione fallita")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                Firebase.auth.signOut()
                _currentUserData.value = null
                _authUiState.value = AuthUiState.Idle
                _profileState.value = ProfileCreationState.Idle
                Log.d("AuthViewModel", "User signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during sign out: ${e.message}", e)
            }
        }
    }

    fun resetState() {
        _authUiState.value = AuthUiState.Idle
    }
}