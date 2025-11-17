package com.riccaturrini.uniadvisor.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccaturrini.uniadvisor.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()

    // Request location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.getCurrentLocation(context)
        }
    }

    // Load map data on screen load
    LaunchedEffect(Unit) {
        viewModel.initLocationClient(context)
        viewModel.loadMapData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Features Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Location Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📍 Your Location",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when (locationState) {
                            is LocationState.Unknown -> {
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Get My Location")
                                }
                            }
                            is LocationState.Loading -> {
                                CircularProgressIndicator()
                            }
                            is LocationState.Success -> {
                                val location = locationState as LocationState.Success
                                Text("✅ Latitude: ${location.latitude}")
                                Text("✅ Longitude: ${location.longitude}")
                            }
                            is LocationState.Error -> {
                                val error = locationState as LocationState.Error
                                Text("❌ Error: ${error.message}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Section 2: Map Data Status
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🗺️ Map Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        when (mapState) {
                            is MapState.Loading -> {
                                CircularProgressIndicator()
                                Text("Loading map data...")
                            }
                            is MapState.Success -> {
                                val data = mapState as MapState.Success
                                Text("✅ Faculties loaded: ${data.faculties.size}")
                                Text("✅ Courses loaded: ${data.courses.size}")

                                Button(
                                    onClick = { viewModel.loadMapData() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reload Data")
                                }
                            }
                            is MapState.Error -> {
                                val error = mapState as MapState.Error
                                Text("❌ Error: ${error.message}", color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = { viewModel.loadMapData() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Faculties List
            if (mapState is MapState.Success) {
                val data = mapState as MapState.Success

                item {
                    Text(
                        text = "🏛️ Faculties",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(data.faculties) { faculty ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Open in Google Maps
                            val uri = Uri.parse("geo:${faculty.latitude},${faculty.longitude}?q=${faculty.latitude},${faculty.longitude}(${faculty.name})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = faculty.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (faculty.buildingName != null) {
                                    Text(
                                        text = faculty.buildingName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "📍 ${faculty.latitude}, ${faculty.longitude}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Default.LocationOn, "Location")
                        }
                    }
                }

                // Section 4: Courses List
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📚 Courses",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(data.courses) { course ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Open in Google Maps
                            val uri = Uri.parse("geo:${course.latitude},${course.longitude}?q=${course.latitude},${course.longitude}(${course.name})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (course.buildingName != null && course.roomNumber != null) {
                                    Text(
                                        text = "${course.buildingName} - ${course.roomNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (course.floor != null) {
                                    Text(
                                        text = "Floor ${course.floor}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Icon(Icons.Default.Place, "Location")
                        }
                    }
                }
            }
        }
    }
}