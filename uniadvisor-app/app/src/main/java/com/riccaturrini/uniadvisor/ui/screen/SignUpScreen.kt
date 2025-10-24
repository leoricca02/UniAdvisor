package com.riccaturrini.uniadvisor.ui.screen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.riccaturrini.uniadvisor.R
import com.riccaturrini.uniadvisor.data.UserProfileCreate
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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

    val authState by authViewModel.authUiState.collectAsState()
    val context = LocalContext.current

    // Validazioni
    val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
    val isEmailValid = email.isEmpty() || email.matches(emailRegex)
    val isPasswordLengthValid = password.length >= 6
    val isPasswordUppercaseValid = password.any { it.isUpperCase() }
    val isConfirmPasswordValid = confirmPassword.isNotEmpty() && password == confirmPassword

    // Google Sign-In setup
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.your_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                authViewModel.signInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("SignUpScreen", "Google sign in failed", e)
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.ProfileCreationRequired) {
            onNavigateToCompleteProfile()
            authViewModel.resetState()
        }
    }

    // --- UI ---
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // LOGO
            Icon(
                painter = painterResource(id = R.drawable.uniadvisor_logo), // XML vettoriale consigliato
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Create your account",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // --- Campi ---
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = email.isNotEmpty() && !isEmailValid
                    )
                    if (email.isNotEmpty() && !isEmailValid) {
                        Text(
                            "Invalid email",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Data di nascita ---
                    Text(
                        "Date of Birth",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wrap each DropdownSelector in a Box with weight so they sit on the same row
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelector("Day", selectedDay.toString(), days.map { it.toString() }) {
                                selectedDay = it
                            }
                        }

                        Box(modifier = Modifier.weight(1.2f)) {
                            DropdownSelector(
                                "Month",
                                months.find { it.first == selectedMonth }?.second ?: "",
                                months.map { it.second }
                            ) {
                                selectedMonth = it
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelector("Year", selectedYear.toString(), years.map { it.toString() }) {
                                selectedYear = it
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password requirements (turn green when met)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Password requirements:", fontStyle = FontStyle.Italic, fontSize = 13.sp)
                        val successGreen = Color(0xFF2E7D32)
                        Text(
                            "• Minimum 6 characters",
                            color = if (isPasswordLengthValid) successGreen else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "• At least one uppercase letter",
                            color = if (isPasswordUppercaseValid) successGreen else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "• Passwords must match",
                            color = if (isConfirmPasswordValid) successGreen else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (authState is AuthUiState.Loading) {
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
                                authViewModel.signUpWithProfile(email, password, profileData)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = firstName.isNotEmpty() && lastName.isNotEmpty() &&
                                    city.isNotEmpty() && email.isNotEmpty() && isEmailValid &&
                                    isPasswordLengthValid && isPasswordUppercaseValid && isConfirmPasswordValid
                        ) {
                            Text("Register")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Register with Google") }

                        TextButton(onClick = onNavigateToLogin) {
                            Text("Already have an account? Sign In")
                        }
                    }

                    if (authState is AuthUiState.Error) {
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
}

/**
 * Componente helper per i menu a discesa (giorno, mese, anno)
 */
@Composable
fun DropdownSelector(label: String, selectedText: String, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(selectedText) }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index + 1) // restituisce 1..N
                        expanded = false
                    }
                )
            }
        }
    }
}
