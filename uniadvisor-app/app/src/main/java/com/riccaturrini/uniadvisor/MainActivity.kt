package com.riccaturrini.uniadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.screen.CompleteProfileScreen
import com.riccaturrini.uniadvisor.ui.screen.LoginScreen
import com.riccaturrini.uniadvisor.ui.screen.SignUpScreen
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniAdvisorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UniAdvisorApp()
                }
            }
        }
    }
}

@Composable
fun UniAdvisorApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Raccolgo lo stato dell'autenticazione dal ViewModel
    val uiState by authViewModel.uiState.collectAsState()

    // Gestione dello stato di caricamento
    if (uiState is AuthUiState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // NavHost con navigazione basata sullo stato di autenticazione
        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    },
                    onNavigateToCompleteProfile = {
                        navController.navigate("complete_profile")
                    }
                )
            }

            composable("signup") {
                SignUpScreen(
                    authViewModel = authViewModel,
                    onSignUpSuccess = {
                        navController.navigate("home") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onNavigateToCompleteProfile = {
                        navController.navigate("complete_profile")
                    }
                )
            }

            composable("complete_profile") {
                CompleteProfileScreen(
                    authViewModel = authViewModel,
                    onProfileCreationSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                HomeScreen()
            }
        }

        // Navigazione automatica in base allo stato del ViewModel
        when (uiState) {
            is AuthUiState.ProfileCreationRequired -> {
                navController.navigate("complete_profile") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthUiState.Success -> {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> { /* Idle o Error: rimane sulla schermata corrente */ }
        }
    }
}

// Schermata Home placeholder
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Benvenuto!", style = MaterialTheme.typography.headlineLarge)
    }
}
