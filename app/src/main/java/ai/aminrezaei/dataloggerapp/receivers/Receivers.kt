package ai.aminrezaei.dataloggerapp.receivers

import ai.aminrezaei.dataloggerapp.utils.Constants
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log

class Receivers(
    private val context: Context,
    private val onTrigger: ((String) -> Unit)? = null
) : BroadcastReceiver() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    var lockUnlockCount: Int
        get() = sharedPreferences.getInt(Constants.LOCK_UNLOCK_COUNT_KEY, 0)
        private set(value) {
            sharedPreferences.edit().putInt(Constants.LOCK_UNLOCK_COUNT_KEY, value).apply()
        }

    var isScreenOn: Boolean = true
        private set

    var isScreenLocked: Boolean = false
        private set

    var chargeMode: String = "UNKNOWN"
        private set

    var batteryPercentage: Int = -1
        private set

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                isScreenOn = true
                updateLockStatus()
                onTrigger?.invoke("screen_change")
            }
            Intent.ACTION_SCREEN_OFF -> {
                isScreenOn = false
                updateLockStatus()
                onTrigger?.invoke("screen_change")
            }
            Intent.ACTION_USER_PRESENT -> {
                lockUnlockCount++
                updateLockStatus()
                onTrigger?.invoke("screen_change")
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val previousChargeMode = chargeMode
                updateChargeMode(intent)
                updateBatteryPercentage(intent)
                if (previousChargeMode != chargeMode) {
                    onTrigger?.invoke("charge_change")
                }
            }
        }
        Log.d("Receivers", "Screen status updated: isScreenOn = $isScreenOn, isScreenLocked = $isScreenLocked, lockUnlockCount = $lockUnlockCount, chargeMode = $chargeMode, batteryPercentage = $batteryPercentage")
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(this, filter)
        updateLockStatus()
        Log.d("Receivers", "Receivers registered")
    }

    fun unregister() {
        try {
            context.unregisterReceiver(this)
            Log.d("Receivers", "Receivers unregistered")
        } catch (e: IllegalArgumentException) {
            Log.e("Receivers", "Receivers not registered or already unregistered")
        }
    }

    private fun updateLockStatus() {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isScreenLocked = keyguardManager?.isKeyguardLocked ?: false
        Log.d("Receivers", "Lock status updated: isScreenLocked = $isScreenLocked")
    }

    private fun updateChargeMode(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        chargeMode = when {
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "Charging via USB"
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "Charging via AC"
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charging via Wireless"
            status == BatteryManager.BATTERY_STATUS_DISCHARGING -> "On Battery"
            status == BatteryManager.BATTERY_STATUS_FULL -> "Full"
            status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    private fun updateBatteryPercentage(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        batteryPercentage = if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            -1
        }
    }
}
