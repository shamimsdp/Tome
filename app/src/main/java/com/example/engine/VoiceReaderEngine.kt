package com.example.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceReaderEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingPlayOnReady = false

    var onPageCompleted: (() -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private val _currentSentenceText = MutableStateFlow("")
    val currentSentenceText: StateFlow<String> = _currentSentenceText.asStateFlow()

    private val _totalSentences = MutableStateFlow(0)
    val totalSentences: StateFlow<Int> = _totalSentences.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackVolume = MutableStateFlow(1.0f)
    val playbackVolume: StateFlow<Float> = _playbackVolume.asStateFlow()

    private var sentences: List<String> = emptyList()

    private val prefs = context.getSharedPreferences("tome_voice_reader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_TTS_SPEED = "tts_playback_speed"
        private const val PREF_TTS_VOLUME = "tts_playback_volume"
    }

    init {
        val savedSpeed = prefs.getFloat(PREF_TTS_SPEED, 1.0f)
        val savedVolume = prefs.getFloat(PREF_TTS_VOLUME, 1.0f)
        _playbackSpeed.value = savedSpeed.coerceIn(0.5f, 3.0f)
        _playbackVolume.value = savedVolume.coerceIn(0.0f, 1.0f)
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val locale = Locale.getDefault().takeIf {
                    engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
                } ?: Locale.US

                val result = engine.setLanguage(locale)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                    engine.setSpeechRate(_playbackSpeed.value)
                    setupUtteranceListener()

                    if (pendingPlayOnReady && sentences.isNotEmpty()) {
                        pendingPlayOnReady = false
                        play()
                    }
                }
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                val nextIndex = _currentSentenceIndex.value + 1
                if (nextIndex < sentences.size && _isPlaying.value) {
                    _currentSentenceIndex.value = nextIndex
                    speakCurrentSentence()
                } else if (_isPlaying.value) {
                    _isPlaying.value = false
                    // Notify that current page's reading is complete
                    onPageCompleted?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isPlaying.value = false
            }
        })
    }

    fun loadText(rawText: String) {
        stop()
        val cleaned = rawText.replace("\r", " ").replace("\n", " ").trim()
        val parsed = cleaned.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        sentences = if (parsed.isNotEmpty()) parsed else if (cleaned.isNotBlank()) listOf(cleaned) else emptyList()
        _totalSentences.value = sentences.size
        _currentSentenceIndex.value = 0
        _currentSentenceText.value = sentences.firstOrNull() ?: ""
    }

    fun play() {
        if (sentences.isEmpty()) return
        if (!isInitialized) {
            pendingPlayOnReady = true
            return
        }
        _isPlaying.value = true
        speakCurrentSentence()
    }

    fun pause() {
        _isPlaying.value = false
        pendingPlayOnReady = false
        tts?.stop()
    }

    fun stop() {
        _isPlaying.value = false
        pendingPlayOnReady = false
        _currentSentenceIndex.value = 0
        _currentSentenceText.value = sentences.firstOrNull() ?: ""
        tts?.stop()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(index: Int) {
        if (sentences.isEmpty()) return
        val clamped = index.coerceIn(0, (sentences.size - 1).coerceAtLeast(0))
        _currentSentenceIndex.value = clamped
        if (_isPlaying.value) {
            speakCurrentSentence()
        } else {
            _currentSentenceText.value = sentences.getOrNull(clamped) ?: ""
        }
    }

    fun skipForward() {
        if (sentences.isEmpty()) return
        val next = (_currentSentenceIndex.value + 1).coerceAtMost(sentences.size - 1)
        _currentSentenceIndex.value = next
        if (_isPlaying.value) {
            speakCurrentSentence()
        } else {
            _currentSentenceText.value = sentences.getOrNull(next) ?: ""
        }
    }

    fun skipBackward() {
        if (sentences.isEmpty()) return
        val prev = (_currentSentenceIndex.value - 1).coerceAtLeast(0)
        _currentSentenceIndex.value = prev
        if (_isPlaying.value) {
            speakCurrentSentence()
        } else {
            _currentSentenceText.value = sentences.getOrNull(prev) ?: ""
        }
    }

    fun cycleSpeed(): Float {
        val nextSpeed = when (_playbackSpeed.value) {
            0.75f -> 1.0f
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 0.75f
        }
        setPlaybackSpeed(nextSpeed)
        return nextSpeed
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 3.0f)
        _playbackSpeed.value = clamped
        prefs.edit().putFloat(PREF_TTS_SPEED, clamped).apply()
        tts?.setSpeechRate(clamped)
        if (_isPlaying.value) {
            speakCurrentSentence()
        }
    }

    fun setPlaybackVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _playbackVolume.value = clamped
        prefs.edit().putFloat(PREF_TTS_VOLUME, clamped).apply()
        if (_isPlaying.value) {
            speakCurrentSentence()
        }
    }

    private fun speakCurrentSentence() {
        val index = _currentSentenceIndex.value
        val textToSpeak = sentences.getOrNull(index) ?: return
        _currentSentenceText.value = textToSpeak

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_$index")
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _playbackVolume.value)
        }
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "sentence_$index")
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        pendingPlayOnReady = false
    }
}
