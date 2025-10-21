package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.R
import com.riccaturrini.uniadvisor.viewmodel.EnrollmentState
import com.riccaturrini.uniadvisor.viewmodel.FacultyUiState
import com.riccaturrini.uniadvisor.viewmodel.FacultyViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFacultyScreen(
    facultyViewModel: FacultyViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    onFacultySelected: () -> Unit
) {
    val facultyState by facultyViewModel.uiState.collectAsState()
    val enrollState by profileViewModel.enrollState.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }
    var selectedFacultyId by remember { mutableStateOf<Int?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }

    // Naviga solo dopo successo enrollment
    LaunchedEffect(enrollState) {
        if (enrollState is EnrollmentState.Success && !hasNavigated) {
            hasNavigated = true
            delay(200) // evita race con Android activity lifecycle
            onFacultySelected()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO
            Icon(
                painter = painterResource(id = R.drawable.uniadvisor_logo),
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = facultyState) {
                        is FacultyUiState.Loading -> {
                            CircularProgressIndicator()
                        }

                        is FacultyUiState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                val m = facultyViewModel.javaClass.getDeclaredMethod("fetchFaculties")
                                m.isAccessible = true
                                m.invoke(facultyViewModel)
                            }) {
                                Text("Riprova")
                            }
                        }

                        is FacultyUiState.Success -> {
                            Text(
                                text = "Scegli la tua facoltà",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedFaculty ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Facoltà") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    state.faculties.forEach { faculty ->
                                        DropdownMenuItem(
                                            text = { Text(faculty.name) },
                                            onClick = {
                                                selectedFaculty = faculty.name
                                                selectedFacultyId = faculty.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            when (enrollState) {
                                is EnrollmentState.Loading -> {
                                    CircularProgressIndicator()
                                }

                                else -> {
                                    Button(
                                        onClick = {
                                            selectedFacultyId?.let {
                                                profileViewModel.enrollInFaculty(it)
                                            }
                                        },
                                        enabled = selectedFacultyId != null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Conferma")
                                    }
                                }
                            }

                            if (enrollState is EnrollmentState.Error) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = (enrollState as EnrollmentState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
