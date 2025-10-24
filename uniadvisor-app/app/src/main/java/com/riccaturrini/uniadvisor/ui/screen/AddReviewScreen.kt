package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.ReviewCreate
import com.riccaturrini.uniadvisor.viewmodel.ReviewViewModel
import com.riccaturrini.uniadvisor.viewmodel.ReviewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    courseId: Int,
    onReviewAdded: () -> Unit,
    onCancel: () -> Unit,
    reviewViewModel: ReviewViewModel = viewModel()
) {
    var ratingClarity by remember { mutableStateOf(0) }
    var ratingFeasibility by remember { mutableStateOf(0) }
    var ratingAvailability by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    val reviewState by reviewViewModel.reviewState.collectAsState()

    LaunchedEffect(reviewState) {
        if (reviewState is ReviewState.Success) {
            onReviewAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Recensione") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Valuta il corso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Rating Clarity
                    RatingSelector(
                        label = "Chiarezza delle lezioni",
                        rating = ratingClarity,
                        onRatingChange = { ratingClarity = it }
                    )

                    Divider()

                    // Rating Feasibility
                    RatingSelector(
                        label = "Fattibilità dell'esame",
                        rating = ratingFeasibility,
                        onRatingChange = { ratingFeasibility = it }
                    )

                    Divider()

                    // Rating Availability
                    RatingSelector(
                        label = "Disponibilità del docente",
                        rating = ratingAvailability,
                        onRatingChange = { ratingAvailability = it }
                    )
                }
            }

            // Comment section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Commento (opzionale)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Condividi la tua esperienza...") },
                        maxLines = 5
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            when (reviewState) {
                is ReviewState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ReviewState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (reviewState as ReviewState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0) {
                                    val review = ReviewCreate(
                                        rating_clarity = ratingClarity,
                                        rating_feasibility = ratingFeasibility,
                                        rating_availability = ratingAvailability,
                                        comment = comment.ifBlank { null }
                                    )
                                    reviewViewModel.addReview(courseId, review)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0
                        ) {
                            Text("Riprova")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            if (ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0) {
                                val review = ReviewCreate(
                                    rating_clarity = ratingClarity,
                                    rating_feasibility = ratingFeasibility,
                                    rating_availability = ratingAvailability,
                                    comment = comment.ifBlank { null }
                                )
                                reviewViewModel.addReview(courseId, review)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ratingClarity > 0 && ratingFeasibility > 0 && ratingAvailability > 0
                    ) {
                        Text("Invia Recensione")
                    }

                    if (ratingClarity == 0 || ratingFeasibility == 0 || ratingAvailability == 0) {
                        Text(
                            text = "Tutte le valutazioni sono obbligatorie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSelector(
    label: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = if (rating > 0) "$rating/5" else "Non valutato",
                style = MaterialTheme.typography.bodyMedium,
                color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}