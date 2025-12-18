package com.riccaturrini.uniadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme

@Composable
fun UniAdvisorBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // --- Accademico ---
        NavigationBarItem(
            selected = currentScreen == "faculty",
            onClick = { onNavigate("faculty") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = "Faculty"
                )
            },
            label = { Text("Faculty") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            )
        )

        // --- Valutazioni ---
        NavigationBarItem(
            selected = currentScreen == "reviews",
            onClick = { onNavigate("reviews") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Reviews"
                )
            },
            label = { Text("Reviews") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            )
        )

        // --- Home (centrale, rialzata) ---
        Box(
            modifier = Modifier
                .offset(y = (-5).dp) // 🔹 Solleva il bottone per farlo "fluttuare"
                .size(62.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(activeColor),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { onNavigate("home") }) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // --- Note ---
        NavigationBarItem(
            selected = currentScreen == "notes",
            onClick = { onNavigate("notes") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Notes"
                )
            },
            label = { Text("Notes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            )
        )

        // --- Profilo ---
        NavigationBarItem(
            selected = currentScreen == "profile",
            onClick = { onNavigate("profile") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = activeColor,
                selectedTextColor = activeColor,
                unselectedIconColor = inactiveColor,
                unselectedTextColor = inactiveColor
            )
        )
    }
}

