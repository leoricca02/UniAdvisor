package com.riccaturrini.uniadvisor.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyScreen(
    authViewModel: AuthViewModel,
    courseViewModel: CourseViewModel = viewModel()
) {
    val navController = rememberNavController()
    val courseListState by courseViewModel.courseListState.collectAsState()
    val currentUserData by authViewModel.currentUserData.collectAsState()

    var facultyId by remember { mutableStateOf<Int?>(null) }
    var facultyName by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    // Load courses when faculty_id is available
    LaunchedEffect(currentUserData) {
        Log.d("FacultyScreen", "LaunchedEffect triggered - currentUserData: $currentUserData")

        currentUserData?.let { userData ->
            Log.d("FacultyScreen", "User data found - faculty_id: ${userData.faculty_id}")

            userData.faculty_id?.let { fId ->
                if (!hasLoaded || facultyId != fId) {
                    Log.d("FacultyScreen", "Loading courses for faculty: $fId")
                    facultyId = fId
                    facultyName = "Your Faculty"
                    courseViewModel.loadCoursesByFaculty(fId)
                    hasLoaded = true
                }
            } ?: run {
                Log.e("FacultyScreen", "User has no faculty_id!")
            }
        } ?: run {
            Log.e("FacultyScreen", "currentUserData is null!")
        }
    }

    NavHost(navController = navController, startDestination = "faculty_main") {
        composable("faculty_main") {
            FacultyMainScreen(
                courseListState = courseListState,
                facultyId = facultyId,
                facultyName = facultyName,
                onCourseClick = { courseId ->
                    navController.navigate("course_detail/$courseId")
                },
                onRefresh = {
                    facultyId?.let { fId ->
                        Log.d("FacultyScreen", "Refreshing courses for faculty: $fId")
                        courseViewModel.loadCoursesByFaculty(fId)
                    }
                }
            )
        }

        // ✅ FIXED: Updated to use new CourseDetailScreen signature
        composable("course_detail/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CourseDetailScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() }
                    // Note: Add review is now handled inside CourseDetailScreen via dialog
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyMainScreen(
    courseListState: CourseListState,
    facultyId: Int?,
    facultyName: String?,
    onCourseClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Academic")
                        facultyName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Show message if no faculty is selected
        if (facultyId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No faculty selected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Please complete your profile first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        // Show courses list based on state
        when (courseListState) {
            is CourseListState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading courses...")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = courseListState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
            }

            is CourseListState.Success -> {
                val courses = courseListState.courses

                if (courses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No courses available",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Check back later for course listings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(courses) { courseWithRatings ->
                            CourseCard(
                                courseWithRatings = courseWithRatings,
                                onClick = { onCourseClick(courseWithRatings.course.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(
    courseWithRatings: CourseWithRatings,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Course name
            Text(
                text = courseWithRatings.course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Teacher
            Text(
                text = "Prof. ${courseWithRatings.teacherName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Ratings summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingSummary(
                    label = "Clarity",
                    rating = courseWithRatings.avgClarity
                )
                RatingSummary(
                    label = "Feasibility",
                    rating = courseWithRatings.avgFeasibility
                )
                RatingSummary(
                    label = "Availability",
                    rating = courseWithRatings.avgAvailability
                )
            }
        }
    }
}

@Composable
fun RatingSummary(label: String, rating: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (rating > 0) String.format("%.1f", rating) else "N/A",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}