package com.riccaturrini.uniadvisor.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.riccaturrini.uniadvisor.data.Lesson
import com.riccaturrini.uniadvisor.network.ApiClient
import com.riccaturrini.uniadvisor.network.CheckInRequest
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileUiState
import com.riccaturrini.uniadvisor.viewmodel.ProfileViewModel
import com.riccaturrini.uniadvisor.viewmodel.ScheduleUiState
import com.riccaturrini.uniadvisor.viewmodel.ScheduleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUserData.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()
    val scheduleState by scheduleViewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load User Profile on startup
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    // Load Schedule once profile is loaded
    LaunchedEffect(profileState) {
        if (profileState is ProfileUiState.Success) {
            val user = (profileState as ProfileUiState.Success).user
            if (user.faculty_id != null) {
                scheduleViewModel.loadSchedule(user.faculty_id)
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UniAdvisor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (profileState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileUiState.Error -> {
                    val errorMsg = (profileState as ProfileUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading profile: $errorMsg", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { profileViewModel.loadProfile() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    val successState = profileState as ProfileUiState.Success
                    val user = successState.user
                    val stats = successState.stats

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Welcome Card
                        WelcomeCard(
                            firstName = user.first_name,
                            greeting = getGreeting()
                        )

                        // 2. TODAY'S LESSONS (Auto Check-in)
                        if (scheduleState is ScheduleUiState.Success) {
                            val allLessons = (scheduleState as ScheduleUiState.Success).allLessons
                            TodayScheduleSection(allLessons = allLessons)
                        } else if (scheduleState is ScheduleUiState.Loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. Main Menu
                        Text(
                            text = "Quick Menu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        CalendarCard(onClick = { onNavigate("calendar") })

                        QuickActionsGrid(onNavigate = onNavigate)

                        // 4. Statistics
                        StatisticsCard(
                            notesCount = stats.notesCount,
                            reviewsCount = stats.reviewsCount,
                            ratingsCount = stats.noteRatingsCount
                        )

                        // 5. Faculty Info
                        successState.faculty?.let { faculty ->
                            FacultyInfoCard(facultyName = faculty.name)
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TodayScheduleSection(allLessons: List<Lesson>) {
    val todayDayOfWeek = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val displayDayOfWeek = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) // English

    // Filter today's lessons
    val todayLessons = allLessons
        .filter { it.dayOfWeek.equals(todayDayOfWeek, ignoreCase = true) }
        .sortedBy { it.startTime }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today ($displayDayOfWeek)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (todayLessons.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${todayLessons.size} lessons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (todayLessons.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Weekend,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "No lessons today!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enjoy your free time 🎉",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Horizontal List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                items(todayLessons) { lesson ->
                    TodayLessonCard(lesson)
                }
            }
        }
    }
}

@Composable
fun TodayLessonCard(lesson: Lesson) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lifecycle Owner for detecting app resume
    val lifecycleOwner = LocalLifecycleOwner.current

    // Time Logic
    val now = LocalTime.now()
    val start = try { LocalTime.parse(lesson.startTime) } catch (e: Exception) { LocalTime.MIN }
    val end = try { LocalTime.parse(lesson.endTime) } catch (e: Exception) { LocalTime.MAX }

    val isActive = now.isAfter(start) && now.isBefore(end)

    // -- STATES --
    var currentOccupancy by remember { mutableIntStateOf(lesson.checkins) }
    var isCheckingIn by remember { mutableStateOf(false) }
    var isCheckedIn by remember { mutableStateOf(false) }
    var checkInFailed by remember { mutableStateOf(false) }
    var locationPermissionDenied by remember { mutableStateOf(false) }

    // Trigger to re-run check logic (incremented on resume)
    var checkTrigger by remember { mutableIntStateOf(0) }

    // -- LIFECYCLE OBSERVER --
    // This makes the app re-check when you open it back up
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // If we are active but failed or haven't checked in, retry
                if (isActive && !isCheckedIn) {
                    checkTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Helper to check permission
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locationPermissionDenied = false
            // Retry immediately
            checkTrigger++
        } else {
            locationPermissionDenied = true
        }
    }

    // -- AUTOMATIC CHECK-IN LOGIC --
    // Runs when:
    // 1. 'isActive' becomes true (Lesson starts)
    // 2. 'checkTrigger' increases (App Resumed or User clicked Retry)
    LaunchedEffect(isActive, checkTrigger) {
        if (isActive && !isCheckedIn && !isCheckingIn) {
            if (hasLocationPermission()) {
                isCheckingIn = true
                checkInFailed = false // Reset error state

                performCheckIn(context, lesson.id, scope) { success ->
                    isCheckingIn = false
                    if (success) {
                        isCheckedIn = true
                        currentOccupancy += 1
                    } else {
                        checkInFailed = true // Too far?
                    }
                }
            } else {
                locationPermissionDenied = true
            }
        }
    }

    // Styling
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Time & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lesson.startTime.take(5)} - ${lesson.endTime.take(5)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )

                if (isActive) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("LIVE", color = Color.White)
                    }
                }
            }

            // Course Name
            Text(
                text = lesson.course.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            // Occupancy Counter (Real Data)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$currentOccupancy present",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer: Status & Navigate
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // STATUS AREA
                Box(
                    modifier = Modifier.height(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isCheckedIn) {
                        // Success State
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Present", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else if (isCheckingIn) {
                        // Loading State
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (checkInFailed && isActive) {
                        // Failed State (Click to Retry!)
                        Row(
                            modifier = Modifier.clickable { checkTrigger++ }, // Manual retry
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Not in class (Retry)", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                        }
                    } else if (locationPermissionDenied && isActive) {
                        // Permission missing
                        Button(
                            onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Enable GPS", fontSize = 11.sp)
                        }
                    } else {
                        // Inactive
                        Text(if (now.isBefore(start)) "Soon" else "Finished", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // NAVIGATE BUTTON
                if (lesson.course.latitude != null && lesson.course.longitude != null) {
                    FilledTonalIconButton(
                        onClick = {
                            openMap(context, lesson.course.latitude, lesson.course.longitude, lesson.course.name)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Logic to get location and call API (Silent check-in)
fun performCheckIn(context: Context, lessonId: Int, scope: CoroutineScope, onResult: (Boolean) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val request = CheckInRequest(location.latitude, location.longitude)
                        val response = ApiClient.instance.checkInLesson(lessonId, request)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                onResult(true)
                            } else {
                                onResult(false)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onResult(false)
                        }
                    }
                }
            } else {
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false)
        }
    } catch (e: SecurityException) {
        onResult(false)
    }
}

// Helper function to open Google Maps
fun openMap(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val genericIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(genericIntent)
    }
}

@Composable
fun CalendarCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Full Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View full week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun WelcomeCard(firstName: String, greeting: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                Text(
                    text = firstName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun StatisticsCard(notesCount: Int, reviewsCount: Int, ratingsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Description,
                    label = "Notes",
                    value = notesCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    icon = Icons.Default.RateReview,
                    label = "Reviews",
                    value = reviewsCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    icon = Icons.Default.Star,
                    label = "Ratings",
                    value = ratingsCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FacultyInfoCard(facultyName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(12.dp)
                )
            }
            Column {
                Text(
                    text = "Degree Program",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = facultyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.School,
                title = "Courses",
                description = "Browse courses",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("faculty") }
            )
            QuickActionCard(
                icon = Icons.Default.Description,
                title = "Notes",
                description = "Your notes",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("notes") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.RateReview,
                title = "Reviews",
                description = "Your reviews",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("reviews") }
            )
            QuickActionCard(
                icon = Icons.Default.Person,
                title = "Profile",
                description = "View profile",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("profile") }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logout") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper for dynamic greeting based on time of day
fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        in 18..21 -> "Good evening,"
        else -> "Good night,"
    }
}