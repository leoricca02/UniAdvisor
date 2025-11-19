package com.riccaturrini.uniadvisor.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.riccaturrini.uniadvisor.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen(
    onNavigateBack: () -> Unit,
    onCourseSelected: (Int) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val selectedFilter by viewModel.selectedFacultyFilter.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }

    val defaultPosition = LatLng(45.4642, 9.1900) // Adjust to your university location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 15f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.getCurrentLocation(context)
    }

    LaunchedEffect(Unit) {
        viewModel.initLocationClient(context)
        viewModel.loadMapData()
    }

    LaunchedEffect(locationState) {
        if (locationState is LocationState.Success) {
            val location = locationState as LocationState.Success
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude), 16f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            if (selectedFilter != null) Icons.Default.FilterAlt else Icons.Default.FilterList,
                            "Filter"
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Courses") },
                            onClick = {
                                viewModel.setFacultyFilter(null)
                                showFilterMenu = false
                            }
                        )
                        if (mapState is MapState.Success) {
                            val faculties = (mapState as MapState.Success).faculties
                            faculties.forEach { faculty ->
                                DropdownMenuItem(
                                    text = { Text(faculty.name) },
                                    onClick = {
                                        viewModel.setFacultyFilter(faculty.id)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (locationState is LocationState.Success) {
                        val location = locationState as LocationState.Success
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(location.latitude, location.longitude), 16f
                        )
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, "My Location")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (mapState) {
                is MapState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MapState.Error -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Error, null, Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text((mapState as MapState.Error).message)
                        Button(onClick = { viewModel.loadMapData() }) {
                            Text("Retry")
                        }
                    }
                }
                is MapState.Success -> {
                    val data = mapState as MapState.Success
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = locationState is LocationState.Success
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        data.faculties.forEach { faculty ->
                            Marker(
                                state = MarkerState(position = LatLng(faculty.latitude, faculty.longitude)),
                                title = faculty.name,
                                snippet = faculty.buildingName ?: "Faculty Department"
                            )
                        }
                        data.courses.forEach { course ->
                            Marker(
                                state = MarkerState(position = LatLng(course.latitude, course.longitude)),
                                title = course.name,
                                snippet = "${course.buildingName} - ${course.roomNumber}",
                                onInfoWindowClick = { onCourseSelected(course.id) }
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.School, null)
                                Text("${data.faculties.size}", fontWeight = FontWeight.Bold)
                                Text("Faculties", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Place, null)
                                Text("${data.courses.size}", fontWeight = FontWeight.Bold)
                                Text("Courses", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}