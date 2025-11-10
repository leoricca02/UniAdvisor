package com.riccaturrini.uniadvisor.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.CourseListState
import com.riccaturrini.uniadvisor.viewmodel.CourseViewModel

@Composable
fun FacultyScreen(
    authViewModel: AuthViewModel
) {
    val courseViewModel: CourseViewModel = viewModel()

    Log.d("FacultyScreen", "🚀 FacultyScreen COMPOSABLE STARTED")

    val navController = rememberNavController()
    val courseListState by courseViewModel.courseListState.collectAsState()
    val currentUserData by authViewModel.currentUserData.collectAsState()

    // ✅ Log per vedere quando lo stato cambia
    LaunchedEffect(courseListState) {
        Log.d("FacultyScreen", "🔄 courseListState CHANGED to: ${courseListState::class.simpleName}")
        when (courseListState) {
            is CourseListState.Success -> {
                Log.d("FacultyScreen", "✅ SUCCESS STATE with ${(courseListState as CourseListState.Success).courses.size} courses")
            }
            is CourseListState.Error -> {
                Log.d("FacultyScreen", "❌ ERROR STATE: ${(courseListState as CourseListState.Error).message}")
            }
            is CourseListState.Loading -> {
                Log.d("FacultyScreen", "⏳ LOADING STATE")
            }
        }
    }

    LaunchedEffect(currentUserData) {
        Log.d("FacultyScreen", "🔄 Screen opened - Reloading user profile")
        Log.d("FacultyScreen", "LaunchedEffect triggered - currentUserData: $currentUserData")

        currentUserData?.let { userData ->
            Log.d("FacultyScreen", "User data found - faculty_id: ${userData.faculty_id}")
            userData.faculty_id?.let { facultyId ->
                Log.d("FacultyScreen", "✅ Loading courses for faculty: $facultyId")
                courseViewModel.loadCoursesByFaculty(facultyId)
            } ?: run {
                Log.d("FacultyScreen", "⚠️ User has no faculty_id")
            }
        } ?: run {
            Log.d("FacultyScreen", "⚠️ currentUserData is null")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "faculty_main"
    ) {
        composable("faculty_main") {
            FacultyMainScreen(
                courseListState = courseListState,
                facultyId = currentUserData?.faculty_id,
                facultyName = when (currentUserData?.faculty_id) {
                    1 -> "Engineering"
                    2 -> "Medicine"
                    3 -> "Law"
                    else -> null
                },
                onCourseClick = { courseId ->
                    navController.navigate("course_detail/$courseId")
                },
                onRefresh = {
                    currentUserData?.faculty_id?.let { facultyId ->
                        courseViewModel.loadCoursesByFaculty(facultyId)
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
    Log.d("FacultyMainScreen", "🎨 Composing FacultyMainScreen")
    Log.d("FacultyMainScreen", "📊 State: ${courseListState::class.simpleName}")
    Log.d("FacultyMainScreen", "🏫 Faculty ID: $facultyId")

    // ✅ NUOVO: State per ordinamento corsi
    var courseSortOrder by remember { mutableStateOf("name_asc") } // name_asc, name_desc, rating_desc, rating_asc
    var showCourseSortMenu by remember { mutableStateOf(false) }

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
                    // ✅ NUOVO: Bottone ordinamento
                    IconButton(onClick = { showCourseSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort courses")
                    }

                    // Menu ordinamento
                    DropdownMenu(
                        expanded = showCourseSortMenu,
                        onDismissRequest = { showCourseSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📚 Name A-Z") },
                            onClick = {
                                courseSortOrder = "name_asc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "name_asc") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📚 Name Z-A") },
                            onClick = {
                                courseSortOrder = "name_desc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "name_desc") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⭐ Highest rated") },
                            onClick = {
                                courseSortOrder = "rating_desc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "rating_desc") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⭐ Lowest rated") },
                            onClick = {
                                courseSortOrder = "rating_asc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "rating_asc") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            facultyId == null -> {
                Log.d("FacultyMainScreen", "⚠️ No faculty ID - showing selection prompt")
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Faculty Selected",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Please select a faculty in your profile",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            courseListState is CourseListState.Loading -> {
                Log.d("FacultyMainScreen", "⏳ Showing loading state")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            courseListState is CourseListState.Error -> {
                Log.d("FacultyMainScreen", "❌ Showing error state")
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
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error Loading Courses",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = (courseListState as CourseListState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            courseListState is CourseListState.Success -> {
                val courses = (courseListState as CourseListState.Success).courses
                Log.d("FacultyMainScreen", "✅ Success state with ${courses.size} courses")

                if (courses.isEmpty()) {
                    Log.d("FacultyMainScreen", "📭 No courses available")
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
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Courses Available",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Check back later for new courses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Log.d("FacultyMainScreen", "📚 About to render LazyColumn with ${courses.size} courses")

                    // ✅ ORDINA i corsi in base a courseSortOrder
                    val sortedCourses = remember(courses, courseSortOrder) {
                        when (courseSortOrder) {
                            "name_asc" -> courses.sortedBy { it.course.name }
                            "name_desc" -> courses.sortedByDescending { it.course.name }
                            "rating_desc" -> courses.sortedByDescending {
                                (it.avgClarity + it.avgFeasibility + it.avgAvailability) / 3.0
                            }
                            "rating_asc" -> courses.sortedBy {
                                (it.avgClarity + it.avgFeasibility + it.avgAvailability) / 3.0
                            }
                            else -> courses
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedCourses) { courseWithRatings ->
                            Log.d("FacultyMainScreen", "🎓 Rendering course: ${courseWithRatings.course.name}")

                            // ✅ USA la funzione CourseCard da CoursesListScreen.kt
                            CourseCard(
                                courseName = courseWithRatings.course.name,
                                teacherName = courseWithRatings.teacherName,
                                avgClarity = courseWithRatings.avgClarity,
                                avgFeasibility = courseWithRatings.avgFeasibility,
                                avgAvailability = courseWithRatings.avgAvailability,
                                onClick = {
                                    Log.d("FacultyMainScreen", "👆 Course clicked: ${courseWithRatings.course.id}")
                                    onCourseClick(courseWithRatings.course.id)
                                }
                            )
                        }
                    }

                    Log.d("FacultyMainScreen", "✅ LazyColumn composition completed")
                }
            }
        }
    }
}