// File: ui/screen/SignUpScreen.kt
package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import java.util.regex.Pattern

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,               // aggiunto
    onNavigateToCompleteProfile: () -> Unit     // aggiunto
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Dropdown data di nascita
    val days = (1..31).toList()
    val months = (1..12).toList()
    val years = (1900..2025).toList()
    var selectedDay by remember { mutableStateOf(1) }
    var selectedMonth by remember { mutableStateOf(1) }
    var selectedYear by remember { mutableStateOf(2000) }

    val authState by authViewModel.uiState.collectAsState()

    // Validazioni
    val isEmailValid = email.isEmpty() || Pattern.compile("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+").matcher(email).matches()
    val isPasswordLengthValid = password.length >= 6
    val isPasswordUppercaseValid = password.any { it.isUpperCase() }
    val isConfirmPasswordValid = confirmPassword.isNotEmpty() && password == confirmPassword


    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            onSignUpSuccess()
            authViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registrati", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.height(16.dp))

                Text("Data di nascita", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SimpleDropdown(days, selectedDay) { selectedDay = it }
                    SimpleDropdown(months, selectedMonth) { selectedMonth = it }
                    SimpleDropdown(years, selectedYear) { selectedYear = it }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Città") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Conferma Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid
                )

                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        "• Minimo 6 caratteri",
                        color = if(isPasswordLengthValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        "• Almeno una maiuscola",
                        color = if(isPasswordUppercaseValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        "• Le password devono coincidere",
                        color = if(isConfirmPasswordValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )

                }

                Spacer(modifier = Modifier.height(24.dp))

                if(authState is AuthUiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            val birthDateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
                            val profileData = UserProfileCreate(
                                first_name = firstName,
                                last_name = lastName,
                                birth_date = birthDateStr,
                                city = city
                            )
                            authViewModel.createUserAndProfile(email, password, profileData)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = firstName.isNotEmpty() && lastName.isNotEmpty() && city.isNotEmpty() &&
                                email.isNotEmpty() && isEmailValid && isPasswordLengthValid &&
                                isPasswordUppercaseValid && isConfirmPasswordValid
                    ) {
                        Text("Crea Account")
                    }
                }

                if(authState is AuthUiState.Error) {
                    Text(
                        text = (authState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}



