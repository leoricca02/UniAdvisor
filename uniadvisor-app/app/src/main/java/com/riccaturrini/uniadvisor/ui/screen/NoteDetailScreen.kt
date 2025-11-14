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
import com.riccaturrini.uniadvisor.network.ApiClient
import com.riccaturrini.uniadvisor.ui.activity.PdfViewerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    courseId: Int,
    onNavigateBack: () -> Unit,
    courseDetailViewModel: CourseDetailViewModel = viewModel()
) {
    val context = LocalContext.current

    // State for note details and reviews
    var note by remember { mutableStateOf<NoteWithRating?>(null) }
    var reviews by remember { mutableStateOf<List<NoteRating>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showRateDialog by remember { mutableStateOf(false) }
    val noteRatingState by courseDetailViewModel.noteRatingState.collectAsState()

    // Load note and reviews
    LaunchedEffect(noteId) {
        isLoading = true
        errorMessage = null
        try {
            // Load note details (we need to get this from the course)
            val courseResponse = ApiClient.instance.getNotesWithRatings(courseId)
            if (courseResponse.isSuccessful && courseResponse.body() != null) {
                note = courseResponse.body()!!.find { it.id == noteId }
                if (note == null) {
                    errorMessage = "Note not found"
                }
            } else {
                errorMessage = "Failed to load note"
            }

            // Load reviews
            val reviewsResponse = ApiClient.instance.getNoteReviews(noteId)
            if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                reviews = reviewsResponse.body()!!
                Log.d("NoteDetailScreen", "✅ Loaded ${reviews.size} reviews")
            }
        } catch (e: Exception) {
            Log.e("NoteDetailScreen", "Error loading note details", e)
            errorMessage = e.message ?: "Connection error"
        } finally {
            isLoading = false
        }
    }

    // Handle rating success
    LaunchedEffect(noteRatingState) {
        if (noteRatingState is NoteRatingState.Success) {
            showRateDialog = false
            // Reload reviews
            try {
                val reviewsResponse = ApiClient.instance.getNoteReviews(noteId)
                if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                    reviews = reviewsResponse.body()!!
                }
                // Reload note to update average rating
                val courseResponse = ApiClient.instance.getNotesWithRatings(courseId)
                if (courseResponse.isSuccessful && courseResponse.body() != null) {
                    note = courseResponse.body()!!.find { it.id == noteId }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailScreen", "Error reloading after rating", e)
            }
            courseDetailViewModel.resetNoteRatingState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Details") },
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
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
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                note != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Note Info Card
                        item {
                            NoteInfoCard(
                                note = note!!,
                                onViewPdf = {
                                    val intent = Intent(context, PdfViewerActivity::class.java).apply {
                                        putExtra("PDF_URL", note!!.file_id)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Rate Button
                        item {
                            Button(
                                onClick = { showRateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rate this Note")
                            }
                        }

                        // Reviews Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reviews (${reviews.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Reviews List
                        if (reviews.isEmpty()) {
                            item {
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
                                            text = "Be the first to review this note!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(reviews) { review ->
                                NoteReviewDetailCard(review = review)
                            }
                        }

                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Rate Dialog
            if (showRateDialog) {
                RateNoteDialog(
                    onDismiss = {
                        showRateDialog = false
                        courseDetailViewModel.resetNoteRatingState()
                    },
                    onConfirm = { rating, comment ->
                        courseDetailViewModel.addNoteRating(courseId, noteId, rating, comment)
                    },
                    noteRatingState = noteRatingState
                )
            }
        }
    }
}

@Composable
fun NoteInfoCard(
    note: NoteWithRating,
    onViewPdf: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Student Note",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Average Rating (if present)
            if (note.average_rating != null && note.average_rating > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Average Rating",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFFFFD700).copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = String.format("%.1f / 5.0", note.average_rating),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Description
            if (!note.description.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // View PDF Button
            // View PDF Button
            Button(
                onClick = onViewPdf,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview PDF")
            }
        }
    }
}

@Composable
fun NoteReviewDetailCard(review: NoteRating) {
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
            // Header with stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Star rating display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Rating badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        review.rating >= 4 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        review.rating >= 3 -> Color(0xFFFFC107).copy(alpha = 0.2f)
                        else -> Color(0xFFF44336).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = "${review.rating}/5",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Comment
            if (!review.comment.isNullOrBlank()) {
                HorizontalDivider()
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Date
            Text(
                text = review.created_at,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}