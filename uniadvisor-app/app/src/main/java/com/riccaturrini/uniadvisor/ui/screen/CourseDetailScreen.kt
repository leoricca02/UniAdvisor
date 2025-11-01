package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.Review
import com.riccaturrini.uniadvisor.data.ReviewCreate
import com.riccaturrini.uniadvisor.viewmodel.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Int,
    onNavigateBack: () -> Unit,
    viewModel: CourseDetailViewModel = viewModel()
) {
    val courseState by viewModel.courseDetailState.collectAsState()
    val addReviewState by viewModel.addReviewState.collectAsState()

    var showAddReviewDialog by remember { mutableStateOf(false) }

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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading course details...")
                        }
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

                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing = false),
                        onRefresh = { viewModel.loadCourseDetail(courseId) }
                    ) {
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

                            // Reviews Section Header
                            item {
                                Text(
                                    text = "Reviews (${data.reviews.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Average Ratings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            CourseRatingRow(label = "Lesson Clarity", rating = avgClarity)
            CourseRatingRow(label = "Exam Feasibility", rating = avgFeasibility)
            CourseRatingRow(label = "Teacher Availability", rating = avgAvailability)
        }
    }
}

@Composable
fun CourseRatingRow(label: String, rating: Double) {
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
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val starRating = index + 1
                Icon(
                    imageVector = if (rating >= starRating) Icons.Filled.Star
                    else if (rating >= starRating - 0.5) Icons.Default.StarHalf
                    else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (rating >= starRating - 0.5) Color(0xFFFFD700)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (rating > 0) String.format("%.1f", rating) else "N/A",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
            // Header with user ID and date
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
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Student #${review.student_id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = review.created_at,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Ratings
            CourseReviewRatingRow(label = "Clarity", rating = review.rating_clarity)
            CourseReviewRatingRow(label = "Feasibility", rating = review.rating_feasibility)
            CourseReviewRatingRow(label = "Availability", rating = review.rating_availability)

            // Comment
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
fun CourseReviewRatingRow(label: String, rating: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (index < rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCourseReviewsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No reviews yet",
                style = MaterialTheme.typography.bodyLarge,
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
fun AddCourseReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: (ReviewCreate) -> Unit,
    addReviewState: AddReviewState
) {
    var ratingClarity by remember { mutableStateOf(0) }
    var ratingFeasibility by remember { mutableStateOf(0) }
    var ratingAvailability by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.RateReview, contentDescription = null)
        },
        title = { Text("Add Review") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Clarity Rating
                Text(
                    text = "Lesson Clarity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                CourseRatingSelector(
                    rating = ratingClarity,
                    onRatingChange = { ratingClarity = it }
                )

                HorizontalDivider()

                // Feasibility Rating
                Text(
                    text = "Exam Feasibility",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                CourseRatingSelector(
                    rating = ratingFeasibility,
                    onRatingChange = { ratingFeasibility = it }
                )

                HorizontalDivider()

                // Availability Rating
                Text(
                    text = "Teacher Availability",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                CourseRatingSelector(
                    rating = ratingAvailability,
                    onRatingChange = { ratingAvailability = it }
                )

                HorizontalDivider()

                // Comment
                Text(
                    text = "Comment (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Share your experience...") },
                    maxLines = 4
                )

                // Error message
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
                enabled = addReviewState !is AddReviewState.Loading &&
                        ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0
            ) {
                if (addReviewState is AddReviewState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = addReviewState !is AddReviewState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CourseRatingSelector(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stars row
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..5) {
                IconButton(
                    onClick = { onRatingChange(i) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Rating $i",
                        tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Rating text below stars
        Text(
            text = if (rating > 0) "$rating/5" else "Tap to rate",
            style = MaterialTheme.typography.bodySmall,
            color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}