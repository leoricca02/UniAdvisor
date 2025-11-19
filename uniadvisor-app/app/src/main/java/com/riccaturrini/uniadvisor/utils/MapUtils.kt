package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens Google Maps with the specified coordinates
 */
fun openGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String? = null) {
    val uri = if (label != null) {
        // With label
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
    } else {
        // Without label
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }

    // If Google Maps is not installed, open in browser
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}