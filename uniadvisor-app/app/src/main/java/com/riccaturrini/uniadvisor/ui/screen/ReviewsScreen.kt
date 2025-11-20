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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    authViewModel: AuthViewModel,
    myReviewsViewModel: MyReviewsViewModel = viewModel()
) {
    val reviewsState by myReviewsViewModel.reviewsState.collectAsState()
    val actionState by myReviewsViewModel.actionState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedReview by remember { mutableStateOf<Review?>(null) }

    // Load reviews on start
    LaunchedEffect(Unit) {
        myReviewsViewModel.loadMyReviews()
    }

    // Handle action success
    LaunchedEffect(actionState) {
        if (actionState is ReviewActionState.Success) {
            showEditDialog = false
            showDeleteDialog = false
            myReviewsViewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reviews") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (reviewsState) {
                is MyReviewsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading reviews...")
                        }
                    }
                }

                is MyReviewsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = (reviewsState as MyReviewsUiState.Error).message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { myReviewsViewModel.loadMyReviews() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                is MyReviewsUiState.Empty -> {
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
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "No reviews yet",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You haven't written any course reviews",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is MyReviewsUiState.Success -> {
                    val reviews = (reviewsState as MyReviewsUiState.Success).reviews

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reviews) { item ->
                            ReviewCard(
                                review = item.review,
                                courseName = item.courseName,
                                onEditClick = {
                                    selectedReview = item.review
                                    showEditDialog = true
                                },
                                onDeleteClick = {
                                    selectedReview = item.review
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Edit review dialog
            if (showEditDialog && selectedReview != null) {
                EditReviewDialog(
                    review = selectedReview!!,
                    onDismiss = {
                        showEditDialog = false
                        myReviewsViewModel.resetActionState()
                    },
                    onConfirm = { updatedReview ->
                        myReviewsViewModel.updateReview(selectedReview!!.id, updatedReview)
                    },
                    actionState = actionState
                )
            }

            // Delete confirmation dialog
            if (showDeleteDialog && selectedReview != null) {
                DeleteReviewDialog(
                    onDismiss = {
                        showDeleteDialog = false
                        myReviewsViewModel.resetActionState()
                    },
                    onConfirm = {
                        myReviewsViewModel.deleteReview(selectedReview!!.id)
                    },
                    actionState = actionState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCard(
    review: Review,
    courseName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            // Header with course ID and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = review.created_at,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ratings
            ReviewRatingRow(label = "Clarity", rating = review.rating_clarity)
            Spacer(modifier = Modifier.height(8.dp))
            ReviewRatingRow(label = "Feasibility", rating = review.rating_feasibility)
            Spacer(modifier = Modifier.height(8.dp))
            ReviewRatingRow(label = "Availability", rating = review.rating_availability)

            // Comment (if present)
            if (!review.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Comment:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ReviewRatingRow(label: String, rating: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (index < rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format("%.1f", rating.toDouble()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReviewDialog(
    review: Review,
    onDismiss: () -> Unit,
    onConfirm: (ReviewCreate) -> Unit,
    actionState: ReviewActionState
) {
    var ratingClarity by remember { mutableStateOf(review.rating_clarity) }
    var ratingFeasibility by remember { mutableStateOf(review.rating_feasibility) }
    var ratingAvailability by remember { mutableStateOf(review.rating_availability) }
    var comment by remember { mutableStateOf(review.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Review") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rating Clarity
                Text(
                    text = "Lesson Clarity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                EditRatingSelector(
                    rating = ratingClarity,
                    onRatingChange = { ratingClarity = it }
                )

                Divider()

                // Rating Feasibility
                Text(
                    text = "Exam Feasibility",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                EditRatingSelector(
                    rating = ratingFeasibility,
                    onRatingChange = { ratingFeasibility = it }
                )

                Divider()

                // Rating Availability
                Text(
                    text = "Teacher Availability",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                EditRatingSelector(
                    rating = ratingAvailability,
                    onRatingChange = { ratingAvailability = it }
                )

                Divider()

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
                    placeholder = { Text("Add a comment...") },
                    maxLines = 4
                )

                // Error message
                if (actionState is ReviewActionState.Error) {
                    Text(
                        text = actionState.message,
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
                enabled = actionState !is ReviewActionState.Loading &&
                        ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0
            ) {
                if (actionState is ReviewActionState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = actionState !is ReviewActionState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditRatingSelector(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(i) }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rating $i",
                    tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = if (rating > 0) "$rating/5" else "Not rated",
            style = MaterialTheme.typography.bodyMedium,
            color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun DeleteReviewDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    actionState: ReviewActionState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Review") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete this review? This action cannot be undone.")

                if (actionState is ReviewActionState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = actionState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = actionState !is ReviewActionState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (actionState is ReviewActionState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = actionState !is ReviewActionState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}