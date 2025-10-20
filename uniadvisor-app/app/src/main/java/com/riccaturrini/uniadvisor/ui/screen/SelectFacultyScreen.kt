package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.viewmodel.EnrollmentState
import com.riccaturrini.uniadvisor.viewmodel.FacultyUiState
import com.riccaturrini.uniadvisor.viewmodel.FacultyViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFacultyScreen(
    facultyViewModel: FacultyViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    onFacultySelected: () -> Unit
) {
    val facultyState by facultyViewModel.uiState.collectAsState()
    val enrollState by profileViewModel.enrollState.collectAsState()

    // Navigate on successful enrollment
    LaunchedEffect(enrollState) {
        if (enrollState is EnrollmentState.Success) {
            onFacultySelected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleziona la tua Facoltà") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Scegli la facoltà a cui sei iscritto:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (val state = facultyState) {
                is FacultyUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is FacultyUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.faculties) { faculty ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        profileViewModel.enrollInFaculty(faculty.id)
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = faculty.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                is FacultyUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { facultyViewModel.uiState }) {
                                Text("Riprova")
                            }
                        }
                    }
                }
            }

            // Show enrollment loading/error state
            when (val state = enrollState) {
                is EnrollmentState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is EnrollmentState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> { /* Idle or Success - do nothing */ }
            }
        }
    }
}