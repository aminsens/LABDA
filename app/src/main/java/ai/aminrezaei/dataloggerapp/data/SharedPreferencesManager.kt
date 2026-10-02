package ai.aminrezaei.dataloggerapp.data

import ai.aminrezaei.dataloggerapp.utils.Constants
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    fun setFirstTimeLaunch(isFirstTime: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.KEY_FIRST_TIME_LAUNCH, isFirstTime).apply()
    }

    fun isFirstTimeLaunch(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_FIRST_TIME_LAUNCH, true)
    }

    fun saveToggleState(feature: String, isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(feature, isEnabled).apply()
        Log.d("SharedPreferencesManager", "Saved toggle state: $feature = $isEnabled")
    }

    fun getToggleState(feature: String): Boolean {
        val state = sharedPreferences.getBoolean(feature, false)
        Log.d("SharedPreferencesManager", "Retrieved toggle state: $feature = $state")
        return state
    }

    fun savePermissionStatus(permission: String, isGranted: Boolean) {
        sharedPreferences.edit().putBoolean(permission, isGranted).apply()
        Log.d("SharedPreferencesManager", "Saved permission status: $permission = $isGranted")
    }

    fun getPermissionStatus(permission: String): Boolean {
        val status = sharedPreferences.getBoolean(permission, false)
        Log.d("SharedPreferencesManager", "Retrieved permission status: $permission = $status")
        return status
    }

    fun saveDataInsertDelay(delay: Long) {
        sharedPreferences.edit().putLong(Constants.DATA_INSERT_DELAY_KEY, delay).apply()
        Log.d("SharedPreferencesManager", "Saved data insert delay: $delay")
    }

    fun getDataInsertDelay(): Long {
        val delay = sharedPreferences.getLong(Constants.DATA_INSERT_DELAY_KEY, 1000L)
        Log.d("SharedPreferencesManager", "Retrieved data insert delay: $delay")
        return delay
    }

    fun saveLoggingPreference(key: String, isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(key, isEnabled).apply()
        Log.d("SharedPreferencesManager", "Saved logging preference: $key = $isEnabled")
    }

    fun getLoggingPreference(key: String): Boolean {
        // Keep explicit per-key defaults so new installs start from intended baseline behavior.
        val defaultValue = when (key) {
            Constants.LOGGING_ACCELEROMETER -> false
            Constants.LOGGING_ACCEL_LIMIT_25HZ -> false
            Constants.LOGGING_EMA_PROMPTS -> false
            else -> true
        }
        val value = sharedPreferences.getBoolean(key, defaultValue)
        Log.d("SharedPreferencesManager", "Retrieved logging preference: $key = $value")
        return value
    }

    fun saveSelectedTheme(themeIndex: Int) {
        sharedPreferences.edit().putInt(Constants.SELECTED_THEME_KEY, themeIndex).apply()
        Log.d("SharedPreferencesManager", "Saved selected theme: $themeIndex")
    }

    fun getSelectedTheme(): Int {
        val themeIndex = sharedPreferences.getInt(Constants.SELECTED_THEME_KEY, 0) // Default to 0 (Light theme)
        Log.d("SharedPreferencesManager", "Retrieved selected theme: $themeIndex")
        return themeIndex
    }

    fun saveMeasuredAccelerometerHz(hz: Float) {
        sharedPreferences.edit().putFloat(Constants.KEY_ACCEL_MEASURED_HZ, hz).apply()
    }

    fun getMeasuredAccelerometerHz(): Float {
        return sharedPreferences.getFloat(Constants.KEY_ACCEL_MEASURED_HZ, 0f)
    }

    fun saveCollectionStartedAtMs(timestampMs: Long) {
        sharedPreferences.edit().putLong(Constants.KEY_COLLECTION_STARTED_AT_MS, timestampMs).apply()
    }

    fun getCollectionStartedAtMs(): Long {
        return sharedPreferences.getLong(Constants.KEY_COLLECTION_STARTED_AT_MS, 0L)
    }

    fun hasAskedForPermission(permission: String): Boolean {
        return sharedPreferences.getBoolean("asked_$permission", false)
    }

    fun setAskedForPermission(permission: String) {
        sharedPreferences.edit().putBoolean("asked_$permission", true).apply()
    }

    fun isBatteryOptimizationPromptShown(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN, false)
    }

    fun setBatteryOptimizationPromptShown(shown: Boolean) {
        sharedPreferences.edit()
            .putBoolean(Constants.KEY_BATTERY_OPTIMIZATION_PROMPT_SHOWN, shown)
            .apply()
    }

}
