// file: viewmodel/CoursesViewModel.kt
package com.riccaturrini.uniadvisor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Definisce i possibili stati della UI
sealed class CoursesUiState {
    object Loading : CoursesUiState()
    data class Success(val courses: List<Course>) : CoursesUiState()
    data class Error(val message: String) : CoursesUiState()
}

class CoursesViewModel : ViewModel() {

    private val repository = CourseRepository()

    // Flusso di dati privato e modificabile
    private val _uiState = MutableStateFlow<CoursesUiState>(CoursesUiState.Loading)
    // Flusso di dati pubblico e di sola lettura per la UI
    val uiState: StateFlow<CoursesUiState> = _uiState

    fun loadCourses(faculty: String) {
        viewModelScope.launch {
            _uiState.value = CoursesUiState.Loading
            try {
                val courses = repository.getCoursesByFaculty(faculty)
                _uiState.value = CoursesUiState.Success(courses)
            } catch (e: Exception) {
                _uiState.value = CoursesUiState.Error("Impossibile caricare i corsi.")
            }
        }
    }
}