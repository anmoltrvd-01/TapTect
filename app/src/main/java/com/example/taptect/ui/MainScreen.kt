package com.example.taptect.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taptect.audio.AudioRecorderManager
import com.example.taptect.audio.FFTProcessor
import com.example.taptect.data.CalibrationRepository
import com.example.taptect.data.SurfaceResult
import com.example.taptect.sensor.TapSensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { AudioRecorderManager() }
    val tapSensorManager = remember { TapSensorManager(context) }
    val fftProcessor = remember { FFTProcessor() }
    val repository = remember { CalibrationRepository() }

    val audioData by audioRecorderManager.audioDataFlow.collectAsState(initial = ShortArray(0))
    var analysisResult by remember { mutableStateOf<FFTProcessor.AnalysisResult?>(null) }
    var surfaceResult by remember { mutableStateOf<SurfaceResult?>(null) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    TapTectPermissionsHandler {
        hasPermissions = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection()

            if (hasPermissions) {
                DisposableEffect(Unit) {
                    var lastImpactTime = 0L
                    audioRecorderManager.startRecording(scope)
                    tapSensorManager.startListening { magnitude ->
                        val now = System.currentTimeMillis()
                        // Debounce: Only process one tap every 500ms
                        if (now - lastImpactTime > 500) {
                            lastImpactTime = now
                            scope.launch {
                                isProcessing = true
                                val buffer = audioRecorderManager.captureBuffer(256)
                                // OFF-LOAD HEAVY CALCULATION TO BACKGROUND
                                val result = withContext(Dispatchers.Default) {
                                    fftProcessor.analyze(buffer, 44100)
                                }
                                analysisResult = result
                                surfaceResult = repository.classifyTap(result.peakFrequency, result.energyDecay)
                                isProcessing = false
                            }
                        }
                    }
                    onDispose {
                        audioRecorderManager.stopRecording()
                        tapSensorManager.stopListening()
                    }
                }

                // Waveform Dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LIVE ACOUSTICS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WaveformVisualizer(
                            audioData = audioData,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(80.dp).fillMaxWidth()
                        )
                    }
                }

                // Result Dashboard
                surfaceResult?.let { result ->
                    MaterialResultCard(result, isProcessing)
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap a surface to calibrate",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Spectrum at the bottom
                analysisResult?.let { result ->
                    FrequencySpectrumVisualizer(
                        magnitudes = result.magnitudes,
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Requesting microphone access...")
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column {
        Text(
            text = "TapTect",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Material Intelligence Engine",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun MaterialResultCard(result: SurfaceResult, isScanning: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isScanning) "ANALYZING..." else "DETECTED MATERIAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = result.material.materialName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Badge(
                    containerColor = if (result.isHollow) Color(0xFFFFB4AB) else Color(0xFFB4E6FF)
                ) {
                    Text(
                        text = if (result.isHollow) "HOLLOW" else "SOLID",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResultMetric("Density", "${result.material.densityScore}%")
                ResultMetric("Confidence", "${(result.confidence * 100).toInt()}%")
            }
        }
    }
}

@Composable
fun ResultMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
