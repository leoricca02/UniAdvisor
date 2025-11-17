package com.riccaturrini.uniadvisor.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.riccaturrini.uniadvisor.data.*
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class MapState {
    object Loading : MapState()
    data class Success(
        val faculties: List<FacultyMapMarker>,
        val courses: List<CourseMapMarker>
    ) : MapState()
    data class Error(val message: String) : MapState()
}

sealed class LocationState {
    object Unknown : LocationState()
    object Loading : LocationState()
    data class Success(val latitude: Double, val longitude: Double) : LocationState()
    data class Error(val message: String) : LocationState()
}

class MapViewModel : ViewModel() {

    private val apiService = ApiClient.instance

    private val _mapState = MutableStateFlow<MapState>(MapState.Loading)
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Unknown)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _selectedFacultyFilter = MutableStateFlow<Int?>(null)
    val selectedFacultyFilter: StateFlow<Int?> = _selectedFacultyFilter.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Initialize location client
     */
    fun initLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Load map data (faculties and courses)
     */
    fun loadMapData(facultyFilter: Int? = null) {
        viewModelScope.launch {
            _mapState.value = MapState.Loading
            try {
                Log.d("MapViewModel", "🗺️ Loading map data, facultyFilter=$facultyFilter")

                // Load faculties
                val facultiesResponse = apiService.getFacultiesForMap()
                if (!facultiesResponse.isSuccessful) {
                    _mapState.value = MapState.Error("Failed to load faculties")
                    return@launch
                }
                val faculties = facultiesResponse.body() ?: emptyList()
                Log.d("MapViewModel", "✅ Loaded ${faculties.size} faculties")

                // Load courses (with optional faculty filter)
                val coursesResponse = apiService.getCoursesForMap(facultyFilter)
                if (!coursesResponse.isSuccessful) {
                    _mapState.value = MapState.Error("Failed to load courses")
                    return@launch
                }
                val courses = coursesResponse.body() ?: emptyList()
                Log.d("MapViewModel", "✅ Loaded ${courses.size} courses")

                _mapState.value = MapState.Success(faculties, courses)

            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error loading map data", e)
                _mapState.value = MapState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Set faculty filter and reload data
     */
    fun setFacultyFilter(facultyId: Int?) {
        _selectedFacultyFilter.value = facultyId
        loadMapData(facultyId)
    }

    /**
     * Get current user location
     */
    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            _locationState.value = LocationState.Loading

            // Check permissions
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                _locationState.value = LocationState.Error("Location permission not granted")
                return@launch
            }

            try {
                // Initialize client if needed
                if (fusedLocationClient == null) {
                    initLocationClient(context)
                }

                val client = fusedLocationClient!!

                // Get current location with high priority
                val cancellationTokenSource = CancellationTokenSource()

                @Suppress("MissingPermission")
                val location: Location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()

                Log.d("MapViewModel", "📍 Current location: ${location.latitude}, ${location.longitude}")
                _locationState.value = LocationState.Success(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error getting location", e)
                _locationState.value = LocationState.Error(e.message ?: "Failed to get location")
            }
        }
    }

    /**
     * Get nearby courses based on current location
     */
    suspend fun getNearbyCourses(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 1000.0,
        facultyId: Int? = null
    ): NearbyCourses? {
        return try {
            val response = apiService.getNearbyCourses(
                latitude, longitude, radiusMeters, facultyId
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("MapViewModel", "Failed to get nearby courses: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error getting nearby courses", e)
            null
        }
    }

    /**
     * Get course location with distance calculation
     */
    suspend fun getCourseLocationWithDistance(
        courseId: Int,
        userLatitude: Double?,
        userLongitude: Double?
    ): CourseLocation? {
        return try {
            val response = apiService.getCourseLocation(
                courseId, userLatitude, userLongitude
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("MapViewModel", "Failed to get course location: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error getting course location", e)
            null
        }
    }

    /**
     * Get navigation info for a course
     */
    suspend fun getNavigationInfo(
        courseId: Int,
        userLatitude: Double,
        userLongitude: Double
    ): NavigationInfo? {
        return try {
            val response = apiService.getNavigationInfo(
                courseId, userLatitude, userLongitude
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("MapViewModel", "Failed to get navigation info: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error getting navigation info", e)
            null
        }
    }
}