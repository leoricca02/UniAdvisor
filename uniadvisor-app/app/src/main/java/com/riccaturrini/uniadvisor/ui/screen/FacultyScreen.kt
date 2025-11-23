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
    var courseSortOrder by remember { mutableStateOf("name_asc") }
    var showCourseSortMenu by remember { mutableStateOf(false) }

    // Stato per la ricerca
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Shake to refresh
    val shakeDetector = rememberShakeDetector {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(facultyName ?: "Select Faculty")
                },
                // ✅ COLORI RIPRISTINATI
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Map button
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

                    // Sort Menu
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
                                if (courseSortOrder == "name_asc") Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📚 Name Z-A") },
                            onClick = {
                                courseSortOrder = "name_desc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "name_desc") Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⭐ Highest rated") },
                            onClick = {
                                courseSortOrder = "rating_desc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "rating_desc") Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⭐ Lowest rated") },
                            onClick = {
                                courseSortOrder = "rating_asc"
                                showCourseSortMenu = false
                            },
                            leadingIcon = {
                                if (courseSortOrder == "rating_asc") Icon(Icons.Default.Check, null)
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

                // Logica di Filtro e Ordinamento
                val processedCourses = remember(courses, courseSortOrder, searchQuery) {
                    val filtered = if (searchQuery.isBlank()) courses else {
                        courses.filter {
                            it.course.name.contains(searchQuery, ignoreCase = true) ||
                                    it.teacherName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    when (courseSortOrder) {
                        "name_asc" -> filtered.sortedBy { it.course.name }
                        "name_desc" -> filtered.sortedByDescending { it.course.name }
                        "rating_desc" -> filtered.sortedByDescending {
                            (it.avgClarity + it.avgFeasibility + it.avgAvailability) / 3.0
                        }
                        "rating_asc" -> filtered.sortedBy {
                            (it.avgClarity + it.avgFeasibility + it.avgAvailability) / 3.0
                        }
                        else -> filtered
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Barra di ricerca
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search courses or professors...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    if (processedCourses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = if(courses.isEmpty()) Icons.Default.MenuBook else Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (courses.isEmpty()) "No Courses Available" else "No courses found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(processedCourses) { courseWithRatings ->
                                CourseCard(
                                    courseName = courseWithRatings.course.name,
                                    teacherName = courseWithRatings.teacherName,
                                    avgClarity = courseWithRatings.avgClarity,
                                    avgFeasibility = courseWithRatings.avgFeasibility,
                                    avgAvailability = courseWithRatings.avgAvailability,
                                    onClick = {
                                        onCourseClick(courseWithRatings.course.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}