package com.riccaturrini.uniadvisor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.data.Faculty
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.data.UserResponse
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ============= ENROLLMENT STATES (per SelectFacultyScreen) =============
sealed class EnrollmentState {
    object Idle : EnrollmentState()
    object Loading : EnrollmentState()
    object Success : EnrollmentState()
    data class Error(val message: String) : EnrollmentState()
}

// ============= PROFILE STATES (per ProfileScreen) =============
data class ProfileStats(
    val notesCount: Int = 0,
    val reviewsCount: Int = 0,
    val noteRatingsCount: Int = 0
)

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: UserResponse,
        val faculty: Faculty?,
        val stats: ProfileStats
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

// ============= UNIFIED PROFILE VIEW MODEL =============
class ProfileViewModel : ViewModel() {
    private val apiService = ApiClient.instance

    // Stati per enrollment (usati da SelectFacultyScreen)
    private val _enrollState = MutableStateFlow<EnrollmentState>(EnrollmentState.Idle)
    val enrollState: StateFlow<EnrollmentState> = _enrollState

    // Stati per profilo (usati da ProfileScreen)
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState

    private val _faculties = MutableStateFlow<List<Faculty>>(emptyList())
    val faculties: StateFlow<List<Faculty>> = _faculties

    // ============= ENROLLMENT FUNCTIONS =============

    /**
     * Iscrive l'utente a una facoltà (usato da SelectFacultyScreen)
     */
    fun enrollInFaculty(facultyId: Int) {
        viewModelScope.launch {
            _enrollState.value = EnrollmentState.Loading
            try {
                val response = apiService.enrollInFaculty(facultyId)
                if (response.isSuccessful) {
                    _enrollState.value = EnrollmentState.Success
                } else {
                    _enrollState.value = EnrollmentState.Error("Errore durante l'iscrizione: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error enrolling in faculty", e)
                _enrollState.value = EnrollmentState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    // ============= PROFILE FUNCTIONS =============

    /**
     * Carica il profilo completo dell'utente con statistiche
     */
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            Log.d("ProfileViewModel", "🔄 Starting profile load...")

            try {
                // Carica il profilo utente
                Log.d("ProfileViewModel", "📡 Fetching user profile...")
                val userResponse = apiService.getMyProfile()
                Log.d("ProfileViewModel", "📡 User profile response code: ${userResponse.code()}")

                if (!userResponse.isSuccessful || userResponse.body() == null) {
                    val errorMsg = "Errore nel caricamento del profilo (${userResponse.code()})"
                    Log.e("ProfileViewModel", "❌ $errorMsg")
                    _profileState.value = ProfileUiState.Error(errorMsg)
                    return@launch
                }
                val user = userResponse.body()!!
                Log.d("ProfileViewModel", "✅ User profile loaded: ${user.email}")

                // Carica la facoltà (se l'utente è iscritto)
                Log.d("ProfileViewModel", "📡 Fetching faculty...")
                val faculty = try {
                    val facultyResponse = apiService.getMyFaculty()
                    Log.d("ProfileViewModel", "📡 Faculty response code: ${facultyResponse.code()}")

                    if (facultyResponse.isSuccessful && facultyResponse.body() != null) {
                        Log.d("ProfileViewModel", "✅ Faculty loaded: ${facultyResponse.body()?.name}")
                        facultyResponse.body()
                    } else {
                        Log.w("ProfileViewModel", "⚠️ Faculty not found (${facultyResponse.code()}), user may not be enrolled yet")
                        null
                    }
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "⚠️ Exception fetching faculty: ${e.message}")
                    null
                }

                // Carica le statistiche
                Log.d("ProfileViewModel", "📊 Loading stats...")
                val stats = loadStats()
                Log.d("ProfileViewModel", "✅ Stats loaded: notes=${stats.notesCount}, reviews=${stats.reviewsCount}, ratings=${stats.noteRatingsCount}")

                _profileState.value = ProfileUiState.Success(user, faculty, stats)
                Log.d("ProfileViewModel", "✅ Profile load completed successfully!")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Errore di connessione"
                Log.e("ProfileViewModel", "💥 Error loading profile: $errorMsg", e)
                _profileState.value = ProfileUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Carica le statistiche dell'utente (appunti, recensioni, valutazioni)
     */
    private suspend fun loadStats(): ProfileStats {
        var notesCount = 0
        var reviewsCount = 0
        var noteRatingsCount = 0

        // Carica appunti
        try {
            Log.d("ProfileViewModel", "📝 Fetching notes...")
            val notesResponse = apiService.getMyNotes()
            Log.d("ProfileViewModel", "📝 Notes response code: ${notesResponse.code()}")

            if (notesResponse.isSuccessful && notesResponse.body() != null) {
                notesCount = notesResponse.body()!!.size
                Log.d("ProfileViewModel", "✅ Notes count: $notesCount")
            } else {
                Log.w("ProfileViewModel", "⚠️ Notes not found or empty (${notesResponse.code()})")
            }
        } catch (e: Exception) {
            Log.w("ProfileViewModel", "⚠️ Error loading notes: ${e.message}")
        }

        // Carica recensioni corsi
        try {
            Log.d("ProfileViewModel", "⭐ Fetching reviews...")
            val reviewsResponse = apiService.getMyReviews()
            Log.d("ProfileViewModel", "⭐ Reviews response code: ${reviewsResponse.code()}")

            if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                reviewsCount = reviewsResponse.body()!!.size
                Log.d("ProfileViewModel", "✅ Reviews count: $reviewsCount")
            } else {
                Log.w("ProfileViewModel", "⚠️ Reviews not found or empty (${reviewsResponse.code()})")
            }
        } catch (e: Exception) {
            Log.w("ProfileViewModel", "⚠️ Error loading reviews: ${e.message}")
        }

        // Carica valutazioni note
        try {
            Log.d("ProfileViewModel", "📊 Fetching note ratings...")
            val ratingsResponse = apiService.getMyNoteRatings()
            Log.d("ProfileViewModel", "📊 Note ratings response code: ${ratingsResponse.code()}")

            if (ratingsResponse.isSuccessful && ratingsResponse.body() != null) {
                noteRatingsCount = ratingsResponse.body()!!.size
                Log.d("ProfileViewModel", "✅ Note ratings count: $noteRatingsCount")
            } else {
                Log.w("ProfileViewModel", "⚠️ Note ratings not found or empty (${ratingsResponse.code()})")
            }
        } catch (e: Exception) {
            Log.w("ProfileViewModel", "⚠️ Error loading note ratings: ${e.message}")
        }

        return ProfileStats(notesCount, reviewsCount, noteRatingsCount)
    }

    /**
     * Aggiorna il profilo utente
     */
    fun updateProfile(profileData: UserProfileCreate) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            try {
                val response = apiService.updateMyProfile(profileData)
                if (response.isSuccessful) {
                    _updateState.value = ProfileUpdateState.Success
                    loadProfile() // Ricarica il profilo aggiornato
                } else {
                    _updateState.value = ProfileUpdateState.Error("Errore nell'aggiornamento: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
                _updateState.value = ProfileUpdateState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    /**
     * Carica tutte le facoltà disponibili
     */
    fun loadFaculties() {
        viewModelScope.launch {
            try {
                val response = apiService.getFaculties()
                if (response.isSuccessful && response.body() != null) {
                    _faculties.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading faculties", e)
            }
        }
    }

    /**
     * Cambia la facoltà dell'utente
     */
    fun changeFaculty(newFacultyId: Int, courseViewModel: CourseViewModel? = null) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading

            try {
                val response = apiService.changeFaculty(newFacultyId)

                if (response.isSuccessful) {
                    courseViewModel?.resetCourseList()

                    // Reload profile to get updated faculty
                    loadProfile()
                    _updateState.value = ProfileUpdateState.Success
                } else {
                    _updateState.value = ProfileUpdateState.Error("Failed to change faculty")
                }
            } catch (e: Exception) {
                _updateState.value = ProfileUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Elimina l'account utente
     */
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            try {
                val response = apiService.deleteMyAccount()
                if (response.isSuccessful) {
                    // Logout da Firebase
                    Firebase.auth.signOut()
                    _updateState.value = ProfileUpdateState.Success
                    onSuccess()
                } else {
                    _updateState.value = ProfileUpdateState.Error("Errore nell'eliminazione: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting account", e)
                _updateState.value = ProfileUpdateState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    /**
     * Effettua il logout
     */
    fun logout(onSuccess: () -> Unit) {
        Firebase.auth.signOut()
        onSuccess()
    }

    /**
     * Resetta lo stato di aggiornamento
     */
    fun resetUpdateState() {
        _updateState.value = ProfileUpdateState.Idle
    }
}