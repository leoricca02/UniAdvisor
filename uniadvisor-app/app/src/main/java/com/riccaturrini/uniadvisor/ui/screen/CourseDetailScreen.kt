package com.riccaturrini.uniadvisor.ui.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.*
import com.riccaturrini.uniadvisor.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    onNavigateBack: () -> Unit,
    viewModel: CourseDetailViewModel = viewModel()
) {
    val courseState by viewModel.courseDetailState.collectAsState()
    val addReviewState by viewModel.addReviewState.collectAsState()
    val noteRatingState by viewModel.noteRatingState.collectAsState()

    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showRateNoteDialog by remember { mutableStateOf(false) }
    var selectedNoteId by remember { mutableStateOf<Int?>(null) }

    // Load course details on start
    LaunchedEffect(courseId) {
        viewModel.loadCourseDetail(courseId)
    }

    // Handle review added successfully
    LaunchedEffect(addReviewState) {
        if (addReviewState is AddReviewState.Success) {
            showAddReviewDialog = false
            viewModel.resetAddReviewState()
        }
    }

    LaunchedEffect(noteRatingState) {
        Log.d("CourseDetailScreen", "🔔 noteRatingState changed: ${noteRatingState::class.simpleName}")

        if (noteRatingState is NoteRatingState.Success) {
            Log.d("CourseDetailScreen", "✅ Note rating success - will reload after delay")
            showRateNoteDialog = false
            selectedNoteId = null

            // ✅ Aspetta un attimo per permettere al backend di aggiornare
            kotlinx.coroutines.delay(1000) // 1 secondo di delay

            // ✅ Ricarica i dati del corso
            Log.d("CourseDetailScreen", "🔄 Calling loadCourseDetail for course $courseId")
            viewModel.loadCourseDetail(courseId)

            // ✅ Resetta lo stato
            Log.d("CourseDetailScreen", "🔄 Resetting note rating state")
            viewModel.resetNoteRatingState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (courseState is CourseDetailState.Success) {
                FloatingActionButton(
                    onClick = { showAddReviewDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Review")
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (courseState) {
                is CourseDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CourseDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = (courseState as CourseDetailState.Error).message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadCourseDetail(courseId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                is CourseDetailState.Success -> {
                    val data = (courseState as CourseDetailState.Success).data

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Course Info Card
                        item {
                            CourseInfoCard(
                                courseName = data.course.name,
                                teacherName = data.teacherName
                            )
                        }

                        // Ratings Card
                        item {
                            CourseRatingsCard(
                                avgClarity = data.avgClarity,
                                avgFeasibility = data.avgFeasibility,
                                avgAvailability = data.avgAvailability
                            )
                        }

                        // Notes Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Course Notes (${data.notes.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                // ✅ Legend for rating
                                if (data.notes.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Best rated first",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Notes List
                        if (data.notes.isEmpty()) {
                            item {
                                EmptyCourseNotesCard()
                            }
                        } else {
                            items(data.notes) { note ->
                                CourseNoteCardWithRating(
                                    note = note,
                                    onRateClick = {
                                        selectedNoteId = note.id
                                        showRateNoteDialog = true
                                    }
                                )
                            }
                        }

                        // Reviews Section Header
                        item {
                            Text(
                                text = "Reviews (${data.reviews.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Reviews List
                        if (data.reviews.isEmpty()) {
                            item {
                                EmptyCourseReviewsCard()
                            }
                        } else {
                            items(data.reviews) { review ->
                                CourseReviewCard(review = review)
                            }
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Add Review Dialog
            if (showAddReviewDialog) {
                AddCourseReviewDialog(
                    onDismiss = {
                        showAddReviewDialog = false
                        viewModel.resetAddReviewState()
                    },
                    onConfirm = { review ->
                        viewModel.addReview(courseId, review)
                    },
                    addReviewState = addReviewState
                )
            }

            // ✅ NEW: Rate Note Dialog
            if (showRateNoteDialog && selectedNoteId != null) {
                RateNoteDialog(
                    onDismiss = {
                        showRateNoteDialog = false
                        selectedNoteId = null
                        viewModel.resetNoteRatingState()
                    },
                    onConfirm = { rating, comment ->
                        viewModel.addNoteRating(courseId, selectedNoteId!!, rating, comment)
                    },
                    noteRatingState = noteRatingState
                )
            }
        }
    }
}

// ✅ NEW: Rate Note Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (rating: Int, comment: String?) -> Unit,
    noteRatingState: NoteRatingState
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Rate Note")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How useful was this note?",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Star rating selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { rating = index + 1 },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "${index + 1} stars",
                                tint = if (index < rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Rating labels
                if (rating > 0) {
                    Text(
                        text = when (rating) {
                            1 -> "⭐ Not useful"
                            2 -> "⭐⭐ Somewhat useful"
                            3 -> "⭐⭐⭐ Useful"
                            4 -> "⭐⭐⭐⭐ Very useful"
                            5 -> "⭐⭐⭐⭐⭐ Extremely useful!"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Optional comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("Share your thoughts about this note...") }
                )

                // Loading state
                if (noteRatingState is NoteRatingState.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Error state
                if (noteRatingState is NoteRatingState.Error) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = noteRatingState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rating > 0) {
                        onConfirm(rating, comment.ifBlank { null })
                    }
                },
                enabled = rating > 0 && noteRatingState !is NoteRatingState.Loading
            ) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ✅ UPDATED: Course Note Card with Rating Display
@Composable
fun CourseNoteCardWithRating(
    note: NoteWithRating,
    onRateClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Note Header with Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Student Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ✅ UPDATED: Rating badge (shows average or "Rate" button)
                if (note.average_rating != null && note.average_rating > 0) {
                    // Show average rating
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        onClick = onRateClick
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = String.format("%.1f", note.average_rating),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF000000).copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    // Show "Rate" button
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onRateClick
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Note Description
            if (!note.description.isNullOrBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View PDF Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(note.file_id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View PDF")
                }

                // Download Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(note.file_id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
            }
        }
    }
}

@Composable
fun EmptyCourseNotesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first to share notes for this course!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CourseInfoCard(
    courseName: String,
    teacherName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Professor: $teacherName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CourseRatingsCard(
    avgClarity: Double,
    avgFeasibility: Double,
    avgAvailability: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Course Ratings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingRow(label = "Clarity", rating = avgClarity)
                RatingRow(label = "Feasibility", rating = avgFeasibility)
                RatingRow(label = "Availability", rating = avgAvailability)
            }
        }
    }
}

@Composable
fun RatingRow(label: String, rating: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < rating.toInt()) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (index < rating.toInt()) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyCourseReviewsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No reviews yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first to review this course!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CourseReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ratings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatingBadge(label = "Clarity", rating = review.rating_clarity)
                RatingBadge(label = "Feasibility", rating = review.rating_feasibility)
                RatingBadge(label = "Availability", rating = review.rating_availability)
            }

            if (!review.comment.isNullOrBlank()) {
                HorizontalDivider()
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RatingBadge(label: String, rating: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when {
            rating >= 4 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            rating >= 3 -> Color(0xFFFFC107).copy(alpha = 0.1f)
            else -> Color(0xFFF44336).copy(alpha = 0.1f)
        }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$rating",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: (ReviewCreate) -> Unit,
    addReviewState: AddReviewState
) {
    var ratingClarity by remember { mutableIntStateOf(0) }
    var ratingFeasibility by remember { mutableIntStateOf(0) }
    var ratingAvailability by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Review") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rating selectors
                Text("Clarity:", fontWeight = FontWeight.Bold)
                StarRatingSelector(rating = ratingClarity, onRatingChange = { ratingClarity = it })

                Text("Feasibility:", fontWeight = FontWeight.Bold)
                StarRatingSelector(rating = ratingFeasibility, onRatingChange = { ratingFeasibility = it })

                Text("Availability:", fontWeight = FontWeight.Bold)
                StarRatingSelector(rating = ratingAvailability, onRatingChange = { ratingAvailability = it })

                // Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Loading state
                if (addReviewState is AddReviewState.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Error state
                if (addReviewState is AddReviewState.Error) {
                    Text(
                        text = addReviewState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0) {
                        onConfirm(
                            ReviewCreate(
                                rating_clarity = ratingClarity,
                                rating_feasibility = ratingFeasibility,
                                rating_availability = ratingAvailability,
                                comment = comment.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0 && addReviewState !is AddReviewState.Loading
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StarRatingSelector(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            IconButton(onClick = { onRatingChange(index + 1) }) {
                Icon(
                    imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (index < rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}