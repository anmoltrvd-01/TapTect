package com.example.taptect.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages audio recording and emits raw PCM data.
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

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()

        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ShortArray(bufferSize / 2)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSize = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                if (readSize > 0) {
                    val actualData = readBuffer.copyOf(readSize)
                    
                    // Update circular buffer
                    for (s in actualData) {
                        circularBuffer[writePos] = s
                        writePos = (writePos + 1) % circularBufferSize
                    }
                    
                    _audioDataFlow.emit(actualData)
                }
            }
        }
    }

    /**
     * Captures a specific duration of audio from the circular buffer.
     * @param durationMs Duration in milliseconds.
     */
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

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
    }
}
