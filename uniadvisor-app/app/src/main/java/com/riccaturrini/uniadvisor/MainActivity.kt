package com.riccaturrini.uniadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.riccaturrini.uniadvisor.ui.screen.CompleteProfileScreen
import com.riccaturrini.uniadvisor.ui.screen.CourseDetailScreen
import com.riccaturrini.uniadvisor.ui.screen.CoursesListScreen
import com.riccaturrini.uniadvisor.ui.screen.LoginScreen
import com.riccaturrini.uniadvisor.ui.screen.SelectFacultyScreen
import com.riccaturrini.uniadvisor.ui.screen.SignUpScreen
import com.riccaturrini.uniadvisor.ui.screen.SplashScreen
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
    val authState by authViewModel.authUiState.collectAsState()

    // Gestisce la navigazione automatica dopo il controllo del profilo nello splash
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                // Check if user has faculty
                val userData = authViewModel.currentUserData.value
                if (userData?.faculty_id != null) {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("select_faculty") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                authViewModel.resetState()
            }
            is AuthUiState.ProfileCreationRequired -> {
                navController.navigate("complete_profile") {
                    popUpTo("splash") { inclusive = true }
                }
                authViewModel.resetState()
            }
            else -> { /* Idle, Loading, Error - non fare nulla */ }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToCompleteProfile = {
                    navController.navigate("complete_profile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToCompleteProfile = {
                    navController.navigate("complete_profile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("complete_profile") {
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onProfileCreationSuccess = {
                    navController.navigate("select_faculty") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("select_faculty") {
            SelectFacultyScreen(
                onFacultySelected = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToCourses = {
                    navController.navigate("courses")
                }
            )
        }
        composable("courses") {
            val userData by authViewModel.currentUserData.collectAsState()
            val facultyId = userData?.faculty_id ?: 1

            CoursesListScreen(
                facultyId = facultyId,
                onBackPressed = { navController.popBackStack() },
                onCourseClick = { courseId ->
                    navController.navigate("course_detail/$courseId")
                }
            )
        }
        composable("course_detail/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: 0
            CourseDetailScreen(
                courseId = courseId,
                onBackPressed = { navController.popBackStack() },
                onAddReview = {
                    // TODO: Navigate to add review screen
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToCourses: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome Home!", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToCourses,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text("I Miei Corsi")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                Firebase.auth.signOut()
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Logout (per test)")
        }
    }
}