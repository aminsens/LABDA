package ai.aminrezaei.dataloggerapp.ui.state

import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.utils.Constants
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val sharedPreferencesManager: SharedPreferencesManager) : ViewModel() {

    private val _accelerometerLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_ACCELEROMETER))
    val accelerometerLogging: StateFlow<Boolean> = _accelerometerLogging.asStateFlow()

    private val _locationLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_LOCATION))
    val locationLogging: StateFlow<Boolean> = _locationLogging.asStateFlow()

    private val _stepsLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_STEPS))
    val stepsLogging: StateFlow<Boolean> = _stepsLogging.asStateFlow()

    private val _batteryLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_BATTERY))
    val batteryLogging: StateFlow<Boolean> = _batteryLogging.asStateFlow()

    private val _screenLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_SCREEN))
    val screenLogging: StateFlow<Boolean> = _screenLogging.asStateFlow()

    private val _wifiConnectedLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_WIFI_CONNECTED))
    val wifiConnectedLogging: StateFlow<Boolean> = _wifiConnectedLogging.asStateFlow()

    private val _wifiHashLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_WIFI_HASH))
    val wifiHashLogging: StateFlow<Boolean> = _wifiHashLogging.asStateFlow()

    private val _signalLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_SIGNAL))
    val signalLogging: StateFlow<Boolean> = _signalLogging.asStateFlow()

    private val _ringerModeLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_RINGER_MODE))
    val ringerModeLogging: StateFlow<Boolean> = _ringerModeLogging.asStateFlow()

    private val _audioOutputLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_AUDIO_OUTPUT))
    val audioOutputLogging: StateFlow<Boolean> = _audioOutputLogging.asStateFlow()

    private val _activityRecognitionLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_ACTIVITY_RECOGNITION))
    val activityRecognitionLogging: StateFlow<Boolean> = _activityRecognitionLogging.asStateFlow()

    private val _lightLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_LIGHT))
    val lightLogging: StateFlow<Boolean> = _lightLogging.asStateFlow()

    private val _proximityLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_PROXIMITY))
    val proximityLogging: StateFlow<Boolean> = _proximityLogging.asStateFlow()

    private val _emaPromptsLogging =
        MutableStateFlow(sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_EMA_PROMPTS))
    val emaPromptsLogging: StateFlow<Boolean> = _emaPromptsLogging.asStateFlow()

    private val _emaIntervalHours = MutableStateFlow(
        sharedPreferencesManager.getSharedPreferences().getInt(Constants.KEY_EMA_INTERVAL_HOURS, 4)
    )
    val emaIntervalHours: StateFlow<Int> = _emaIntervalHours.asStateFlow()

    private val _emaQuietStart = MutableStateFlow(
        sharedPreferencesManager.getSharedPreferences().getString(Constants.KEY_EMA_QUIET_HOURS_START, "22:00")
            ?: "22:00"
    )
    val emaQuietStart: StateFlow<String> = _emaQuietStart.asStateFlow()

    private val _emaQuietEnd = MutableStateFlow(
        sharedPreferencesManager.getSharedPreferences().getString(Constants.KEY_EMA_QUIET_HOURS_END, "07:00")
            ?: "07:00"
    )
    val emaQuietEnd: StateFlow<String> = _emaQuietEnd.asStateFlow()

    private val _measuredAccelerometerHz =
        MutableStateFlow(sharedPreferencesManager.getMeasuredAccelerometerHz())
    val measuredAccelerometerHz: StateFlow<Float> = _measuredAccelerometerHz.asStateFlow()

    fun refreshMeasuredAccelerometerHz() {
        _measuredAccelerometerHz.value = sharedPreferencesManager.getMeasuredAccelerometerHz()
    }

    private fun setToggle(key: String, value: Boolean, target: MutableStateFlow<Boolean>) {
        target.value = value
        sharedPreferencesManager.saveLoggingPreference(key, value)
    }

    fun toggleAccelerometerLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_ACCELEROMETER, isEnabled, _accelerometerLogging)

    fun toggleLocationLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_LOCATION, isEnabled, _locationLogging)

    fun toggleStepsLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_STEPS, isEnabled, _stepsLogging)

    fun toggleBatteryLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_BATTERY, isEnabled, _batteryLogging)

    fun toggleScreenLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_SCREEN, isEnabled, _screenLogging)

    fun toggleWifiConnectedLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_WIFI_CONNECTED, isEnabled, _wifiConnectedLogging)

    fun toggleWifiHashLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_WIFI_HASH, isEnabled, _wifiHashLogging)

    fun toggleSignalLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_SIGNAL, isEnabled, _signalLogging)

    fun toggleRingerModeLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_RINGER_MODE, isEnabled, _ringerModeLogging)

    fun toggleAudioOutputLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_AUDIO_OUTPUT, isEnabled, _audioOutputLogging)

    fun toggleActivityRecognitionLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_ACTIVITY_RECOGNITION, isEnabled, _activityRecognitionLogging)

    fun toggleLightLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_LIGHT, isEnabled, _lightLogging)

    fun toggleProximityLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_PROXIMITY, isEnabled, _proximityLogging)

    fun toggleEmaPromptsLogging(isEnabled: Boolean) =
        setToggle(Constants.LOGGING_EMA_PROMPTS, isEnabled, _emaPromptsLogging)

    fun setEmaIntervalHours(hours: Int) {
        _emaIntervalHours.value = hours
        sharedPreferencesManager.getSharedPreferences()
            .edit()
            .putInt(Constants.KEY_EMA_INTERVAL_HOURS, hours)
            .apply()
    }

    fun setEmaQuietStart(value: String) {
        _emaQuietStart.value = value
        sharedPreferencesManager.getSharedPreferences()
            .edit()
            .putString(Constants.KEY_EMA_QUIET_HOURS_START, value)
            .apply()
    }

    fun setEmaQuietEnd(value: String) {
        _emaQuietEnd.value = value
        sharedPreferencesManager.getSharedPreferences()
            .edit()
            .putString(Constants.KEY_EMA_QUIET_HOURS_END, value)
            .apply()
    }

    // Grouped "Context data" toggle for the participant-facing Settings screen.
    fun isContextDataEnabled(): Boolean = listOf(
        stepsLogging, batteryLogging, screenLogging, wifiConnectedLogging,
        wifiHashLogging, signalLogging, ringerModeLogging, audioOutputLogging,
        activityRecognitionLogging, lightLogging, proximityLogging
    ).all { it.value }

    fun toggleContextDataLogging(isEnabled: Boolean) {
        toggleStepsLogging(isEnabled)
        toggleBatteryLogging(isEnabled)
        toggleScreenLogging(isEnabled)
        toggleWifiConnectedLogging(isEnabled)
        toggleWifiHashLogging(isEnabled)
        toggleSignalLogging(isEnabled)
        toggleRingerModeLogging(isEnabled)
        toggleAudioOutputLogging(isEnabled)
        toggleActivityRecognitionLogging(isEnabled)
        toggleLightLogging(isEnabled)
        toggleProximityLogging(isEnabled)
    }

    // Backward-compatible wrappers for existing callers.
    fun toggleWifiStatusLogging(isEnabled: Boolean) = toggleWifiConnectedLogging(isEnabled)
    fun toggleScreenStatusLogging(isEnabled: Boolean) = toggleScreenLogging(isEnabled)
    fun toggleSignalStrengthLogging(isEnabled: Boolean) = toggleSignalLogging(isEnabled)
    fun togglePhysicalActivityLogging(isEnabled: Boolean) = toggleActivityRecognitionLogging(isEnabled)
    val wifiStatusLogging: StateFlow<Boolean> = wifiConnectedLogging
    val screenStatusLogging: StateFlow<Boolean> = screenLogging
    val signalStrengthLogging: StateFlow<Boolean> = signalLogging
    val physicalActivityLogging: StateFlow<Boolean> = activityRecognitionLogging
}
