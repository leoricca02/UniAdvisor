package com.riccaturrini.uniadvisor.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.Lesson
import com.riccaturrini.uniadvisor.viewmodel.ScheduleUiState
import com.riccaturrini.uniadvisor.viewmodel.ScheduleViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

// --- GRID CONFIGURATION ---
private val HOUR_HEIGHT = 90.dp
private val TIME_COLUMN_WIDTH = 60.dp
private val START_HOUR = 8
private val END_HOUR = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(),
    userFacultyId: Int?
) {
    // Viewmodel states
    val uiState by viewModel.uiState.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedCourses by viewModel.selectedCourses.collectAsState()

    // Local state for filter dialog
    var showFilterDialog by remember { mutableStateOf(false) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // Load data
    LaunchedEffect(userFacultyId) {
        if (userFacultyId != null) {
            viewModel.loadSchedule(userFacultyId)
        }
    }

    // Filter Dialog
    if (showFilterDialog && uiState is ScheduleUiState.Success) {
        val state = uiState as ScheduleUiState.Success
        FilterDialog(
            availableCourses = state.availableCourses,
            selectedCourses = selectedCourses,
            onCourseToggle = { name, isSelected -> viewModel.onCourseFilterChanged(name, isSelected) },
            onClear = { viewModel.clearCourseFilter() },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        val icon = if (selectedCourses.isNotEmpty()) Icons.Default.FilterListOff else Icons.Default.FilterList
                        val tint = if (selectedCourses.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        Icon(icon, contentDescription = "Filter", tint = tint)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. DAY SELECTOR (Header)
            DaySelector(
                days = days,
                selectedDay = selectedDay,
                onDaySelected = { viewModel.onDaySelected(it) }
            )

            Divider()

            // 2. GRID CONTENT
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is ScheduleUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ScheduleUiState.Error -> {
                        val errorMsg = (uiState as ScheduleUiState.Error).message
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Error: $errorMsg",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { if (userFacultyId != null) viewModel.loadSchedule(userFacultyId) }) {
                                Text("Retry")
                            }
                        }
                    }
                    is ScheduleUiState.Success -> {
                        val lessons = (uiState as ScheduleUiState.Success).filteredLessons

                        if (lessons.isEmpty()) {
                            EmptyScheduleView(selectedDay, modifier = Modifier.align(Alignment.Center))
                        } else {
                            // Render grid
                            DailyTimetableGrid(lessons)
                        }
                    }
                }
            }
        }
    }
}

// --- UI COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelector(
    days: List<String>,
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(days) { day ->
            // Map days for short display (EN/IT mixed for UI)
            val shortLabel = when(day) {
                "Monday" -> "Mon"
                "Tuesday" -> "Tue"
                "Wednesday" -> "Wed"
                "Thursday" -> "Thu"
                "Friday" -> "Fri"
                else -> day.take(3)
            }

            FilterChip(
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
                label = { Text(shortLabel) },
                leadingIcon = if (day == selectedDay) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun DailyTimetableGrid(lessons: List<Lesson>) {
    val scrollState = rememberScrollState()

    // Total grid height
    val totalGridHeight = HOUR_HEIGHT * (END_HOUR - START_HOUR + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalGridHeight)
        ) {
            // A. BACKGROUND (Time Lines)
            for (hour in START_HOUR..END_HOUR) {
                val topOffset = (hour - START_HOUR) * HOUR_HEIGHT

                // Hour Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(HOUR_HEIGHT)
                ) {
                    // Time Label (e.g., 09:00)
                    Text(
                        text = String.format("%02d:00", hour),
                        modifier = Modifier
                            .width(TIME_COLUMN_WIDTH)
                            .padding(end = 8.dp, top = 4.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Divider Line
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }

            // B. LESSON BLOCKS
            lessons.forEach { lesson ->
                SingleDayLessonBlock(lesson)
            }

            // C. CURRENT TIME LINE
            CurrentTimeLine()
        }
        // Extra space at bottom
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SingleDayLessonBlock(lesson: Lesson) {
    val context = LocalContext.current

    // 1. Parsing Time
    val startTime = try { LocalTime.parse(lesson.startTime) } catch (e: Exception) { LocalTime.of(0,0) }
    val endTime = try { LocalTime.parse(lesson.endTime) } catch (e: Exception) { LocalTime.of(0,0) }

    // 2. Geometry Calculation
    val minutesFromStart = ChronoUnit.MINUTES.between(LocalTime.of(START_HOUR, 0), startTime)
    val durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime)

    val topOffset = (minutesFromStart / 60.0).toFloat() * HOUR_HEIGHT
    val blockHeight = (durationMinutes / 60.0).toFloat() * HOUR_HEIGHT

    val finalHeight = if (blockHeight < 40.dp) 40.dp else blockHeight

    // 3. Color
    val baseColor = generateColorForCourse(lesson.course.name)

    // Render Card
    Card(
        modifier = Modifier
            .padding(start = TIME_COLUMN_WIDTH + 4.dp, end = 8.dp)
            .offset(y = topOffset)
            .height(finalHeight)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseColor.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, baseColor.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Row for Title and Map Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Header: Course Name
                Text(
                    text = lesson.course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // MAP BUTTON (Small)
                if (lesson.course.latitude != null && lesson.course.longitude != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                openMap(context, lesson.course.latitude, lesson.course.longitude, lesson.course.name)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Body: Room & Building
            val room = lesson.course.roomNumber ?: "Room N/A"
            val building = lesson.course.buildingName ?: ""
            Text(
                text = if(building.isNotEmpty()) "$room • $building" else room,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer: Time and Teacher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Formatted Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = baseColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatTime(startTime)} - ${formatTime(endTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Teacher Name
                if (!lesson.course.teacherName.isNullOrEmpty()) {
                    Text(
                        text = lesson.course.teacherName,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentTimeLine() {
    val now = LocalTime.now()
    if (now.hour >= START_HOUR && now.hour <= END_HOUR) {
        val minutesFromStart = ChronoUnit.MINUTES.between(LocalTime.of(START_HOUR, 0), now)
        val topOffset = (minutesFromStart / 60.0).toFloat() * HOUR_HEIGHT

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = topOffset)
        ) {
            Divider(
                color = MaterialTheme.colorScheme.error,
                thickness = 2.dp,
                modifier = Modifier.padding(start = TIME_COLUMN_WIDTH)
            )
            Box(
                modifier = Modifier
                    .offset(x = TIME_COLUMN_WIDTH - 5.dp, y = (-4).dp)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )
        }
    }
}

@Composable
fun EmptyScheduleView(day: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No lessons found for",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = translateDay(day),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun FilterDialog(
    availableCourses: List<String>,
    selectedCourses: Set<String>,
    onCourseToggle: (String, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Courses") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClear) {
                        Text("Show All")
                    }
                }
                Divider()
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(availableCourses) { courseName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCourseToggle(courseName, !selectedCourses.contains(courseName))
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = selectedCourses.contains(courseName),
                                onCheckedChange = { isChecked ->
                                    onCourseToggle(courseName, isChecked)
                                }
                            )
                            Text(
                                text = courseName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// --- HELPER FUNCTIONS ---



private fun formatTime(time: LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun translateDay(day: String): String {
    return when(day) {
        "Monday" -> "Mon"
        "Tuesday" -> "Tue"
        "Wednesday" -> "Wes"
        "Thursday" -> "Thu"
        "Friday" -> "Fri"
        "Saturday" -> "Sat"
        "Sunday" -> "Sun"
        else -> day
    }
}

private fun generateColorForCourse(courseName: String): Color {
    val hash = courseName.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    val saturation = 0.60f
    val lightness = 0.50f

    return Color.hsl(hue, saturation, lightness)
}