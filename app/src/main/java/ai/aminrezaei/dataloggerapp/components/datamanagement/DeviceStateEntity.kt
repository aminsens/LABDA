package ai.aminrezaei.dataloggerapp.components.datamanagement

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_state")
data class DeviceStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "trigger")
    val trigger: String,
    @ColumnInfo(name = "battery_pct")
    val batteryPct: Int?,
    @ColumnInfo(name = "charging_state")
    val chargingState: String?,
    @ColumnInfo(name = "charge_type")
    val chargeType: String?,
    @ColumnInfo(name = "screen_on")
    val screenOn: Int?,
    @ColumnInfo(name = "screen_locked")
    val screenLocked: Int?,
    @ColumnInfo(name = "unlock_count")
    val unlockCount: Int?,
    @ColumnInfo(name = "wifi_connected")
    val wifiConnected: Int?,
    @ColumnInfo(name = "wifi_network_hash")
    val wifiNetworkHash: String?,
    @ColumnInfo(name = "signal_dbm")
    val signalDbm: Int?,
    @ColumnInfo(name = "signal_bars")
    val signalBars: Int?,
    @ColumnInfo(name = "ringer_mode")
    val ringerMode: String?,
    @ColumnInfo(name = "audio_output")
    val audioOutput: String?,
    @ColumnInfo(name = "light_lux")
    val lightLux: Double?,
    @ColumnInfo(name = "proximity_near")
    val proximityNear: Int?,
    @ColumnInfo(name = "activity_type")
    val activityType: String?,
    @ColumnInfo(name = "activity_confidence")
    val activityConfidence: Int?,
    @ColumnInfo(name = "steps_since_boot")
    val stepsSinceBoot: Int?,
    @ColumnInfo(name = "device_uptime_ms")
    val deviceUptimeMs: Long?
)
