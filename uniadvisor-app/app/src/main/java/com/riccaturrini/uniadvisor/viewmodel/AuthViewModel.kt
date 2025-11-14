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

    /**
     * Check if user profile exists in backend
     * IMPROVED: Better error handling to distinguish between "profile not found" and network errors
     */
    fun checkUserProfile() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔍 Checking user profile...")
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                Log.w("AuthViewModel", "⚠️ No Firebase user found")
                _authUiState.value = AuthUiState.Error("No user logged in")
                return@launch
            }

            Log.d("AuthViewModel", "Firebase user: ${firebaseUser.uid}")

            try {
                _authUiState.value = AuthUiState.Loading

                // Get fresh token
                val token = firebaseUser.getIdToken(true).await().token
                if (token == null) {
                    Log.e("AuthViewModel", "❌ Failed to get auth token")
                    _authUiState.value = AuthUiState.Error("Failed to get auth token")
                    return@launch
                }

                Log.d("AuthViewModel", "✅ Got Firebase token, calling backend...")

                // Try to get profile from backend
                val profileResponse = userRepository.getMyProfile()

                if (profileResponse != null) {
                    // ✅ AGGIUNTO: Log dettagliato della risposta
                    Log.d("AuthViewModel", "✅ Profile loaded successfully")
                    Log.d("AuthViewModel", "   User ID: ${profileResponse.id}")
                    Log.d("AuthViewModel", "   Email: ${profileResponse.email}")
                    Log.d("AuthViewModel", "   Name: ${profileResponse.first_name} ${profileResponse.last_name}")
                    Log.d("AuthViewModel", "   Faculty ID: ${profileResponse.faculty_id}")
                    Log.d("AuthViewModel", "   Faculty Name: ${profileResponse.faculty_name}") // ✅ Verifica questo!

                    _currentUserData.value = profileResponse
                    _authUiState.value = AuthUiState.Success
                } else {
                    Log.w("AuthViewModel", "⚠️ Profile not found in backend - need to create")
                    _authUiState.value = AuthUiState.ProfileCreationRequired
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "💥 Error checking profile: ${e.message}", e)
                _authUiState.value = AuthUiState.Error(e.message ?: "Error checking profile")
            }
        }
    }

    /**
     * Create user profile in backend
     * IMPROVED: Better handling of duplicate profile attempts
     */
    fun createUserAndProfile(profileData: UserProfileCreate) {
        viewModelScope.launch {
            _profileState.value = ProfileCreationState.Loading

            try {
                val token = auth.currentUser?.getIdToken(false)?.await()?.token
                if (token == null) {
                    Log.e("AuthViewModel", "❌ Token is null")
                    _profileState.value = ProfileCreationState.Error("User not authenticated")
                    return@launch
                }

                Log.d("AuthViewModel", "🟢 Creating profile with data: $profileData")
                val response = userRepository.createProfile(profileData)

                if (response != null) {
                    Log.d("AuthViewModel", "✅ Profile created successfully: $response")
                    _currentUserData.value = response
                    _profileState.value = ProfileCreationState.Success
                } else {
                    // Profile creation failed - might already exist
                    Log.e("AuthViewModel", "❌ Profile creation returned null")

                    // Try to fetch existing profile instead
                    Log.d("AuthViewModel", "🔄 Attempting to fetch existing profile...")
                    val existingProfile = userRepository.getMyProfile()

                    if (existingProfile != null) {
                        Log.d("AuthViewModel", "✅ Found existing profile: $existingProfile")
                        _currentUserData.value = existingProfile
                        _profileState.value = ProfileCreationState.Success
                    } else {
                        _profileState.value = ProfileCreationState.Error(
                            "Profile creation failed. Please try again or contact support."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "💥 Profile creation exception: ${e.message}", e)
                _profileState.value = ProfileCreationState.Error(
                    e.message ?: "An unexpected error occurred"
                )
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
                _authUiState.value = AuthUiState.Error(e.message ?: "Login failed")
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
                _authUiState.value = AuthUiState.Error(e.message ?: "Google login failed")
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
                _authUiState.value = AuthUiState.Error(e.message ?: "Registration failed")
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
                    Log.e("AuthViewModel", "Profile creation returned null")
                    _authUiState.value = AuthUiState.Error("Error creating profile. Please try again.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "SignUp with profile exception: ${e.message}", e)
                _authUiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    /**
     * Sign out from Firebase
     * This clears the Firebase session
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                Firebase.auth.signOut()
                _currentUserData.value = null
                _authUiState.value = AuthUiState.Idle
                _profileState.value = ProfileCreationState.Idle
                Log.d("AuthViewModel", "✅ User signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during sign out: ${e.message}", e)
            }
        }
    }

    fun resetState() {
        _authUiState.value = AuthUiState.Idle
    }

    fun updateCurrentUserData(userData: UserResponse) {
        _currentUserData.value = userData
        Log.d("AuthViewModel", "✅ Updated currentUserData with faculty: ${userData.faculty_id}")
    }
}