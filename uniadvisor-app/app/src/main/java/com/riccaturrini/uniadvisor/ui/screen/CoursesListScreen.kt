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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

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
                title = { Text("My Courses") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Text("No courses available for this faculty")
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
                            Text("Retry")
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
                text = "Teacher: $teacherName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ratings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingChip("Clarity", avgClarity)
                RatingChip("Feasibility", avgFeasibility)
                RatingChip("Availability", avgAvailability)
            }
        }
    }
}

@Composable
fun StarRating(rating: Double, starSize: Int = 18) {
    val gold = Color(0xFFFFD700)
    val fullStars = rating.toInt()
    val hasHalfStar = (rating - fullStars) >= 0.5
    val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0

    Row(verticalAlignment = Alignment.CenterVertically) {

        // ⭐ stelle piene (oro)
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = gold,
                modifier = Modifier.size(starSize.dp)
            )
        }

        // ⯨ mezza stella (oro)
        if (hasHalfStar) {
            Icon(
                imageVector = Icons.Filled.StarHalf,
                contentDescription = null,
                tint = gold,
                modifier = Modifier.size(starSize.dp)
            )
        }

        // ☆ stelle vuote (BORDO NERO)
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Filled.StarBorder, // ⭐ stessa icona
                contentDescription = null,
                tint = Color.Black,                    // ⬅⬅⬅ BORDO NERO
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}


@Composable
fun RatingChip(label: String, rating: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Label NON in grassetto
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )

        // Solo il valore numerico in grassetto
        Text(
            text = if (rating > 0) String.format("%.1f", rating) else "N/A",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = when {
                rating >= 4.0 -> MaterialTheme.colorScheme.primary
                rating >= 2.5 -> MaterialTheme.colorScheme.tertiary
                rating > 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // ⭐⭐⭐ Stelline stile B (con mezza stella)
        if (rating > 0) {
            StarRating(rating = rating, starSize = 16)
        }
    }
}
