// file: ui/screens/CoursesListScreen.kt
package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.Course
import com.riccaturrini.uniadvisor.viewmodel.CoursesUiState
import com.riccaturrini.uniadvisor.viewmodel.CoursesViewModel

@Composable
fun CoursesListScreen(
    faculty: String, // Passata dalla schermata precedente
    coursesViewModel: CoursesViewModel = viewModel()
) {
    // Carica i corsi la prima volta che la schermata viene visualizzata
    LaunchedEffect(key1 = faculty) {
        coursesViewModel.loadCourses(faculty)
    }

    val uiState by coursesViewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CoursesUiState.Loading -> {
                // Mostra un indicatore di caricamento
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is CoursesUiState.Success -> {
                // Mostra la lista dei corsi
                CourseList(courses = state.courses)
            }
            is CoursesUiState.Error -> {
                // Mostra un messaggio di errore
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = state.message)
                }
            }
        }
    }
}

@Composable
fun CourseList(courses: List<Course>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(courses) { course ->
            CourseItem(course = course)
        }
    }
}

@Composable
fun CourseItem(course: Course) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = course.name, style = MaterialTheme.typography.titleMedium)
            Text(text = course.professor, style = MaterialTheme.typography.bodySmall)
        }
    }
}