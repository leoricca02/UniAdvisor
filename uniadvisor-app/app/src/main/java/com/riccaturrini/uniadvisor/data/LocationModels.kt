package com.riccaturrini.uniadvisor.data

import com.google.gson.annotations.SerializedName

// ============================================
// LOCATION-SPECIFIC DATA CLASSES
// ============================================

data class CourseLocation(
    val id: Int,
    val name: String,
    @SerializedName("room_number")
    val roomNumber: String?,
    @SerializedName("building_name")
    val buildingName: String?,
    val latitude: Double,
    val longitude: Double,
    val floor: Int?,
    @SerializedName("teacher_name")
    val teacherName: String?,
    @SerializedName("distance_meters")
    val distanceMeters: Double? = null,
    @SerializedName("walking_time_minutes")
    val walkingTimeMinutes: Int? = null
)

data class FacultyLocation(
    val id: Int,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    @SerializedName("building_name")
    val buildingName: String?
)

data class NavigationInfo(
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("course_name")
    val courseName: String,
    val destination: LocationDestination,
    @SerializedName("distance_meters")
    val distanceMeters: Double,
    @SerializedName("walking_time_minutes")
    val walkingTimeMinutes: Int,
    @SerializedName("google_maps_url")
    val googleMapsUrl: String,
    @SerializedName("waze_url")
    val wazeUrl: String
)

data class LocationDestination(
    @SerializedName("room_number")
    val roomNumber: String?,
    @SerializedName("building_name")
    val buildingName: String?,
    val latitude: Double,
    val longitude: Double,
    val floor: Int?
)

data class NearbyCourses(
    @SerializedName("user_location")
    val userLocation: UserLocation,
    @SerializedName("radius_meters")
    val radiusMeters: Double,
    @SerializedName("total_courses_found")
    val totalCoursesFound: Int,
    val courses: List<CourseLocation>
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

data class FacultyMapMarker(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("building_name")
    val buildingName: String?
)

data class CourseMapMarker(
    val id: Int,
    val name: String,
    @SerializedName("faculty_id")
    val facultyId: Int,
    @SerializedName("room_number")
    val roomNumber: String?,
    @SerializedName("building_name")
    val buildingName: String?,
    val latitude: Double,
    val longitude: Double,
    val floor: Int?
)