package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.components.UniAdvisorBottomBar
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.CourseViewModel
import com.riccaturrini.uniadvisor.viewmodel.ProfileViewModel

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigate: (String) -> Unit, // Callback per navigazione "esterna" (es. Calendar o Camera nel MainNavHost)
    onLogout: () -> Unit = {}
) {
    val profileViewModel: ProfileViewModel = viewModel()
    // Questo è il controller interno per la BottomBar (Home, Notes, Profile, etc.)
    val navController = rememberNavController()
    val courseViewModel: CourseViewModel = viewModel()

    // Ensure user data is loaded
    LaunchedEffect(Unit) {
        if (authViewModel.currentUserData.value == null) {
            authViewModel.checkUserProfile()
        }
    }

    Scaffold(
        bottomBar = {
            UniAdvisorBottomBar(
                currentScreen = currentDestination(navController),
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        DashboardNavGraph(
            profileViewModel = profileViewModel,
            navController = navController,
            authViewModel = authViewModel,
            courseViewModel = courseViewModel,
            onParentNavigate = onNavigate,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// Navigation graph inside dashboard
@Composable
fun DashboardNavGraph(
    profileViewModel: ProfileViewModel,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    courseViewModel: CourseViewModel,
    onParentNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                profileViewModel = profileViewModel,
                authViewModel = authViewModel,
                onNavigate = { route ->
                    // LOGICA DI SMISTAMENTO:
                    if (route == "calendar") {
                        // Se la rotta è "calendar", usiamo il navigatore padre (MainActivity)
                        onParentNavigate(route)
                    } else {
                        // Altrimenti (faculty, notes, reviews...) navighiamo internamente alla Dashboard
                        navController.navigate(route) {
                            popUpTo("home") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onLogout = onLogout
            )
        }
        composable("faculty") {
            FacultyScreen(
                authViewModel = authViewModel,
                courseViewModel = courseViewModel
            )
        }
        composable("reviews") {
            ReviewsScreen(authViewModel = authViewModel)
        }
        composable("notes") {
            // --- FIX QUI: Passiamo il navController ---
            NotesScreen(
                navController = navController, // Necessario per la fotocamera
                authViewModel = authViewModel
            )
        }
        composable("profile") {
            ProfileScreen(
                profileViewModel = profileViewModel,
                authViewModel = authViewModel,
                courseViewModel = courseViewModel,
                onLogout = onLogout,
                onAccountDeleted = onLogout
            )
        }

        // NOTA IMPORTANTE:
        // Se "camera_ocr" non è definito qui dentro (nel grafo interno),
        // l'app crasherà quando proverai ad aprire la fotocamera da NotesScreen.
        // Se "camera_ocr" è nel MainNavHost (padre), NotesScreen non riuscirà a trovarlo
        // usando 'navController' (che è quello figlio).
        //
        // Soluzione rapida: Aggiungi la rotta camera anche qui se serve,
        // oppure assicurati che NotesScreen usi il root controller se la camera è fuori.
        composable("camera_ocr") {
            com.riccaturrini.uniadvisor.ui.screen.CameraOcrScreen(
                courseId = null, // Opzionale
                onNavigateBack = { navController.popBackStack() },
                onResult = { uri ->
                    // Restituisci il risultato a NotesScreen o CourseDetailScreen
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scanned_pdf_uri", uri.toString())
                    navController.popBackStack()
                }
            )
        }
    }
}

// Helper to know which section is active
@Composable
private fun currentDestination(navController: NavHostController): String {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    return currentDestination ?: "home"
}