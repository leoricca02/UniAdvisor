package com.riccaturrini.uniadvisor.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberShakeDetector(onShakeDetected: () -> Unit): ShakeDetector {
    val context = LocalContext.current
    val shakeDetector = remember {
        ShakeDetector(context, onShakeDetected)
    }

    DisposableEffect(Unit) {
        shakeDetector.start()
        onDispose {
            shakeDetector.stop()
        }
    }

    return shakeDetector
}