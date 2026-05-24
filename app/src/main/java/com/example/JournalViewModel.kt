package com.example

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.JournalEntry
import com.example.database.JournalRepository
import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RecordingState {
    object Idle : RecordingState
    object PermissionRequired : RecordingState
    object Listening : RecordingState
    object Deserializing : RecordingState
    data class Error(val message: String) : RecordingState
}

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: JournalRepository
    val allEntries: StateFlow<List<JournalEntry>>

    private val _decryptedEntries = MutableStateFlow<Map<Int, String>>(emptyMap())
    val decryptedEntries = _decryptedEntries.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState = _recordingState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript = _liveTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = JournalRepository(database.journalDao())
        allEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun toggleDecryption(entry: JournalEntry) {
        val current = _decryptedEntries.value.toMutableMap()
        if (current.containsKey(entry.id)) {
            current.remove(entry.id)
        } else {
            val decrypted = CryptoHelper.decrypt(entry.encryptedText, entry.iv)
            current[entry.id] = decrypted
        }
        _decryptedEntries.value = current
    }

    fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            _recordingState.value = RecordingState.Error("On-device speech recognition is not available on this device.")
            return
        }

        _liveTranscript.value = ""
        _recordingState.value = RecordingState.Listening

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _recordingState.value = RecordingState.Listening
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _recordingState.value = RecordingState.Deserializing
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission required to access microphone"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error (Recognition fallback failed)"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout (Verification failed)"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Speak clearly and try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "System recorder is currently busy"
                            SpeechRecognizer.ERROR_SERVER -> "Android speech recognition error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence detected. Voice input stopped."
                            else -> "Mic recognition issue ($error). Try speaking again."
                        }
                        _recordingState.value = RecordingState.Error(message)
                        cleanup()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: _liveTranscript.value
                        if (text.isNotBlank()) {
                            _liveTranscript.value = text
                        }
                        _recordingState.value = RecordingState.Idle
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _liveTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Could not run on-device speech recognizer: ${e.localizedMessage}")
            cleanup()
        }
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        _recordingState.value = RecordingState.Idle
    }

    fun cancelRecording() {
        speechRecognizer?.cancel()
        _liveTranscript.value = ""
        _recordingState.value = RecordingState.Idle
        cleanup()
    }

    fun saveEntry(category: String, durationSec: Int) {
        val textToSave = _liveTranscript.value
        if (textToSave.isBlank()) {
            _recordingState.value = RecordingState.Error("No transcript collected to save.")
            return
        }

        viewModelScope.launch {
            try {
                val encrypted = CryptoHelper.encrypt(textToSave)
                val entry = JournalEntry(
                    encryptedText = encrypted.cipherText,
                    iv = encrypted.iv,
                    durationSec = durationSec,
                    category = category
                )
                repository.insert(entry)
                _liveTranscript.value = ""
                _recordingState.value = RecordingState.Idle
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error("Failed to encrypt with AES-256: ${e.localizedMessage}")
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteById(entry.id)
            val current = _decryptedEntries.value.toMutableMap()
            current.remove(entry.id)
            _decryptedEntries.value = current
        }
    }

    fun clearError() {
        _recordingState.value = RecordingState.Idle
    }

    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
