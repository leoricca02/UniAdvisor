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
import com.riccaturrini.uniadvisor.ui.screen.NoteDetailScreen
import com.riccaturrini.uniadvisor.utils.rememberShakeDetector
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.riccaturrini.uniadvisor.utils.openGoogleMaps
import androidx.compose.ui.platform.LocalContext
import com.riccaturrini.uniadvisor.utils.openGoogleMaps
import com.riccaturrini.uniadvisor.network.ApiClient


@Composable
fun FacultyScreen(
    authViewModel: AuthViewModel,
    courseViewModel: CourseViewModel = viewModel()
) {
    val courseViewModel: CourseViewModel = viewModel()

    Log.d("FacultyScreen", "🚀 FacultyScreen COMPOSABLE STARTED")

    val navController = rememberNavController()
    val courseListState by courseViewModel.courseListState.collectAsState()
    val currentUserData by authViewModel.currentUserData.collectAsState()
    var facultyLatitude by remember { mutableStateOf<Double?>(null) }
    var facultyLongitude by remember { mutableStateOf<Double?>(null) }
    var facultyBuildingName by remember { mutableStateOf<String?>(null) }

    // Load faculty location when faculty ID changes
    LaunchedEffect(currentUserData?.faculty_id) {
        currentUserData?.faculty_id?.let { facultyId ->
            // You can get this from the API or from a cached faculty list
            // For now, we'll use the data from currentUserData if available
        }
    }

    // Log to see the state changes
    LaunchedEffect(courseListState) {
        Log.d("FacultyScreen", "🔄 courseListState CHANGED to: ${courseListState::class.simpleName}")
        when (courseListState) {
            is CourseListState.Success -> {
                Log.d(
                    "FacultyScreen",
                    "✅ SUCCESS STATE with ${(courseListState as CourseListState.Success).courses.size} courses"
                )
            }

            is CourseListState.Error -> {
                Log.d(
                    "FacultyScreen",
                    "❌ ERROR STATE: ${(courseListState as CourseListState.Error).message}"
                )
            }

            is CourseListState.Loading -> {
                Log.d("FacultyScreen", "⏳ LOADING STATE")
            }
        }
    }


    LaunchedEffect(currentUserData?.faculty_id) {
        currentUserData?.faculty_id?.let { facultyId ->
            try {
                Log.d("FacultyScreen", "📍 Fetching location for faculty: $facultyId")
                val response = ApiClient.instance.getFacultyLocation(facultyId)
                if (response.isSuccessful) {
                    response.body()?.let { facultyLocation ->
                        facultyLatitude = facultyLocation.latitude
                        facultyLongitude = facultyLocation.longitude
                        facultyBuildingName = facultyLocation.buildingName
                        Log.d("FacultyScreen", "✅ Faculty location loaded: $facultyLatitude, $facultyLongitude")
                    }
                } else {
                    Log.e("FacultyScreen", "❌ Failed to load faculty location: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FacultyScreen", "❌ Error fetching faculty location", e)
            }
        }
    }

    LaunchedEffect(currentUserData) {
        Log.d("FacultyScreen", "🔄 Screen opened - Reloading user profile")
        currentUserData?.let { userData ->
            userData.faculty_id?.let { facultyId ->
                Log.d("FacultyScreen", "✅ Loading courses for faculty: $facultyId")
                courseViewModel.loadCoursesByFaculty(facultyId, forceReload = true)
            }
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
                facultyLatitude = facultyLatitude,
                facultyLongitude = facultyLongitude,
                facultyName = currentUserData?.faculty_name,
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

        composable("camera_ocr/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CameraOcrScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.popBackStack()
                        // Optionally show success message
                    }
                )
            }
        }

        // Added onNavigateToNoteDetail
        composable("course_detail/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CourseDetailScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate("note_detail/$courseId/$noteId")
                    },
                    onNavigateToCourseNotes = { id ->
                        navController.navigate("course_notes/$id")
                    },
                    onNavigateToCourseReviews = { id ->
                        navController.navigate("course_reviews/$id")
                    },
                    onNavigateToCamera = { id ->
                        // Use your navController to go to the camera screen
                        navController.navigate("camera_ocr/$id")
                    }
                )
            }
        }

        composable("course_notes/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CourseNotesScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate("note_detail/$courseId/$noteId")
                    },
                    onNavigateToCamera = { id ->
                        // Use your navController to go to the camera screen
                        navController.navigate("camera_ocr/$id")
                    }
                )
            }
        }

        composable("course_reviews/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            if (courseId != null) {
                CourseReviewsScreen(
                    courseId = courseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Route for NoteDetailScreen
        composable("note_detail/{courseId}/{noteId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull()

            if (courseId != null && noteId != null) {
                NoteDetailScreen(
                    noteId = noteId,
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
    facultyLatitude: Double?,
    facultyLongitude: Double?,
    onCourseClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Log.d("FacultyMainScreen", "🎨 Composing FacultyMainScreen")
    Log.d("FacultyMainScreen", "📊 State: ${courseListState::class.simpleName}")
    Log.d("FacultyMainScreen", "🏫 Faculty ID: $facultyId")

    var courseSortOrder by remember { mutableStateOf("name_asc") } // name_asc, name_desc, rating_desc, rating_asc
    var showCourseSortMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Shake to refresh
    val shakeDetector = rememberShakeDetector {
        onRefresh() // Triggers your existing refresh function
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(facultyName ?: "Select Faculty")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Map button - only show if location is available
                    if (facultyLatitude != null && facultyLongitude != null) {
                        IconButton(
                            onClick = {
                                openGoogleMaps(
                                    context = context,
                                    latitude = facultyLatitude,
                                    longitude = facultyLongitude,
                                    label = facultyName
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open in Maps"
                            )
                        }
                    }

                    // Sort button
                    IconButton(onClick = { showCourseSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort courses")
                    }

                    // Menu for sorting courses
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

                    // sort courses based on courseSortOrder
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