package com.example.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Representation of a selectable TTS voice or voice persona.
 */
data class TtsVoiceOption(
    val id: String,
    val name: String,
    val displayName: String,
    val locale: Locale,
    val languageDisplay: String,
    val isBangla: Boolean,
    val isNetwork: Boolean = false,
    val presetPitch: Float = 1.0f,
    val presetSpeed: Float = 1.0f,
    val gender: String = "Neutral" // "Female", "Male", "Neutral"
)

data class TtsEngineOption(
    val packageName: String,
    val label: String,
    val isCurrent: Boolean
)

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

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    private val _availableEngines = MutableStateFlow<List<TtsEngineOption>>(emptyList())
    val availableEngines: StateFlow<List<TtsEngineOption>> = _availableEngines.asStateFlow()

    private val _selectedEnginePackage = MutableStateFlow<String?>(null)
    val selectedEnginePackage: StateFlow<String?> = _selectedEnginePackage.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _selectedLanguageMode = MutableStateFlow("AUTO") // AUTO, BANGLA, ENGLISH, DEFAULT
    val selectedLanguageMode: StateFlow<String> = _selectedLanguageMode.asStateFlow()

    private val _isReadingBangla = MutableStateFlow(false)
    val isReadingBangla: StateFlow<Boolean> = _isReadingBangla.asStateFlow()

    private val _isBanglaSupportedOnDevice = MutableStateFlow(false)
    val isBanglaSupportedOnDevice: StateFlow<Boolean> = _isBanglaSupportedOnDevice.asStateFlow()

    private var sentences: List<String> = emptyList()

    private val prefs = context.getSharedPreferences("tome_voice_reader_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_TTS_SPEED = "tts_playback_speed"
        private const val PREF_TTS_VOLUME = "tts_playback_volume"
        private const val PREF_TTS_PITCH = "tts_playback_pitch"
        private const val PREF_TTS_VOICE_NAME = "tts_voice_name"
        private const val PREF_TTS_LANG_MODE = "tts_language_mode"
        private const val PREF_TTS_ENGINE_PKG = "tts_engine_package"

        val BANGLA_LOCALES = listOf(
            Locale("bn", "BD"),
            Locale("bn", "IN"),
            Locale("bn")
        )

        fun isBanglaText(text: String): Boolean {
            return text.any { it in '\u0980'..'\u09FF' }
        }

        // Standard curated presets to ensure users always have distinct voice personas
        val CURATED_PRESETS = listOf(
            TtsVoiceOption(
                id = "bn_female_afrin",
                name = "bn_female_afrin",
                displayName = "আফরিন (Afrin) • বাংলা নারী (Natural Female)",
                locale = Locale("bn", "BD"),
                languageDisplay = "বাংলা (Bangladesh)",
                isBangla = true,
                presetPitch = 1.35f,
                presetSpeed = 0.95f,
                gender = "Female"
            ),
            TtsVoiceOption(
                id = "bn_male_tanvir",
                name = "bn_male_tanvir",
                displayName = "তানভীর (Tanvir) • বাংলা পুরুষ (Deep Male)",
                locale = Locale("bn", "BD"),
                languageDisplay = "বাংলা (Bangladesh)",
                isBangla = true,
                presetPitch = 0.82f,
                presetSpeed = 0.92f,
                gender = "Male"
            ),
            TtsVoiceOption(
                id = "bn_scholar_tagore",
                name = "bn_scholar_tagore",
                displayName = "রবীন্দ্রনাথ কথক (Tagore Scholar) • গম্ভীর পাঠ",
                locale = Locale("bn", "IN"),
                languageDisplay = "বাংলা (Kolkata)",
                isBangla = true,
                presetPitch = 0.78f,
                presetSpeed = 0.88f,
                gender = "Male"
            ),
            TtsVoiceOption(
                id = "bn_storyteller_shayla",
                name = "bn_storyteller_shayla",
                displayName = "শায়লা (Shayla) • শান্ত গল্পপাঠক (Storyteller)",
                locale = Locale("bn", "BD"),
                languageDisplay = "বাংলা (Bangladesh)",
                isBangla = true,
                presetPitch = 1.30f,
                presetSpeed = 0.90f,
                gender = "Female"
            ),
            TtsVoiceOption(
                id = "en_female_emma",
                name = "en_female_emma",
                displayName = "Emma • Natural & Warm (US Female)",
                locale = Locale.US,
                languageDisplay = "English (US)",
                isBangla = false,
                presetPitch = 1.35f,
                presetSpeed = 1.0f,
                gender = "Female"
            ),
            TtsVoiceOption(
                id = "en_male_james",
                name = "en_male_james",
                displayName = "James • Deep & Resonant (US Male)",
                locale = Locale.US,
                languageDisplay = "English (US)",
                isBangla = false,
                presetPitch = 0.82f,
                presetSpeed = 0.95f,
                gender = "Male"
            ),
            TtsVoiceOption(
                id = "en_scholar_oliver",
                name = "en_scholar_oliver",
                displayName = "Oliver • British Oxford Scholar (UK)",
                locale = Locale.UK,
                languageDisplay = "English (UK)",
                isBangla = false,
                presetPitch = 0.85f,
                presetSpeed = 0.92f,
                gender = "Male"
            ),
            TtsVoiceOption(
                id = "en_speed_maya",
                name = "en_speed_maya",
                displayName = "Maya • Crisp Academic Reader",
                locale = Locale.US,
                languageDisplay = "English (US)",
                isBangla = false,
                presetPitch = 1.32f,
                presetSpeed = 1.25f,
                gender = "Female"
            )
        )

        fun detectVoiceGender(voice: Voice): String {
            // 1. Inspect engine features
            voice.features?.forEach { feature ->
                val fLower = feature.lowercase()
                if (fLower.contains("female") || fLower.contains("gender=female") || fLower.contains("gender:female")) {
                    return "Female"
                }
                if (fLower.contains("gender=male") || fLower.contains("gender:male")) {
                    return "Male"
                }
            }

            val nameLower = voice.name.lowercase()

            // 2. Inspect voice name (female patterns must be checked BEFORE male!)
            if (nameLower.contains("female") ||
                nameLower.contains("#fem") ||
                nameLower.contains("-fem") ||
                nameLower.contains("_fem") ||
                nameLower.contains("woman") ||
                nameLower.contains("girl")
            ) {
                return "Female"
            }

            if (nameLower.contains("male") ||
                nameLower.contains("man") ||
                nameLower.contains("boy")
            ) {
                return "Male"
            }

            return "Neutral"
        }

        fun formatVoiceDisplayName(voice: Voice, detectedGender: String): String {
            val rawName = voice.name
            val langName = voice.locale.displayLanguage.ifBlank { voice.locale.language }
            val countryName = voice.locale.displayCountry.ifBlank { "" }
            val localeDisplay = if (countryName.isNotBlank()) "$langName ($countryName)" else langName

            // Extract voice number or code from patterns like #female_1, #male_2, -1, _2
            val indexMatch = Regex("""(?:female|male)[-_](\d+)""", RegexOption.IGNORE_CASE).find(rawName)
                ?: Regex("""[-_](\d+)(?:[-_]|$)""").find(rawName)
            val indexStr = indexMatch?.groupValues?.getOrNull(1)?.let { " #$it" } ?: ""

            val genderTag = when (detectedGender) {
                "Female" -> "Female"
                "Male" -> "Male"
                else -> ""
            }

            val personaName = if (genderTag.isNotEmpty()) {
                "$localeDisplay Voice$indexStr ($genderTag)"
            } else {
                "$localeDisplay Voice$indexStr"
            }

            return "$personaName • $localeDisplay"
        }

        /**
         * Converts Bengali script into Romanized phonetic text so that if a device
         * lacks the Google TTS Bangla voice pack, the synthesizer still reads out
         * the Bengali words clearly and phonetically rather than staying silent.
         */
        fun transliterateBanglaToPhonetic(banglaText: String): String {
            val sb = StringBuilder()
            var i = 0
            val len = banglaText.length

            while (i < len) {
                val c = banglaText[i]
                when (c) {
                    // Vowels
                    'অ' -> sb.append("o")
                    'আ' -> sb.append("aa")
                    'ই' -> sb.append("i")
                    'ঈ' -> sb.append("ee")
                    'উ' -> sb.append("u")
                    'ঊ' -> sb.append("oo")
                    'ঋ' -> sb.append("ri")
                    'এ' -> sb.append("e")
                    'ঐ' -> sb.append("oi")
                    'ও' -> sb.append("o")
                    'ঔ' -> sb.append("ou")

                    // Consonants
                    'ক' -> sb.append("k")
                    'খ' -> sb.append("kh")
                    'গ' -> sb.append("g")
                    'ঘ' -> sb.append("gh")
                    'ঙ' -> sb.append("ng")
                    'চ' -> sb.append("ch")
                    'ছ' -> sb.append("chh")
                    'জ' -> sb.append("j")
                    'ঝ' -> sb.append("jh")
                    'ঞ' -> sb.append("n")
                    'ট' -> sb.append("t")
                    'ঠ' -> sb.append("th")
                    'ড' -> sb.append("d")
                    'ঢ' -> sb.append("dh")
                    'ণ' -> sb.append("n")
                    'ত' -> sb.append("t")
                    'থ' -> sb.append("th")
                    'দ' -> sb.append("d")
                    'ধ' -> sb.append("dh")
                    'ন' -> sb.append("n")
                    'প' -> sb.append("p")
                    'ফ' -> sb.append("f")
                    'ব' -> sb.append("b")
                    'ভ' -> sb.append("bh")
                    'ম' -> sb.append("m")
                    'য' -> sb.append("j")
                    'র' -> sb.append("r")
                    'ল' -> sb.append("l")
                    'শ' -> sb.append("sh")
                    'ষ' -> sb.append("sh")
                    'স' -> sb.append("s")
                    'হ' -> sb.append("h")
                    '\u09DC' -> sb.append("r") // ড়
                    '\u09DD' -> sb.append("rh") // ঢ়
                    '\u09DF' -> sb.append("y") // য়
                    'ৎ' -> sb.append("t")
                    'ং' -> sb.append("ng")
                    'ঃ' -> sb.append("h")
                    'ঁ' -> sb.append("n")

                    // Vowel signs (kar)
                    'া' -> sb.append("aa")
                    'ি' -> sb.append("i")
                    'ী' -> sb.append("ee")
                    'ু' -> sb.append("u")
                    'ূ' -> sb.append("oo")
                    'ৃ' -> sb.append("ri")
                    'ে' -> sb.append("e")
                    'ৈ' -> sb.append("oi")
                    'ো' -> sb.append("o")
                    'ৌ' -> sb.append("ou")
                    '্' -> {} // Hasanta suppresses vowel

                    // Punctuation
                    '।', '॥' -> sb.append(". ")
                    '\u200C', '\u200D' -> {} // zero-width non-joiner / joiner ignore
                    else -> sb.append(c)
                }
                i++
            }
            return sb.toString()
        }
    }

    init {
        val savedSpeed = prefs.getFloat(PREF_TTS_SPEED, 1.0f)
        val savedVolume = prefs.getFloat(PREF_TTS_VOLUME, 1.0f)
        val savedPitch = prefs.getFloat(PREF_TTS_PITCH, 1.0f)
        val savedEngine = prefs.getString(PREF_TTS_ENGINE_PKG, null)
        _playbackSpeed.value = savedSpeed.coerceIn(0.5f, 3.0f)
        _playbackVolume.value = savedVolume.coerceIn(0.0f, 1.0f)
        _playbackPitch.value = savedPitch.coerceIn(0.5f, 2.0f)
        _selectedVoiceName.value = prefs.getString(PREF_TTS_VOICE_NAME, null)
        _selectedLanguageMode.value = prefs.getString(PREF_TTS_LANG_MODE, "AUTO") ?: "AUTO"
        _selectedEnginePackage.value = savedEngine

        // Initialize with default curated presets
        _availableVoices.value = CURATED_PRESETS

        initTts()
    }

    fun initTts() {
        val savedEngine = _selectedEnginePackage.value
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        isInitialized = false

        tts = if (!savedEngine.isNullOrBlank()) {
            try {
                TextToSpeech(context, this, savedEngine)
            } catch (_: Exception) {
                TextToSpeech(context, this)
            }
        } else {
            TextToSpeech(context, this)
        }

        // Immediately discover installed TTS engines
        refreshAvailableEngines()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                isInitialized = true

                try {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    engine.setAudioAttributes(audioAttributes)
                } catch (_: Exception) {}

                engine.setSpeechRate(_playbackSpeed.value)
                engine.setPitch(_playbackPitch.value)
                setupUtteranceListener()

                // Query and refresh available TTS engines
                refreshAvailableEngines()

                // Check Bangla availability on this engine
                val banglaAvail = BANGLA_LOCALES.any {
                    try {
                        engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
                    } catch (_: Exception) {
                        false
                    }
                }
                _isBanglaSupportedOnDevice.value = banglaAvail

                // Refresh combined voices (curated personas + system voices)
                refreshAvailableVoices()

                // Apply saved voice if available
                applyConfiguredVoice()

                if (pendingPlayOnReady && sentences.isNotEmpty()) {
                    pendingPlayOnReady = false
                    play()
                }
            }
        } else {
            isInitialized = false
            // Auto-recovery: if specific engine failed, fall back to default
            if (_selectedEnginePackage.value != null) {
                _selectedEnginePackage.value = null
                prefs.edit().remove(PREF_TTS_ENGINE_PKG).apply()
                try {
                    tts?.shutdown()
                    tts = TextToSpeech(context, this)
                } catch (_: Exception) {}
            }
        }
    }

    fun refreshAvailableEngines() {
        try {
            val list = mutableListOf<TtsEngineOption>()
            val currentPkg = _selectedEnginePackage.value ?: tts?.defaultEngine

            // 1. Check engine.engines from active TTS instance
            tts?.engines?.forEach { info ->
                list.add(
                    TtsEngineOption(
                        packageName = info.name,
                        label = info.label.ifBlank { info.name.substringAfterLast(".") },
                        isCurrent = info.name == currentPkg
                    )
                )
            }

            // 2. Query PackageManager for all installed TTS engine services
            try {
                val pm = context.packageManager
                val intent = android.content.Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
                val resolveInfos = pm.queryIntentServices(intent, 0)
                for (resolveInfo in resolveInfos) {
                    val serviceInfo = resolveInfo.serviceInfo ?: continue
                    val pkgName = serviceInfo.packageName
                    if (list.none { it.packageName == pkgName }) {
                        val label = resolveInfo.loadLabel(pm)?.toString() ?: pkgName.substringAfterLast(".")
                        list.add(
                            TtsEngineOption(
                                packageName = pkgName,
                                label = label,
                                isCurrent = pkgName == currentPkg
                            )
                        )
                    }
                }
            } catch (_: Exception) {}

            // 3. Fallback standard options if none found (e.g. Google Speech Services)
            if (list.isEmpty()) {
                list.add(
                    TtsEngineOption(
                        packageName = "com.google.android.tts",
                        label = "Google Speech Services (Google TTS)",
                        isCurrent = true
                    )
                )
                list.add(
                    TtsEngineOption(
                        packageName = "default",
                        label = "System Default Speech Engine",
                        isCurrent = false
                    )
                )
            }

            _availableEngines.value = list
        } catch (_: Exception) {
            _availableEngines.value = listOf(
                TtsEngineOption(
                    packageName = "com.google.android.tts",
                    label = "Google Speech Services (Google TTS)",
                    isCurrent = true
                )
            )
        }
    }

    fun switchTtsEngine(packageName: String) {
        if (_selectedEnginePackage.value == packageName) return
        _selectedEnginePackage.value = packageName
        prefs.edit().putString(PREF_TTS_ENGINE_PKG, packageName).apply()

        isInitialized = false
        tts?.shutdown()
        tts = try {
            TextToSpeech(context.applicationContext, this, packageName)
        } catch (_: Exception) {
            TextToSpeech(context.applicationContext, this)
        }
    }

    private fun refreshAvailableVoices() {
        val engine = tts ?: return
        try {
            val systemVoices = engine.voices?.map { voice ->
                val isBangla = voice.locale.language.equals("bn", ignoreCase = true)
                val langName = voice.locale.displayLanguage.ifBlank { voice.locale.language }
                val countryName = voice.locale.displayCountry.ifBlank { "" }
                val fullDisplay = if (countryName.isNotBlank()) "$langName ($countryName)" else langName
                val detectedGender = detectVoiceGender(voice)
                val formattedDisplay = formatVoiceDisplayName(voice, detectedGender)

                TtsVoiceOption(
                    id = voice.name,
                    name = voice.name,
                    displayName = formattedDisplay,
                    locale = voice.locale,
                    languageDisplay = fullDisplay,
                    isBangla = isBangla,
                    isNetwork = voice.isNetworkConnectionRequired,
                    presetPitch = if (detectedGender == "Female") 1.25f else if (detectedGender == "Male") 0.85f else 1.0f,
                    gender = detectedGender
                )
            } ?: emptyList()

            // Combine curated personas with system voices (avoiding duplicate names)
            val combined = (CURATED_PRESETS + systemVoices).distinctBy { it.name }
            _availableVoices.value = combined
        } catch (e: Exception) {
            e.printStackTrace()
            _availableVoices.value = CURATED_PRESETS
        }
    }

    private fun findMatchingVoiceForGender(locale: Locale, targetGender: String): Voice? {
        val voices = tts?.voices ?: return null
        // 1. Language + Gender match
        val langGenderMatch = voices.firstOrNull { v ->
            v.locale.language.equals(locale.language, ignoreCase = true) &&
                detectVoiceGender(v) == targetGender
        }
        if (langGenderMatch != null) return langGenderMatch

        // 2. Any voice in target gender
        return voices.firstOrNull { v ->
            detectVoiceGender(v) == targetGender
        }
    }

    /**
     * Applies voice, locale, pitch, and speed in proper order:
     * 1. Set language FIRST (so setLanguage never resets our selected voice!)
     * 2. Set exact voice or matching gender voice SECOND
     * 3. Set pitch and rate THIRD to guarantee genuine female/male vocal timbre
     */
    private fun applyVoiceConfiguration(targetLocale: Locale, isBangla: Boolean) {
        val engine = tts ?: return

        // 1. Set language first (safely checking availability)
        try {
            val status = engine.isLanguageAvailable(targetLocale)
            if (status >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = targetLocale
            } else {
                engine.language = Locale.US
            }
        } catch (_: Exception) {
            try { engine.language = Locale.US } catch (_: Exception) {}
        }

        val chosenVoice = _selectedVoiceName.value
        val preset = CURATED_PRESETS.find { it.name == chosenVoice }

        if (preset != null) {
            val targetGender = preset.gender
            val sysMatchingVoice = findMatchingVoiceForGender(preset.locale, targetGender)
            if (sysMatchingVoice != null) {
                try {
                    engine.voice = sysMatchingVoice
                } catch (_: Exception) {}
            }

            // Determine pitch to guarantee audible female or male tone
            val effectivePitch = when (targetGender) {
                "Female" -> {
                    // If a true female system voice is bound, 1.22x; if synthesized on base male voice, 1.38x
                    val factor = if (sysMatchingVoice != null) 1.22f else 1.38f
                    (factor * _playbackPitch.value).coerceIn(0.6f, 2.0f)
                }
                "Male" -> (0.85f * _playbackPitch.value).coerceIn(0.5f, 2.0f)
                else -> _playbackPitch.value
            }

            engine.setPitch(effectivePitch)
            engine.setSpeechRate((preset.presetSpeed * _playbackSpeed.value).coerceIn(0.5f, 3.0f))
            return
        }

        // Specific system voice
        val sysVoice = engine.voices?.find { it.name == chosenVoice }
        if (sysVoice != null) {
            try {
                engine.voice = sysVoice
            } catch (_: Exception) {}

            val gender = detectVoiceGender(sysVoice)
            val effectivePitch = when (gender) {
                "Female" -> (1.28f * _playbackPitch.value).coerceIn(0.6f, 2.0f)
                "Male" -> (0.85f * _playbackPitch.value).coerceIn(0.5f, 2.0f)
                else -> _playbackPitch.value
            }
            engine.setPitch(effectivePitch)
            engine.setSpeechRate(_playbackSpeed.value)
            return
        }

        // Default auto voice
        if (isBangla) {
            val banglaVoice = engine.voices?.find { it.locale.language.equals("bn", ignoreCase = true) }
            if (banglaVoice != null) {
                try {
                    engine.voice = banglaVoice
                } catch (_: Exception) {}
            }
        }
        engine.setPitch(_playbackPitch.value)
        engine.setSpeechRate(_playbackSpeed.value)
    }

    private fun applyConfiguredVoice() {
        val defLocale = if (_selectedLanguageMode.value == "BANGLA" || _isReadingBangla.value) {
            BANGLA_LOCALES.first()
        } else {
            Locale.getDefault()
        }
        applyVoiceConfiguration(defLocale, _isReadingBangla.value)
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

    fun loadText(rawText: String, fallbackTitle: String = "Document", pageNumber: Int = 1) {
        stop()
        val cleaned = rawText.replace("\r", " ").replace("\n", " ").trim()
        // Support Latin (. ! ?) and Bengali sentence end marks: Dari (।) and double dari (॥)
        val parsed = cleaned.split(Regex("(?<=[.!?।॥])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        sentences = if (parsed.isNotEmpty()) {
            parsed
        } else if (cleaned.isNotBlank()) {
            listOf(cleaned)
        } else {
            listOf("$fallbackTitle. Page $pageNumber.")
        }
        _totalSentences.value = sentences.size
        _currentSentenceIndex.value = 0
        _currentSentenceText.value = sentences.firstOrNull() ?: ""
    }

    fun play() {
        if (sentences.isEmpty()) {
            sentences = listOf("Document. Page 1.")
            _totalSentences.value = 1
            _currentSentenceIndex.value = 0
            _currentSentenceText.value = sentences.first()
        }
        if (!isInitialized || tts == null) {
            pendingPlayOnReady = true
            initTts()
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

    fun setPlaybackPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _playbackPitch.value = clamped
        prefs.edit().putFloat(PREF_TTS_PITCH, clamped).apply()
        tts?.setPitch(clamped)
        if (_isPlaying.value) {
            speakCurrentSentence()
        }
    }

    fun setVoiceByName(voiceName: String?) {
        _selectedVoiceName.value = voiceName
        prefs.edit().putString(PREF_TTS_VOICE_NAME, voiceName).apply()

        val defLocale = if (_selectedLanguageMode.value == "BANGLA" || _isReadingBangla.value) {
            BANGLA_LOCALES.first()
        } else {
            Locale.getDefault()
        }
        applyVoiceConfiguration(defLocale, _isReadingBangla.value)

        if (_isPlaying.value) {
            speakCurrentSentence()
        }
    }

    fun setLanguageMode(mode: String) {
        _selectedLanguageMode.value = mode
        prefs.edit().putString(PREF_TTS_LANG_MODE, mode).apply()
        if (_isPlaying.value) {
            speakCurrentSentence()
        }
    }

    fun testVoice(sampleText: String? = null, isBangla: Boolean = false) {
        val engine = tts ?: return
        val rawText = sampleText ?: if (isBangla) {
            "চিত্ত যেথা ভয়শূন্য, উচ্চ যেথা শির। বই মানুষের শ্রেষ্ঠ বন্ধু।"
        } else {
            "This is a test preview of the selected voice."
        }

        val textToSpeak = prepareEngineForText(rawText, isBangla)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "test_utterance")
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _playbackVolume.value)
        }
        engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "test_utterance")
    }

    /**
     * Configures the TTS engine for the incoming sentence.
     * Returns the finalized string to speak (may be phonetically transliterated if device lacks Bangla).
     */
    private fun prepareEngineForText(text: String, forcedBangla: Boolean = false): String {
        val engine = tts ?: return text
        val isBangla = forcedBangla || isBanglaText(text)
        _isReadingBangla.value = isBangla

        val mode = _selectedLanguageMode.value
        val shouldReadBangla = mode == "BANGLA" || (mode == "AUTO" && isBangla)

        if (shouldReadBangla) {
            // Find if any Bangla locale is natively available in the TTS engine
            var nativeBanglaLocale: Locale? = null
            for (loc in BANGLA_LOCALES) {
                try {
                    val status = engine.isLanguageAvailable(loc)
                    if (status >= TextToSpeech.LANG_AVAILABLE) {
                        nativeBanglaLocale = loc
                        break
                    }
                } catch (_: Exception) {}
            }

            if (nativeBanglaLocale != null) {
                _isBanglaSupportedOnDevice.value = true
                applyVoiceConfiguration(nativeBanglaLocale, true)
                // Clean Bengali punctuation for smooth pauses
                return text.replace("।", ".").replace("॥", ".").replace("\u200C", "").replace("\u200D", "")
            } else {
                // Native Bangla voice is NOT downloaded on device:
                // Provide intelligent phonetic transliteration fallback so the user hears the Bengali text spoken clearly!
                _isBanglaSupportedOnDevice.value = false
                applyVoiceConfiguration(Locale.US, false)
                val phonetic = transliterateBanglaToPhonetic(text)
                return phonetic
            }
        } else if (mode == "ENGLISH") {
            applyVoiceConfiguration(Locale.US, false)
            return text
        } else {
            // Auto / System Default for non-Bangla
            val defLocale = Locale.getDefault()
            val avail = try { engine.isLanguageAvailable(defLocale) } catch (_: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
            val safeLocale = if (avail >= TextToSpeech.LANG_AVAILABLE) defLocale else Locale.US
            applyVoiceConfiguration(safeLocale, false)
            return text
        }
    }

    private fun speakCurrentSentence() {
        val index = _currentSentenceIndex.value
        val rawTextToSpeak = sentences.getOrNull(index) ?: return
        _currentSentenceText.value = rawTextToSpeak

        val engine = tts
        if (engine == null || !isInitialized) {
            pendingPlayOnReady = true
            initTts()
            return
        }

        val processedText = prepareEngineForText(rawTextToSpeak)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_$index")
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _playbackVolume.value)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        val result = engine.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "sentence_$index")
        if (result != TextToSpeech.SUCCESS) {
            try {
                engine.language = Locale.US
                engine.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "sentence_$index")
            } catch (_: Exception) {}
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        pendingPlayOnReady = false
    }
}
