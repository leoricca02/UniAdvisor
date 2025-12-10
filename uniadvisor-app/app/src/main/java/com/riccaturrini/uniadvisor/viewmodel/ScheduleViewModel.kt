package com.riccaturrini.uniadvisor.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riccaturrini.uniadvisor.data.Lesson
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Stato della UI
sealed class ScheduleUiState {
    object Loading : ScheduleUiState()
    data class Success(
        val allLessons: List<Lesson>,       // Tutte le lezioni scaricate
        val filteredLessons: List<Lesson>,  // Lezioni da mostrare (filtrate)
        val availableCourses: List<String>  // Nomi dei corsi per il filtro
    ) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
}

class ScheduleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Stato dei filtri
    private val _selectedDay = MutableStateFlow("Monday")
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _selectedCourses = MutableStateFlow<Set<String>>(emptySet()) // Nomi dei corsi selezionati
    val selectedCourses: StateFlow<Set<String>> = _selectedCourses.asStateFlow()

    fun loadSchedule(facultyId: Int) {
        viewModelScope.launch {
            _uiState.value = ScheduleUiState.Loading
            try {
                // 1. Scarichiamo i corsi della facoltà
                val coursesResponse = ApiClient.instance.getCoursesByFaculty(facultyId)

                if (coursesResponse.isSuccessful && coursesResponse.body() != null) {
                    val courses = coursesResponse.body()!!

                    // 2. Scarichiamo le lezioni per OGNI corso in parallelo
                    val lessonsDeferred = courses.map { course ->
                        async {
                            val response = ApiClient.instance.getLessonsByCourse(course.id)
                            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                        }
                    }

                    // Aspettiamo che tutti i download finiscano e uniamo le liste
                    val nestedLessons = lessonsDeferred.awaitAll()
                    val allLessons = nestedLessons.flatten()

                    // Lista univoca dei nomi dei corsi per il filtro
                    val courseNames = allLessons.map { it.course.name }.distinct().sorted()

                    // Aggiorniamo lo stato iniziale (senza filtri sui corsi, solo giorno default)
                    updateUiState(allLessons, _selectedDay.value, emptySet(), courseNames)
                } else {
                    _uiState.value = ScheduleUiState.Error("Failed to load courses")
                }
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun onDaySelected(day: String) {
        _selectedDay.value = day
        applyFilters()
    }

    fun onCourseFilterChanged(courseName: String, isSelected: Boolean) {
        val currentSelection = _selectedCourses.value.toMutableSet()
        if (isSelected) {
            currentSelection.add(courseName)
        } else {
            currentSelection.remove(courseName)
        }
        _selectedCourses.value = currentSelection
        applyFilters()
    }

    fun clearCourseFilter() {
        _selectedCourses.value = emptySet()
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        if (currentState is ScheduleUiState.Success) {
            updateUiState(
                currentState.allLessons,
                _selectedDay.value,
                _selectedCourses.value,
                currentState.availableCourses
            )
        }
    }

    private fun updateUiState(
        allLessons: List<Lesson>,
        day: String,
        selectedCourses: Set<String>,
        availableCourses: List<String>
    ) {
        // Logica di Filtro
        val filtered = allLessons.filter { lesson ->
            val matchesDay = lesson.dayOfWeek.equals(day, ignoreCase = true)
            // Se il set è vuoto mostra tutto, altrimenti controlla se il nome è nel set
            val matchesCourse = if (selectedCourses.isEmpty()) true else selectedCourses.contains(lesson.course.name)

            matchesDay && matchesCourse
        }.sortedBy { it.startTime } // Ordina per orario

        _uiState.value = ScheduleUiState.Success(
            allLessons = allLessons,
            filteredLessons = filtered,
            availableCourses = availableCourses
        )
    }
}