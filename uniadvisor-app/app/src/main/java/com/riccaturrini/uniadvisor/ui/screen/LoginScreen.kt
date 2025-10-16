// File: ui/screen/LoginScreen.kt
package com.riccaturrini.uniadvisor.ui.screen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.riccaturrini.uniadvisor.R
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- Google Sign-In ---
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
                val idToken = account.idToken!!
                authViewModel.signInWithGoogle(idToken)
            } catch (e: ApiException) {
                Log.w("LoginScreen", "Google sign in failed", e)
            }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                onLoginSuccess()
                authViewModel.resetState()
            }
            is AuthUiState.ProfileCreationRequired -> {
                onNavigateToCompleteProfile()
                authViewModel.resetState()
            }
            else -> { /* Idle, Loading, Error handled below */ }
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
                Text("Accedi", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
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
                Spacer(modifier = Modifier.height(16.dp))

                if (authState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Button(
                        onClick = { authViewModel.signIn(email, password) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Accedi") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Accedi con Google") }
                    TextButton(onClick = onNavigateToSignUp) {
                        Text("Non hai un account? Registrati")
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