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

    // ✅ FIXED: Force reload user data when screen is first displayed
    LaunchedEffect(Unit) {
        Log.d("FacultyScreen", "🔄 Screen opened - Reloading user profile")
        authViewModel.checkUserProfile()
    }

    // Load courses when faculty_id is available
    LaunchedEffect(currentUserData) {
        Log.d("FacultyScreen", "LaunchedEffect triggered - currentUserData: $currentUserData")

        currentUserData?.let { userData ->
            Log.d("FacultyScreen", "User data found - faculty_id: ${userData.faculty_id}")

            userData.faculty_id?.let { fId ->
                if (!hasLoaded || facultyId != fId) {
                    Log.d("FacultyScreen", "✅ Loading courses for faculty: $fId")
                    facultyId = fId
                    facultyName = "Your Faculty" // Could fetch actual name from API if needed
                    courseViewModel.loadCoursesByFaculty(fId)
                    hasLoaded = true
                }
            } ?: run {
                Log.e("FacultyScreen", "❌ User has no faculty_id!")
            }
        } ?: run {
            Log.e("FacultyScreen", "❌ currentUserData is null!")
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
                        Log.d("FacultyScreen", "🔄 Refreshing courses for faculty: $fId")
                        courseViewModel.loadCoursesByFaculty(fId)
                    } ?: run {
                        // ✅ FIXED: If no faculty_id, try reloading user profile
                        Log.d("FacultyScreen", "🔄 No faculty_id, reloading user profile")
                        authViewModel.checkUserProfile()
                    }
                }
            )
        }

        composable("course_detail/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CourseDetailScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
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
                        text = "Please select a faculty in your profile first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    // ✅ ADDED: Refresh button to try reloading
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reload")
                    }
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
                        Text(
                            text = "Loading courses...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error loading courses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = courseListState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}