package com.riccaturrini.uniadvisor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun UniAdvisorTopBar(
    title: String = "UniAdvisor",
    onLogoutClick: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // --- Logout Confirmation Dialog ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Custom Top Bar ---
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        // We use a Row that handles the status bar padding internally.
        // This allows the Surface (background) to fill the status bar area,
        // while the content (Text, Icon) is pushed down to the correct position.
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars) // Adds padding for the status bar
                .height(56.dp) // The height of the actual bar content
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}