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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taptect.audio.AudioRecorderManager
import com.example.taptect.audio.FFTProcessor
import com.example.taptect.sensor.TapSensorManager
import kotlinx.coroutines.launch

/**
 * Main UI Screen for TapTect.
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { AudioRecorderManager() }
    val tapSensorManager = remember { TapSensorManager(context) }
    val fftProcessor = remember { FFTProcessor() }

    val audioData by audioRecorderManager.audioDataFlow.collectAsState(initial = ShortArray(0))
    var analysisResult by remember { mutableStateOf<FFTProcessor.AnalysisResult?>(null) }
    var hasPermissions by remember { mutableStateOf(value = false) }

    TapTectPermissionsHandler {
        hasPermissions = true
    }

    if (hasPermissions) {
        DisposableEffect(Unit) {
            audioRecorderManager.startRecording(scope)
            tapSensorManager.startListening { magnitude ->
                // Impact detected! Capture 256ms of audio and process
                scope.launch {
                    val buffer = audioRecorderManager.captureBuffer(256)
                    analysisResult = fftProcessor.analyze(buffer, 44100)
                }
            }
            onDispose {
                audioRecorderManager.stopRecording()
                tapSensorManager.stopListening()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "TapTect Analysis",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Real-time Waveform", style = MaterialTheme.typography.titleSmall)
            WaveformVisualizer(
                audioData = audioData,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            analysisResult?.let { result ->
                Text(text = "Last Tap Analysis", style = MaterialTheme.typography.titleMedium)
                Text(text = "Peak Frequency: ${result.peakFrequency.toInt()} Hz")
                Text(text = "Energy Decay: ${"%.2f".format(result.energyDecay)}")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FrequencySpectrumVisualizer(
                    magnitudes = result.magnitudes,
                    modifier = Modifier.height(120.dp)
                )
            } ?: run {
                Text(
                    text = "Tap a surface to see frequency analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    } else {
        Text(
            text = "Permissions required to use the audio visualizer.",
            modifier = Modifier.padding(16.dp)
        )
    }
}
