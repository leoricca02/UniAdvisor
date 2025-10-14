// File: MainActivity.kt
package com.riccaturrini.uniadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.screen.CoursesListScreen
import com.riccaturrini.uniadvisor.ui.screen.LoginScreen
import com.riccaturrini.uniadvisor.ui.screen.SignUpScreen
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme
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
    // Condividiamo lo stesso ViewModel tra le schermate di login e registrazione
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable(route = "login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    // Naviga alla lista corsi e pulisce lo stack di navigazione
                    navController.navigate("courses_list") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        composable(route = "signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = {
                    // Torna alla schermata di login dopo la registrazione
                    navController.popBackStack()
                }
            )
        }

        composable(route = "courses_list") {
            CoursesListScreen(faculty = "Engineering in Computer Science")
        }
    }
}