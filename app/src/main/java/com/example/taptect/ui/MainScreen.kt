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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    val hapticFeedback = remember { HapticFeedback(context) }

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                                hapticFeedback.triggerClick()
                                isProcessing = true
                                val result = withContext(Dispatchers.Default) {
                                    val filtered = noiseFilter.process(buffer)
                                    fftProcessor.analyze(filtered, 44100)
                                }
                                analysisResult = result
                                val finalResult = repository.classifyTap(result.peakFrequency, result.energyDecay)
                                surfaceResult = finalResult
                                hapticFeedback.triggerSuccess()
                                isProcessing = false
                            }
                        }
                    }
                    onDispose {
                        audioRecorderManager.stopRecording()
                        tapSensorManager.stopListening()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RadarTarget(isImpacted = isProcessing)
                    
                    if (!isProcessing && surfaceResult == null) {
                        Text(
                            "READY FOR SCAN",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 220.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ACOUSTIC SIGNATURE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        WaveformVisualizer(
                            audioData = audioData,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(60.dp).fillMaxWidth()
                        )
                    }
                }

                surfaceResult?.let { result ->
                    MaterialResultCard(result, isProcessing)
                }

                analysisResult?.let { result ->
                    FrequencySpectrumVisualizer(
                        magnitudes = result.magnitudes,
                        modifier = Modifier.height(80.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
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
    val densityProgress by animateFloatAsState(
        targetValue = result.material.densityScore / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "DensityProgress"
    )

    val densityColor by animateColorAsState(
        targetValue = when {
            result.material.densityScore < 40 -> Color(0xFFFF9800) // Orange (Hollow)
            result.material.densityScore < 70 -> Color(0xFF00BCD4) // Cyan (Medium)
            else -> Color(0xFF3F51B5) // Deep Blue (High)
        },
        animationSpec = tween(1000),
        label = "DensityColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isScanning) "ANALYZING..." else "MATERIAL IDENTIFIED",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        result.material.materialName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Badge(containerColor = if (result.isHollow) Color(0xFFFFB4AB) else Color(0xFFB4E6FF)) {
                    Text(
                        if (result.isHollow) "HOLLOW" else "SOLID",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DENSITY METER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("${result.material.densityScore}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { densityProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = densityColor,
                    trackColor = densityColor.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ResultMetric("Confidence", "${(result.confidence * 100).toInt()}%")
                ResultMetric("Frequency", "${result.material.targetFrequencyRange.first}Hz+")
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
