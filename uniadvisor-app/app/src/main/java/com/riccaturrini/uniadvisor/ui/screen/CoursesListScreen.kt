package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.viewmodel.CourseListState
import com.riccaturrini.uniadvisor.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesListScreen(
    facultyId: Int,
    onBackPressed: () -> Unit,
    onCourseClick: (Int) -> Unit,
    courseViewModel: CourseViewModel = viewModel()
) {
    val courseListState by courseViewModel.courseListState.collectAsState()

    // Load courses when screen is displayed
    LaunchedEffect(facultyId) {
        courseViewModel.loadCoursesByFaculty(facultyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I Miei Corsi") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (val state = courseListState) {
            is CourseListState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CourseListState.Success -> {
                if (state.courses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nessun corso disponibile per questa facoltà")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.courses) { courseWithRatings ->
                            CourseCard(
                                courseName = courseWithRatings.course.name,
                                teacherName = courseWithRatings.teacherName,
                                avgClarity = courseWithRatings.avgClarity,
                                avgFeasibility = courseWithRatings.avgFeasibility,
                                avgAvailability = courseWithRatings.avgAvailability,
                                onClick = { onCourseClick(courseWithRatings.course.id) }
                            )
                        }
                    }
                }
            }

            is CourseListState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { courseViewModel.loadCoursesByFaculty(facultyId) }) {
                            Text("Riprova")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(
    courseName: String,
    teacherName: String,
    avgClarity: Double,
    avgFeasibility: Double,
    avgAvailability: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = courseName,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Docente: $teacherName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ratings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingChip("Chiarezza", avgClarity)
                RatingChip("Fattibilità", avgFeasibility)
                RatingChip("Disponibilità", avgAvailability)
            }
        }
    }
}

@Composable
fun RatingChip(label: String, rating: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = if (rating > 0) String.format("%.1f", rating) else "N/A",
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                rating >= 4.0 -> MaterialTheme.colorScheme.primary
                rating >= 2.5 -> MaterialTheme.colorScheme.tertiary
                rating > 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}