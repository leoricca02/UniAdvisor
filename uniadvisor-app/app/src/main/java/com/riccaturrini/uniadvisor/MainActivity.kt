package com.riccaturrini.uniadvisor

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast // Added for debug message
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.screen.*
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme
import com.riccaturrini.uniadvisor.utils.PostureDetector
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // --- SMART ROTATION LOCK (BED MODE) ---
            val context = LocalContext.current
            val activity = context as? Activity

            // Initialize the sensor detector
            val postureDetector = remember { PostureDetector(context) }

            // Collect the "isLyingFlat" state (true = bed mode/flat, false = upright)
            val isLyingFlat by postureDetector.isLyingFlat.collectAsState(initial = false)

            // React to state changes
            LaunchedEffect(isLyingFlat) {
                if (isLyingFlat) {
                    // Lock to Portrait
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                    // DEBUG: Show a Toast when bed mode activates
                    //Toast.makeText(context, "Bed Mode Active: Rotation Locked 🛏️", Toast.LENGTH_SHORT).show()
                    Log.d("SENSOR", "Bed Mode Detected: Locking to Portrait")
                } else {
                    // Unlock rotation
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    Log.d("SENSOR", "Upright Mode: Unlocking Rotation")
                }
            }
            // --------------------------------------

            UniAdvisorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
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

    // Log per monitorare ogni cambio di stato
    LaunchedEffect(authState) {
        val user = authViewModel.currentUserData.value
        Log.d("DEBUG_NAV", "🔄 Stato cambiato: $authState")
        Log.d("DEBUG_NAV", "👤 Utente corrente: ${user?.email}, FacultyID: ${user?.faculty_id}")

        if (authState is AuthUiState.Success) {
            if (user?.faculty_id != null) {
                Log.d("DEBUG_NAV", "✅ Utente ha facoltà -> Navigo a DASHBOARD")
                navController.navigate("dashboard") {
                    popUpTo("splash") { inclusive = true }
                    popUpTo("select_faculty") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                Log.d("DEBUG_NAV", "⚠️ Utente SENZA facoltà -> Navigo a SELECT_FACULTY")
                navController.navigate("select_faculty") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
            // Resetta lo stato per evitare loop, ma loggalo
            Log.d("DEBUG_NAV", "🧹 Resetting AuthState to Idle")
            authViewModel.resetState()
        } else if (authState is AuthUiState.ProfileCreationRequired) {
            Log.d("DEBUG_NAV", "🆕 Profilo richiesto -> Navigo a COMPLETE_PROFILE")
            navController.navigate("complete_profile") {
                popUpTo("splash") { inclusive = true }
            }
            authViewModel.resetState()
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SPLASH")
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("login") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: LOGIN")
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onLoginSuccess = { Log.d("DEBUG_NAV", "⚡ Login UI callback triggered (gestito da Global Listener)") },
                onNavigateToCompleteProfile = { navController.navigate("complete_profile") }
            )
        }

        composable("signup") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SIGNUP")
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToCompleteProfile = { navController.navigate("complete_profile") }
            )
        }

        composable("complete_profile") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: COMPLETE_PROFILE")
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onProfileCreationSuccess = { Log.d("DEBUG_NAV", "⚡ Profile Creation callback (gestito da Global Listener)") },
                onLogout = {
                    navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                }
            )
        }

        composable("select_faculty") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SELECT_FACULTY")
            SelectFacultyScreen(
                authViewModel = authViewModel,
                onFacultySelected = { Log.d("DEBUG_NAV", "⚡ Faculty Selected callback (gestito da Global Listener)") }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                authViewModel = authViewModel,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                }
            )
        }

        composable("calendar") {
            val currentUser by authViewModel.currentUserData.collectAsState()
            val facultyId = currentUser?.faculty_id

            CalendarScreen(
                onBackClick = { navController.popBackStack() },
                userFacultyId = facultyId
            )
        }
    }
}