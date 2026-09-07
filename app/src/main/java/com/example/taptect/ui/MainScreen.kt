package com.example.taptect.ui

import android.util.Log
import androidx.compose.animation.*
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
import com.example.taptect.audio.*
import com.example.taptect.data.CalibrationRepository
import com.example.taptect.data.SurfaceResult
import com.example.taptect.sensor.TapSensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val audioRecorderManager = remember { AudioRecorderManager() }
    val tapSensorManager = remember { TapSensorManager(context) }
    val fftProcessor = remember { FFTProcessor() }
    val noiseFilter = remember { NoiseFilter() }
    val repository = remember { CalibrationRepository() }
    val calibrator = remember { AmbientNoiseCalibrator(audioRecorderManager) }

    val audioData by audioRecorderManager.audioDataFlow.collectAsState(initial = ShortArray(0))
    var analysisResult by remember { mutableStateOf<FFTProcessor.AnalysisResult?>(null) }
    var surfaceResult by remember { mutableStateOf<SurfaceResult?>(null) }
    
    var hasPermissions by remember { mutableStateOf(false) }
    var isCalibrating by remember { mutableStateOf(false) }
    var audioThreshold by remember { mutableStateOf(800f) }
    var isProcessing by remember { mutableStateOf(false) }

    TapTectPermissionsHandler {
        hasPermissions = true
    }

    // Auto-calibration on launch
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            isCalibrating = true
            audioRecorderManager.startRecording(this)
            calibrator.startCalibration(this) { threshold ->
                audioThreshold = threshold
                isCalibrating = false
                Log.d("TapTect", "Calibration complete. Audio threshold: $threshold")
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection()

            if (isCalibrating) {
                CalibrationLoadingView()
            } else if (hasPermissions) {
                DisposableEffect(Unit) {
                    tapSensorManager.startListening { magnitude ->
                        scope.launch {
                            val buffer = audioRecorderManager.captureBuffer(256)
                            val rms = calculateRMS(buffer)
                            
                            // DUAL TRIGGER: Accelerometer magnitude AND Audio amplitude
                            if (rms > audioThreshold) {
                                isProcessing = true
                                val result = withContext(Dispatchers.Default) {
                                    val filtered = noiseFilter.process(buffer)
                                    fftProcessor.analyze(filtered, 44100)
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LIVE ACOUSTICS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        WaveformVisualizer(
                            audioData = audioData,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(80.dp).fillMaxWidth()
                        )
                    }
                }

                surfaceResult?.let { result ->
                    MaterialResultCard(result, isProcessing)
                } ?: run {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Tap a surface to analyze", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }

                analysisResult?.let { result ->
                    FrequencySpectrumVisualizer(
                        magnitudes = result.magnitudes,
                        modifier = Modifier.height(100.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

@Composable
fun CalibrationLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Calibrating ambient noise floor...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun calculateRMS(data: ShortArray): Float {
    if (data.isEmpty()) return 0f
    var sum = 0.0
    for (s in data) sum += (s.toInt() * s.toInt()).toDouble()
    return sqrt(sum / data.size).toFloat()
}

@Composable
fun HeaderSection() {
    Column {
        Text("TapTect", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Material Intelligence Engine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun MaterialResultCard(result: SurfaceResult, isScanning: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isScanning) "ANALYZING..." else "DETECTED MATERIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(result.material.materialName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Badge(containerColor = if (result.isHollow) Color(0xFFFFB4AB) else Color(0xFFB4E6FF)) {
                    Text(if (result.isHollow) "HOLLOW" else "SOLID", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
