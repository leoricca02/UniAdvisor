package com.riccaturrini.uniadvisor

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UniAdvisorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Observe app lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        // App moved to background - logout
                        Firebase.auth.signOut()
                        android.util.Log.d("UniAdvisorApp", "App moved to background - User logged out")
                    }
                    Lifecycle.Event.ON_START -> {
                        // App moved to foreground
                        android.util.Log.d("UniAdvisorApp", "App moved to foreground")
                    }
                    else -> {}
                }
            }
        )
    }
}