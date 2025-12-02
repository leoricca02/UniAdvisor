package com.riccaturrini.uniadvisor.ui.screen

import android.content.Intent
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
import com.riccaturrini.uniadvisor.viewmodel.CourseDetailViewModel
import com.riccaturrini.uniadvisor.viewmodel.NoteRatingState
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

    // State per note e recensioni
    var note by remember { mutableStateOf<NoteWithRating?>(null) }
    var reviews by remember { mutableStateOf<List<NoteRating>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showRateDialog by remember { mutableStateOf(false) }
    val noteRatingState by courseDetailViewModel.noteRatingState.collectAsState()

    // --- NUOVO: Stati per il filtro ---
    var sortOrder by remember { mutableStateOf("newest") } // opzioni: newest, oldest, best, worst
    var showSortMenu by remember { mutableStateOf(false) }

    // Stati per edit review
    var editReviewState by remember { mutableStateOf<NoteRating?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // current user id (può essere null se non ancora caricato)
    val currentUserId by courseDetailViewModel.currentUserId.collectAsState()

    // --- NUOVO: Logica di ordinamento ---
    val sortedReviews = remember(reviews, sortOrder) {
        when (sortOrder) {
            "newest" -> reviews.sortedByDescending { it.created_at }
            "oldest" -> reviews.sortedBy { it.created_at }
            "best" -> reviews.sortedByDescending { it.rating }
            "worst" -> reviews.sortedBy { it.rating }
            else -> reviews
        }
    }

    // Load note and reviews (Codice esistente invariato...)
    LaunchedEffect(noteId) {
        isLoading = true
        errorMessage = null
        try {
            val courseResponse = ApiClient.instance.getNotesWithRatings(courseId)
            if (courseResponse.isSuccessful && courseResponse.body() != null) {
                note = courseResponse.body()!!.find { it.id == noteId }
                if (note == null) errorMessage = "Note not found"
            } else {
                errorMessage = "Failed to load note"
            }

            val reviewsResponse = ApiClient.instance.getNoteReviews(noteId)
            if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                reviews = reviewsResponse.body()!!
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Connection error"
        } finally {
            isLoading = false
        }
    }

    // Handle rating/update success -> reload reviews and note summary
    LaunchedEffect(noteRatingState) {
        if (noteRatingState is NoteRatingState.Success) {
            showRateDialog = false
            try {
                val reviewsResponse = ApiClient.instance.getNoteReviews(noteId)
                if (reviewsResponse.isSuccessful && reviewsResponse.body() != null) {
                    reviews = reviewsResponse.body()!!
                }
                val courseResponse = ApiClient.instance.getNotesWithRatings(courseId)
                if (courseResponse.isSuccessful && courseResponse.body() != null) {
                    note = courseResponse.body()!!.find { it.id == noteId }
                }
            } catch (e: Exception) {
                Log.e("NoteDetailScreen", "Error reloading after rating/update", e)
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
                // --- NUOVO: Action Menu per il Sort ---
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort Reviews")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📅 Newest first") },
                                onClick = { sortOrder = "newest"; showSortMenu = false },
                                leadingIcon = { if(sortOrder == "newest") Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("📅 Oldest first") },
                                onClick = { sortOrder = "oldest"; showSortMenu = false },
                                leadingIcon = { if(sortOrder == "oldest") Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("⭐ Highest rated") },
                                onClick = { sortOrder = "best"; showSortMenu = false },
                                leadingIcon = { if(sortOrder == "best") Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("⭐ Lowest rated") },
                                onClick = { sortOrder = "worst"; showSortMenu = false },
                                leadingIcon = { if(sortOrder == "worst") Icon(Icons.Default.Check, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                note != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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

                        item {
                            Button(
                                onClick = { showRateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Rate this Note")
                            }
                        }

                        // Reviews Header
                        item {
                            Text(
                                text = "Reviews (${reviews.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Reviews List (Usando sortedReviews)
                        if (sortedReviews.isEmpty()) {
                            item {
                                Text("No reviews yet. Be the first!", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            items(sortedReviews) { review ->
                                NoteReviewDetailCard(
                                    review = review,
                                    currentUserId = currentUserId,
                                    onEditReview = {
                                        editReviewState = it
                                        showEditDialog = true
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }

            if (showRateDialog) {
                RateNoteDialog(
                    onDismiss = { showRateDialog = false; courseDetailViewModel.resetNoteRatingState() },
                    onConfirm = { rating, comment -> courseDetailViewModel.addNoteRating(courseId, noteId, rating, comment) },
                    noteRatingState = noteRatingState
                )
            }

            // Edit dialog
            if (showEditDialog && editReviewState != null) {
                EditReviewDialog(
                    review = editReviewState!!,
                    onDismiss = {
                        showEditDialog = false
                        editReviewState = null
                    },
                    onConfirm = { newRating, newComment ->
                        // Use ViewModel to call update endpoint
                        courseDetailViewModel.updateNoteReview(
                            ratingId = editReviewState!!.id,
                            rating = newRating,
                            comment = newComment
                        )
                        showEditDialog = false
                        editReviewState = null
                    }
                )
            }
        }
    }
}

fun getBadgeColorForRating(rating: Double?): Color {
    return when {
        rating == null || rating <= 0.0 -> Color(0xFF9E9E9E) // Grigio: Rating non disponibile/nullo
        rating >= 4.0 -> Color(0xFF4CAF50)                   // Verde: Ottimo (4.0 - 5.0)
        rating >= 3.0 -> Color(0xFFFFC107)                   // Giallo/Arancio: Medio (3.0 - 3.9)
        else -> Color(0xFFF44336)                            // Rosso: Basso (< 3.0)
    }
}

@Composable
fun NoteInfoCard(
    note: NoteWithRating,
    onViewPdf: () -> Unit
) {
    val noteTitle = (note.description ?: "").ifBlank { "Untitled Note" }
    val badgeColor = remember(note.average_rating) {
        getBadgeColorForRating(note.average_rating)
    }

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
            // Header
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
                    text = noteTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Average Rating
            if (note.average_rating != null && note.average_rating > 0) {
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

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
                        color = badgeColor.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = badgeColor,
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

/**
 * Composable che mostra la singola review + icona matita per l'autore.
 * Modificata per accettare currentUserId e onEditReview.
 */
@Composable
fun NoteReviewDetailCard(
    review: NoteRating,
    currentUserId: Int?,
    onEditReview: (NoteRating) -> Unit
) {
    val formattedDate = remember(review.created_at) {
        formatReviewDate(review.created_at)
    }

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
            // Top row: Data a sinistra, Matita + Badge a destra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Data a sinistra
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Matita + Badge a destra
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Matita solo se currentUser è autore
                    if (currentUserId != null && review.student_id == currentUserId) {
                        IconButton(onClick = { onEditReview(review) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Review",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Badge rating subito dopo la matita
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            review.rating >= 4 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            review.rating >= 3 -> Color(0xFFFFC107).copy(alpha = 0.2f)
                            else -> Color(0xFFF44336).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "${review.rating}.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Star rating sotto
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

            // Commento
            if (!review.comment.isNullOrBlank()) {
                Divider()
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}




/**
 * Dialog per modificare una review (rating + comment) con valori precompilati
 */
@Composable
fun EditReviewDialog(
    review: NoteRating,
    onDismiss: () -> Unit,
    onConfirm: (rating: Int, comment: String) -> Unit
) {
    var newRating by remember { mutableStateOf(review.rating) }
    var newComment by remember { mutableStateOf(review.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Review") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    repeat(5) { index ->
                        IconButton(onClick = { newRating = index + 1 }) {
                            Icon(
                                imageVector = if (index < newRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (index < newRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newRating, newComment) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
