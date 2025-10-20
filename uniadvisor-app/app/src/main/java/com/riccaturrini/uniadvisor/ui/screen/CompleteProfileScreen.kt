package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileCreationState

@Composable
fun CompleteProfileScreen(
    authViewModel: AuthViewModel,
    onProfileCreationSuccess: () -> Unit
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

    val days = (1..31).toList()
    val months = listOf(
        1 to "Gennaio", 2 to "Febbraio", 3 to "Marzo", 4 to "Aprile",
        5 to "Maggio", 6 to "Giugno", 7 to "Luglio", 8 to "Agosto",
        9 to "Settembre", 10 to "Ottobre", 11 to "Novembre", 12 to "Dicembre"
    )
    val years = (1950..2010).toList().reversed()

    LaunchedEffect(profileState) {
        if (profileState is ProfileCreationState.Success) {
            onProfileCreationSuccess()
            authViewModel.resetProfileState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Completa il tuo Profilo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Cognome") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Data di nascita section
        Text(
            text = "Data di Nascita",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expandedDay = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDay.toString())
                }
                DropdownMenu(
                    expanded = expandedDay,
                    onDismissRequest = { expandedDay = false }
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

            // Month Dropdown
            Box(modifier = Modifier.weight(2f)) {
                OutlinedButton(
                    onClick = { expandedMonth = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(months.find { it.first == selectedMonth }?.second ?: "")
                }
                DropdownMenu(
                    expanded = expandedMonth,
                    onDismissRequest = { expandedMonth = false }
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

            // Year Dropdown
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedButton(
                    onClick = { expandedYear = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedYear.toString())
                }
                DropdownMenu(
                    expanded = expandedYear,
                    onDismissRequest = { expandedYear = false }
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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Città") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (profileState is ProfileCreationState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank() && city.isNotBlank()) {
                        val birthDateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
                        val profile = UserProfileCreate(
                            first_name = firstName,
                            last_name = lastName,
                            birth_date = birthDateStr,
                            city = city
                        )
                        authViewModel.createUserAndProfile(profile)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crea Profilo")
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