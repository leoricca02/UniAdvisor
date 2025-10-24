package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.riccaturrini.uniadvisor.R
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileCreationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    authViewModel: AuthViewModel,
    onProfileCreationSuccess: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val profileState by authViewModel.profileState.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    // Date dropdowns
    var selectedDay by remember { mutableStateOf(1) }
    var selectedMonth by remember { mutableStateOf(1) }
    var selectedYear by remember { mutableStateOf(2000) }

    var expandedDay by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    // Dialog state for logout confirmation
    var showLogoutDialog by remember { mutableStateOf(false) }

    val days = (1..31).toList()
    val months = listOf(
        1 to "January", 2 to "February", 3 to "March", 4 to "April",
        5 to "May", 6 to "June", 7 to "July", 8 to "August",
        9 to "September", 10 to "October", 11 to "November", 12 to "December"
    )
    val years = (1950..2010).toList().reversed()

    LaunchedEffect(profileState) {
        if (profileState is ProfileCreationState.Success) {
            onProfileCreationSuccess()
            authViewModel.resetProfileState()
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out? The data entered will not be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete your Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Logout button in TopBar
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Icon(
                    painter = painterResource(id = R.drawable.uniadvisor_logo),
                    contentDescription = "UniAdvisor Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(180.dp)
                        .padding(bottom = 12.dp)
                )

                Text(
                    text = "Complete your Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(10.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Birthdate",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Day
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expandedDay = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedDay.toString())
                                }
                                DropdownMenu(
                                    expanded = expandedDay,
                                    onDismissRequest = { expandedDay = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    days.forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day.toString()) },
                                            onClick = {
                                                selectedDay = day
                                                expandedDay = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Month
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedButton(
                                    onClick = { expandedMonth = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(months.find { it.first == selectedMonth }?.second ?: "")
                                }
                                DropdownMenu(
                                    expanded = expandedMonth,
                                    onDismissRequest = { expandedMonth = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    months.forEach { (num, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedMonth = num
                                                expandedMonth = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expandedYear = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedYear.toString())
                                }
                                DropdownMenu(
                                    expanded = expandedYear,
                                    onDismissRequest = { expandedYear = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    years.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString()) },
                                            onClick = {
                                                selectedYear = year
                                                expandedYear = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (profileState is ProfileCreationState.Loading) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = {
                                    val birthDateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
                                    val profile = UserProfileCreate(
                                        first_name = firstName,
                                        last_name = lastName,
                                        birth_date = birthDateStr,
                                        city = city
                                    )
                                    authViewModel.createUserAndProfile(profile)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = firstName.isNotBlank() && lastName.isNotBlank() && city.isNotBlank()
                            ) {
                                Text("Create profile")
                            }

                            // Logout button at bottom
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exit without saving")
                            }
                        }

                        if (profileState is ProfileCreationState.Error) {
                            Text(
                                text = (profileState as ProfileCreationState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}