// File: MainActivity.kt
package com.riccaturrini.uniadvisor // Assicurati che il package name sia il tuo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.screen.CoursesListScreen // Importa la tua schermata
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniAdvisorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Avviamo il sistema di navigazione della nostra app
                    UniAdvisorApp()
                }
            }
        }
    }
}

@Composable
fun UniAdvisorApp() {
    // 1. Creiamo un controller che gestisce la navigazione
    val navController = rememberNavController()

    // 2. Definiamo il "NavHost", il contenitore che scambierà le schermate
    NavHost(
        navController = navController,
        startDestination = "login" // 3. Definiamo la schermata di partenza
    ) {
        // Definiamo la rotta per la schermata di login
        composable(route = "login") {
            // Quando la rotta è "login", mostriamo la LoginScreen
            LoginScreen(
                onLoginSuccess = {
                    // Quando il login ha successo, navighiamo alla lista dei corsi
                    navController.navigate("courses_list")
                }
            )
        }

        // Definiamo la rotta per la lista dei corsi
        composable(route = "courses_list") {
            // Quando la rotta è "courses_list", mostriamo la CoursesListScreen
            // TODO: In futuro passeremo qui la facoltà dell'utente
            CoursesListScreen(faculty = "Engineering in Computer Science")
        }
    }
}

// Questa è una schermata di Login finta, da sostituire con la tua vera LoginScreen.kt
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onLoginSuccess) {
            Text("Vai alla Lista Corsi (Login Finto)")
        }
    }
}