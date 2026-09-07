package com.example.taptect.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taptect.audio.AudioRecorderManager

/**
 * Main UI Screen for TapTect.
 */
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { AudioRecorderManager() }
    val audioData by audioRecorderManager.audioDataFlow.collectAsState(initial = ShortArray(0))
    var hasPermissions by remember { mutableStateOf(value = false) }

    TapTectPermissionsHandler {
        hasPermissions = true
    }

    if (hasPermissions) {
        DisposableEffect(Unit) {
            audioRecorderManager.startRecording(scope)
            onDispose {
                audioRecorderManager.stopRecording()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Audio Visualizer",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            WaveformVisualizer(
                audioData = audioData,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Text(
            text = "Permissions required to use the audio visualizer.",
            modifier = Modifier.padding(16.dp)
        )
    }
}
