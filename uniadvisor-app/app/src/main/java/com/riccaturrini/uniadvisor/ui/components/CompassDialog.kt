package com.riccaturrini.uniadvisor.ui.components

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.riccaturrini.uniadvisor.utils.CompassSensor
import com.riccaturrini.uniadvisor.utils.getLocationFlow

@Composable
fun CompassDialog(
    initialLocation: Location,
    targetLat: Double,
    targetLng: Double,
    targetName: String,
    // --- Extra Info Parameters ---
    targetRoom: String? = null,
    targetFloor: Int? = null,
    targetBuilding: String? = null,
    // -----------------------------
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // --- SENSORS & LOCATION STATE ---
    val compassSensor = remember { CompassSensor(context) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var userLocation by remember { mutableStateOf(initialLocation) }

    // 1. Collect Compass Data (Phone Rotation)
    LaunchedEffect(Unit) {
        compassSensor.azimuth.collect { newAzimuth ->
            azimuth = newAzimuth
        }
    }

    // 2. Collect GPS Data (Real-time Distance)
    LaunchedEffect(Unit) {
        getLocationFlow(context).collect { newLocation ->
            userLocation = newLocation
        }
    }

    // 3. Dynamic Calculations
    val targetLocation = remember(targetLat, targetLng) {
        Location("target").apply {
            latitude = targetLat
            longitude = targetLng
        }
    }

    // Calculate Distance
    val distanceMeters = userLocation.distanceTo(targetLocation).toInt()

    // Calculate Bearing (Direction to target relative to North)
    val bearingToTarget = userLocation.bearingTo(targetLocation).let {
        (it + 360) % 360
    }

    // Calculate Arrow Rotation
    // Rotation = Bearing (Target) - Azimuth (Phone Heading)
    var rotationAngle = (bearingToTarget - azimuth)
    // Normalize to -180 to +180 for smooth animation
    if (rotationAngle > 180) rotationAngle -= 360
    if (rotationAngle < -180) rotationAngle += 360

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        label = "compassRotation"
    )

    // --- UI ---
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full width
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- 1. HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Navigating to",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. INFO SECTION (Updated Size) ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Line 1: Building (Ridimensionato)
                    if (targetBuilding != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp) // Icona leggermente più piccola
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = targetBuilding,
                                // QUI LA MODIFICA: da headlineSmall a titleMedium
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Line 2: Room - Floor (Secondary)
                    if (targetRoom != null || targetFloor != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Room
                            if (targetRoom != null) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = targetRoom,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Separator
                            if (targetRoom != null && targetFloor != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            // Floor
                            if (targetFloor != null) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Floor $targetFloor",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // --- 3. COMPASS ARROW ---
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Decor Ring
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.size(220.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        strokeWidth = 1.dp
                    )

                    // The Arrow
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Direction",
                        modifier = Modifier
                            .size(160.dp)
                            .rotate(animatedRotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // --- 4. DISTANCE & HINT ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$distanceMeters meters",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Keep phone flat for accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 5. MAPS BUTTON ---
                Button(
                    onClick = {
                        openGoogleMaps(context, targetLat, targetLng, targetName)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Google Maps")
                }
            }
        }
    }
}

// Helper to open Google Maps
private fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(browserIntent)
    }
}