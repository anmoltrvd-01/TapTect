package com.example.taptect.ui

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Utility to handle runtime permissions for Camera and Audio.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TapTectPermissionsHandler(
    onPermissionsGranted: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionsGranted()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }
}
