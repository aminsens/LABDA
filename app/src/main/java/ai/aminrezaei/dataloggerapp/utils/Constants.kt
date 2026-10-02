package ai.aminrezaei.dataloggerapp.utils

object Constants {
    const val PREFS_NAME = "LABDALoggerPrefs"
    const val KEY_FIRST_TIME_LAUNCH = "isFirstTimeLaunch"
    const val KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN = "batteryOptimizationPromptShown"
    const val LOCK_UNLOCK_COUNT_KEY = "lockUnlockCount"
    const val DATA_INSERT_DELAY_KEY = "dataInsertDelay"
    const val SELECTED_THEME_KEY = "selected_theme"
    const val KEY_ACCEL_MEASURED_HZ = "accel_measured_hz"
    const val KEY_COLLECTION_STARTED_AT_MS = "collection_started_at_ms"
    const val KEY_EMA_INTERVAL_HOURS = "ema_interval_hours"
    const val KEY_EMA_QUIET_HOURS_START = "ema_quiet_hours_start"
    const val KEY_EMA_QUIET_HOURS_END = "ema_quiet_hours_end"

    const val LOGGING_ACCELEROMETER = "logging_accelerometer"
    const val LOGGING_ACCEL_LIMIT_25HZ = "logging_acc_limit_25hz"
    const val LOGGING_LOCATION = "logging_location"
    const val LOGGING_STEPS = "logging_steps"

    const val LOGGING_BATTERY = "logging_battery"
    const val LOGGING_SCREEN = "logging_screen"
    const val LOGGING_WIFI_CONNECTED = "logging_wifi_connected"
    const val LOGGING_WIFI_HASH = "logging_wifi_hash"
    const val LOGGING_SIGNAL = "logging_signal"
    const val LOGGING_RINGER_MODE = "logging_ringer_mode"
    const val LOGGING_AUDIO_OUTPUT = "logging_audio_output"

    const val LOGGING_ACTIVITY_RECOGNITION = "logging_activity_recognition"
    const val LOGGING_LIGHT = "logging_light"
    const val LOGGING_PROXIMITY = "logging_proximity"

    const val LOGGING_EMA_PROMPTS = "logging_ema_prompts"

    // EMA notification and scheduling constants
    const val EMA_NOTIFICATION_CHANNEL_ID = "ema_prompts_channel"
    const val EMA_WORK_TAG = "ema_prompt_work"
    const val EMA_INTENT_ACTION_OPEN = "OPEN_EMA"
    const val EMA_INTENT_EXTRA_PROMPT_ROW_ID = "PROMPT_ROW_ID"
    const val EMA_NOTIFICATION_ID_BASE = 9000

    // Legacy aliases kept for backward compatibility with existing UI/state code.
    const val LOGGING_WIFI_STATUS = LOGGING_WIFI_CONNECTED
    const val LOGGING_SCREEN_STATUS = LOGGING_SCREEN
    const val LOGGING_SIGNAL_STRENGTH = LOGGING_SIGNAL
    const val LOGGING_PHYSICAL_ACTIVITY = LOGGING_ACTIVITY_RECOGNITION
}
