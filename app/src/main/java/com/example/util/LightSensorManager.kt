package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

enum class AmbientLightLevel(
    val label: String,
    val description: String,
    val suggestedThemeDark: Boolean
) {
    DARK(
        label = "Night / Low Light",
        description = "< 15 lux • Dimmed to reduce eye fatigue",
        suggestedThemeDark = true
    ),
    DIM(
        label = "Cozy Indoor",
        description = "15 - 150 lux • Soft balanced reading light",
        suggestedThemeDark = false
    ),
    NORMAL(
        label = "Well-Lit Room",
        description = "150 - 800 lux • Crisp daytime clarity",
        suggestedThemeDark = false
    ),
    BRIGHT(
        label = "Bright Daylight",
        description = "> 800 lux • High contrast outdoor visibility",
        suggestedThemeDark = false
    )
}

/**
 * Manages device ambient light sensor (Sensor.TYPE_LIGHT) to automatically
 * compute optimal screen brightness and complement dark/light themes.
 */
class LightSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isSensorAvailable: Boolean = lightSensor != null

    private val _currentLux = MutableStateFlow(100f) // reasonable indoor default
    val currentLux: StateFlow<Float> = _currentLux.asStateFlow()

    private val _calculatedBrightness = MutableStateFlow(0.35f)
    val calculatedBrightness: StateFlow<Float> = _calculatedBrightness.asStateFlow()

    private val _ambientLightLevel = MutableStateFlow(AmbientLightLevel.NORMAL)
    val ambientLightLevel: StateFlow<AmbientLightLevel> = _ambientLightLevel.asStateFlow()

    private var isListening = false
    private var simulatedLux: Float? = null

    // Exponential moving average filter to prevent jitter when user shifts posture
    private var smoothedLux = 100f

    fun startListening() {
        if (isListening || lightSensor == null || sensorManager == null) return
        isListening = true
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        isListening = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LIGHT) return
        if (simulatedLux != null) return // manual test override active

        val rawLux = event.values.firstOrNull() ?: return
        processNewLux(rawLux)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    /**
     * Set a simulated lux value for testing or emulator environments.
     * Pass null to return to live hardware readings.
     */
    fun setSimulatedLux(lux: Float?) {
        simulatedLux = lux
        if (lux != null) {
            processNewLux(lux)
        }
    }

    private fun processNewLux(rawLux: Float) {
        // Apply low-pass smoothing (alpha 0.20)
        smoothedLux = smoothedLux * 0.80f + rawLux * 0.20f
        val displayLux = max(0f, smoothedLux)
        
        // Hysteresis: only update if change is noticeable (prevents constant brightness hunting)
        if (abs(displayLux - _currentLux.value) < 6f && _currentLux.value > 0f) {
            return
        }
        
        _currentLux.value = displayLux

        // Determine ambient category
        _ambientLightLevel.value = when {
            displayLux < 15f -> AmbientLightLevel.DARK
            displayLux < 150f -> AmbientLightLevel.DIM
            displayLux < 800f -> AmbientLightLevel.NORMAL
            else -> AmbientLightLevel.BRIGHT
        }

        // Map Lux to comfortable reading brightness (never blinding, max 0.80f)
        val targetBrightness = computeBrightnessFromLux(displayLux)
        _calculatedBrightness.value = targetBrightness
    }

    companion object {
        fun computeBrightnessFromLux(lux: Float): Float {
            return when {
                lux <= 1f -> 0.08f
                lux <= 15f -> {
                    // 0.08f to 0.20f (comfortable night reading)
                    0.08f + (lux / 15f) * 0.12f
                }
                lux <= 150f -> {
                    // 0.20f to 0.38f (balanced cozy room)
                    0.20f + ((lux - 15f) / 135f) * 0.18f
                }
                lux <= 1000f -> {
                    // 0.38f to 0.62f (well-lit daylight room)
                    0.38f + ((lux - 150f) / 850f) * 0.24f
                }
                else -> {
                    // 0.62f to 0.80f (outdoor / direct light, capped at 0.80 to avoid blinding)
                    val highFactor = ((log10(max(1000f, lux)) - 3f) / 2f).coerceIn(0f, 1f)
                    0.62f + highFactor * 0.18f
                }
            }.coerceIn(0.06f, 0.80f)
        }
    }
}
