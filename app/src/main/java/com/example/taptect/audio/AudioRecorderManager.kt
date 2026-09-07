package com.example.taptect.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages audio recording and emits raw PCM data with throttling to prevent UI saturation.
 */
class AudioRecorderManager {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val circularBufferSize = sampleRate * 1 // 1 second buffer

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val _audioDataFlow = MutableSharedFlow<ShortArray>(replay = 0)
    val audioDataFlow: SharedFlow<ShortArray> = _audioDataFlow

    private val circularBuffer = ShortArray(circularBufferSize)
    private var writePos = 0

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope) {
        if (recordingJob != null) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("TapTect", "AudioRecord failed to initialize")
                return
            }

            audioRecord?.startRecording()
            Log.d("TapTect", "Audio recording started")

            recordingJob = scope.launch(Dispatchers.IO) {
                val readBuffer = ShortArray(bufferSize / 2)
                val emissionBuffer = mutableListOf<Short>()
                var lastEmitTime = System.currentTimeMillis()

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readSize = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (readSize > 0) {
                        // Update circular buffer
                        for (i in 0 until readSize) {
                            val sample = readBuffer[i]
                            circularBuffer[writePos] = sample
                            writePos = (writePos + 1) % circularBufferSize
                            emissionBuffer.add(sample)
                        }

                        // Throttle emissions to ~20Hz (every 50ms) to save UI thread
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastEmitTime >= 50 && emissionBuffer.isNotEmpty()) {
                            _audioDataFlow.emit(emissionBuffer.toShortArray())
                            emissionBuffer.clear()
                            lastEmitTime = currentTime
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TapTect", "Error starting audio recording", e)
        }
    }

    fun stopRecording() {
        Log.d("TapTect", "Stopping audio recording")
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("TapTect", "Error stopping audio recording", e)
        }
        audioRecord = null
    }

    fun captureBuffer(durationMs: Int): ShortArray {
        val size = (sampleRate * durationMs / 1000).coerceAtMost(circularBufferSize)
        val result = ShortArray(size)
        var readPos = (writePos - size + circularBufferSize) % circularBufferSize
        for (i in 0 until size) {
            result[i] = circularBuffer[readPos]
            readPos = (readPos + 1) % circularBufferSize
        }
        return result
    }
}
