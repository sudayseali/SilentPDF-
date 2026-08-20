package com.silentpdf.app.ui.viewmodel.controllers

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceRecordingController(private val context: Context, private val coroutineScope: CoroutineScope) {
    private var mediaRecorder: MediaRecorder? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private var recordingFile: File? = null
    private var recordingJob: Job? = null

    fun startVoiceRecording() {
        if (_isRecording.value) return
        try {
            val file = File(context.cacheDir, "audio_note_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            
            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("voice_notes")
            } else {
                context
            }

            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(attributionContext)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            recorder.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            mediaRecorder = recorder
            _isRecording.value = true
            _recordingSeconds.value = 0
            
            recordingJob = coroutineScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingController", "Failed to start audio recording", e)
        }
    }

    fun stopVoiceRecording(onRecordFinished: (File?) -> Unit) {
        if (!_isRecording.value) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingController", "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }
        
        onRecordFinished(recordingFile)
        recordingFile = null
    }
}
