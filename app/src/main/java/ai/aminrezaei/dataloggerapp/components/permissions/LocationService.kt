package ai.aminrezaei.dataloggerapp.components.permissions

import ai.aminrezaei.dataloggerapp.R
import ai.aminrezaei.dataloggerapp.StepsCounter
import ai.aminrezaei.dataloggerapp.components.datamanagement.AccelerometerDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.AccelerometerEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.AppDatabase
import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.datamanagement.DeviceStateDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.DeviceStateEntity
import ai.aminrezaei.dataloggerapp.components.datamanagement.LocationDao
import ai.aminrezaei.dataloggerapp.components.datamanagement.LocationEntity
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.receivers.Receivers
import ai.aminrezaei.dataloggerapp.utils.Constants
import ai.aminrezaei.dataloggerapp.utils.NetworkUtils
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import kotlin.coroutines.resume

class LocationService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock
    private val wakeLockTimeout: Long = 10 * 60 * 1000L // 10 minutes
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var appDatabase: AppDatabase
    private lateinit var locationDao: LocationDao
    private lateinit var accelerometerDao: AccelerometerDao
    private lateinit var deviceStateDao: DeviceStateDao

    private lateinit var stepsCounter: StepsCounter
    private lateinit var receivers: Receivers
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private val deviceStateMutex = Mutex()

    private var periodicDeviceStateJob: Job? = null
    private var lastLocationElapsedRealtimeNanos: Long? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var accelBootToUnixOffsetMs: Long = 0L
    private val accelerometerBuffer = mutableListOf<AccelerometerEntity>()
    private val accelerometerBufferLock = Any()
    private var isAccelerometerRegistered: Boolean = false
    private var rateCalibrationStartNs: Long = 0L
    private var rateCalibrationCount: Int = 0
    private var measuredHzCache: Float = 0f

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastWifiConnected: Int? = null
    private var lastWifiNetworkHash: String? = null

    private val activityRecognitionClient by lazy { ActivityRecognition.getClient(this) }
    private var activityUpdatePendingIntent: PendingIntent? = null
    private var activityUpdateReceiver: BroadcastReceiver? = null
    private var isActivityReceiverRegistered: Boolean = false
    private var latestActivityType: String = "UNKNOWN"
    private var latestActivityConfidence: Int = 0

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
                return
            }

            if (!sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_ACCELEROMETER)) {
                return
            }

            calibrateAccelerometerRate(event)

            val unixTimestampMs = accelBootToUnixOffsetMs + (event.timestamp / 1_000_000L)
            val entity = AccelerometerEntity(
                timestamp = unixTimestampMs,
                elapsedNs = event.timestamp,
                x = event.values[0].toDouble(),
                y = event.values[1].toDouble(),
                z = event.values[2].toDouble()
            )

            val pendingBatch = synchronized(accelerometerBufferLock) {
                accelerometerBuffer.add(entity)
                if (accelerometerBuffer.size >= ACCEL_BATCH_SIZE) {
                    val copy = accelerometerBuffer.toList()
                    accelerometerBuffer.clear()
                    copy
                } else {
                    emptyList()
                }
            }

            if (pendingBatch.isNotEmpty()) {
                serviceScope.launch(Dispatchers.IO) {
                    runCatching {
                        accelerometerDao.insertAll(pendingBatch)
                    }.onFailure { error ->
                        Log.e("LocationService", "Failed to insert accelerometer batch", error)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        isRunning = true

        DatabaseProvider.init(this)
        appDatabase = DatabaseProvider.getDatabase()
        locationDao = appDatabase.locationDao()
        accelerometerDao = appDatabase.accelerometerDao()
        deviceStateDao = appDatabase.deviceStateDao()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        stepsCounter = StepsCounter(this)
        receivers = Receivers(this) { trigger ->
            serviceScope.launch {
                insertDeviceState(trigger)
            }
        }.apply { register() }
        sharedPreferencesManager = SharedPreferencesManager(this)
        sharedPreferencesManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch(Dispatchers.IO) {
                        insertLocation(location)
                    }
                } ?: Log.e("LocationService", "Location is null")
            }
        }

        updateAccelerometerCollectionState()
        registerWifiChangeCallback()
        registerActivityUpdates()
        startPeriodicDeviceStateCollection()
        warnIfLegacyTableExists()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("LABDA Logger")
            .setContentText("Logging in progress...")
            .setSmallIcon(R.drawable.labda)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        acquireWakeLock()
        if (sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_LOCATION)) {
            startLocationUpdates()
        }

        Log.d("LocationService", "onStartCommand called")
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val intervalMs = sharedPreferencesManager
            .getSharedPreferences()
            .getLong(Constants.DATA_INSERT_DELAY_KEY, 1000L)
            .coerceIn(1000L, 60_000L)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setIntervalMillis(intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .setMinUpdateDistanceMeters(5f)
            .setMaxUpdateDelayMillis(intervalMs * 5)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d("LocationService", "Location updates started")
        } else {
            Log.d("LocationService", "Location permission not granted, skipping location updates")
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService::WakeLock")
        wakeLock.acquire(wakeLockTimeout)

        handler.postDelayed({
            if (::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
            }
            acquireWakeLock()
        }, wakeLockTimeout - 60 * 1000L)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        isRunning = false
        flushAccelerometerBufferBlocking()
        unregisterAccelerometerCollection()

        periodicDeviceStateJob?.cancel()
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null)
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        unregisterWifiChangeCallback()
        unregisterActivityUpdates()
        receivers.unregister()
        stepsCounter.unregister()
        sharedPreferencesManager.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)

        Log.d("LocationService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "LocationServiceChannel",
            "Location Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for location service"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        Log.d("LocationService", "Notification channel created")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.DATA_INSERT_DELAY_KEY -> {
                Log.d("LocationService", "Data insert delay preference changed: $key")
                fusedLocationClient.removeLocationUpdates(locationCallback)
                startLocationUpdates()
            }
            Constants.LOGGING_LOCATION -> {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                if (sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_LOCATION)) {
                    startLocationUpdates()
                }
            }
            Constants.LOGGING_ACCELEROMETER -> {
                updateAccelerometerCollectionState()
            }
            Constants.LOGGING_ACTIVITY_RECOGNITION -> {
                if (sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_ACTIVITY_RECOGNITION)) {
                    registerActivityUpdates()
                } else {
                    unregisterActivityUpdates()
                    latestActivityType = "UNKNOWN"
                    latestActivityConfidence = 0
                }
            }
        }
    }

    private suspend fun insertLocation(location: android.location.Location) {
        val isLocationLoggingEnabled =
            sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_LOCATION)
        if (!isLocationLoggingEnabled) {
            return
        }

        val elapsedRealtimeNanos = location.elapsedRealtimeNanos
        val isFreshFix = if (lastLocationElapsedRealtimeNanos == elapsedRealtimeNanos) 0 else 1
        lastLocationElapsedRealtimeNanos = elapsedRealtimeNanos

        val entity = LocationEntity(
            timestamp = System.currentTimeMillis(),
            elapsedRealtimeNanos = elapsedRealtimeNanos,
            elapsedRealtimeUncertaintyNs = location.elapsedRealtimeUncertaintyNanos,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy.toDouble(),
            verticalAccuracy = location.verticalAccuracyMeters.toDouble(),
            speed = location.speed.toDouble(),
            speedAccuracy = location.speedAccuracyMetersPerSecond.toDouble(),
            bearing = location.bearing.toDouble(),
            bearingAccuracy = location.bearingAccuracyDegrees.toDouble(),
            provider = location.provider ?: "fused",
            isFreshFix = isFreshFix
        )

        runCatching {
            locationDao.insert(entity)
            Log.d("LocationService", "Location row inserted")
        }.onFailure { error ->
            Log.e("LocationService", "Failed to insert location row", error)
        }
    }

    private fun registerAccelerometerCollection() {
        if (isAccelerometerRegistered) {
            return
        }
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometerSensor == null) {
            Log.w("LocationService", "TYPE_ACCELEROMETER sensor not available")
            return
        }

        accelBootToUnixOffsetMs =
            System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() / 1_000_000L)

        val registered = sensorManager.registerListener(
            accelerometerListener,
            accelerometerSensor,
            ACCEL_PERIOD_US,
            Handler(Looper.getMainLooper())
        )
        isAccelerometerRegistered = registered
        Log.d("LocationService", "Accelerometer listener registered: $registered")
    }

    private fun unregisterAccelerometerCollection() {
        if (!isAccelerometerRegistered) {
            return
        }
        accelerometerSensor?.let {
            sensorManager.unregisterListener(accelerometerListener, it)
        }
        isAccelerometerRegistered = false
    }

    private fun updateAccelerometerCollectionState() {
        val enabled = sharedPreferencesManager
            .getLoggingPreference(Constants.LOGGING_ACCELEROMETER)
        if (enabled) {
            registerAccelerometerCollection()
        } else {
            flushAccelerometerBufferBlocking()
            unregisterAccelerometerCollection()
        }
    }

    private fun calibrateAccelerometerRate(event: SensorEvent) {
        if (rateCalibrationCount >= RATE_CALIBRATION_EVENTS) {
            return
        }
        if (rateCalibrationCount == 0) {
            rateCalibrationStartNs = event.timestamp
        }
        rateCalibrationCount++
        if (rateCalibrationCount == RATE_CALIBRATION_EVENTS) {
            val durationSeconds =
                (event.timestamp - rateCalibrationStartNs).toDouble() / 1_000_000_000.0
            if (durationSeconds > 0) {
                measuredHzCache = (RATE_CALIBRATION_EVENTS.toDouble() / durationSeconds).toFloat()
                sharedPreferencesManager.saveMeasuredAccelerometerHz(measuredHzCache)
                Log.i("LocationService", "Actual accelerometer delivery rate: $measuredHzCache Hz")
            }
        }
    }

    private fun flushAccelerometerBufferBlocking() {
        val pending = synchronized(accelerometerBufferLock) {
            if (accelerometerBuffer.isEmpty()) {
                emptyList()
            } else {
                val copy = accelerometerBuffer.toList()
                accelerometerBuffer.clear()
                copy
            }
        }

        if (pending.isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                runCatching {
                    accelerometerDao.insertAll(pending)
                }.onFailure { error ->
                    Log.e("LocationService", "Failed to flush accelerometer buffer", error)
                }
            }
        }
    }

    private fun startPeriodicDeviceStateCollection() {
        if (periodicDeviceStateJob?.isActive == true) {
            return
        }
        periodicDeviceStateJob = serviceScope.launch {
            delay(DEVICE_STATE_STARTUP_WARMUP_MS)
            insertDeviceState("periodic")
            while (isActive) {
                delay(DEVICE_STATE_PERIODIC_INTERVAL_MS)
                insertDeviceState("periodic")
            }
        }
    }

    private suspend fun insertDeviceState(trigger: String) {
        deviceStateMutex.withLock {
            val batteryEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_BATTERY)
            val screenEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_SCREEN)
            val wifiEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_WIFI_CONNECTED)
            val wifiHashEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_WIFI_HASH)
            val signalEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_SIGNAL)
            val ringerEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_RINGER_MODE)
            val audioOutputEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_AUDIO_OUTPUT)
            val lightEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_LIGHT)
            val proximityEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_PROXIMITY)
            val activityEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_ACTIVITY_RECOGNITION)
            val stepsEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_STEPS)

            if (!batteryEnabled && !screenEnabled && !wifiEnabled && !wifiHashEnabled
                && !signalEnabled && !ringerEnabled && !audioOutputEnabled
                && !lightEnabled && !proximityEnabled && !activityEnabled && !stepsEnabled) {
                return@withLock
            }

            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryPct = if (batteryEnabled) batteryIntent?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    (level.toFloat() / scale.toFloat() * 100).toInt()
                } else {
                    null
                }
            } else null

            val batteryStatus =
                batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val batteryPlugged =
                batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

            val wifiConnected = if (wifiEnabled) NetworkUtils.isWifiConnected(this@LocationService) else null
            val wifiNetworkHash = if (wifiEnabled && wifiHashEnabled && wifiConnected == 1) {
                resolveWifiNetworkHash()
            } else {
                null
            }

            lastWifiConnected = wifiConnected
            lastWifiNetworkHash = wifiNetworkHash

            val entity = DeviceStateEntity(
                timestamp = System.currentTimeMillis(),
                trigger = trigger,
                batteryPct = batteryPct,
                chargingState = if (batteryEnabled) mapChargingState(batteryStatus) else null,
                chargeType = if (batteryEnabled) mapChargeType(batteryPlugged) else null,
                screenOn = if (screenEnabled) (if (isScreenOn()) 1 else 0) else null,
                screenLocked = if (screenEnabled) (if (isScreenLocked()) 1 else 0) else null,
                unlockCount = if (screenEnabled) receivers.lockUnlockCount else null,
                wifiConnected = wifiConnected,
                wifiNetworkHash = wifiNetworkHash,
                signalDbm = if (signalEnabled) NetworkUtils.getSignalDbm(this@LocationService) else null,
                signalBars = if (signalEnabled) NetworkUtils.getSignalBars(this@LocationService) else null,
                ringerMode = if (ringerEnabled) getRingerMode() else null,
                audioOutput = if (audioOutputEnabled) getAudioOutput() else null,
                lightLux = if (lightEnabled) readLightLuxSnapshot() else null,
                proximityNear = if (proximityEnabled) readProximityNearSnapshot() else null,
                activityType = if (activityEnabled) latestActivityType else null,
                activityConfidence = if (activityEnabled) latestActivityConfidence else null,
                stepsSinceBoot = if (stepsEnabled) stepsCounter.getStepsSinceBoot() else null,
                deviceUptimeMs = SystemClock.elapsedRealtime()
            )

            runCatching {
                withContext(Dispatchers.IO) {
                    deviceStateDao.insert(entity)
                }
                Log.d("LocationService", "Device state row inserted (trigger=$trigger)")
            }.onFailure { error ->
                Log.e("LocationService", "Failed to insert device state row", error)
            }

            if (trigger == "periodic" && wifiEnabled && wifiHashEnabled && wifiConnected == 1 && wifiNetworkHash == null) {
                serviceScope.launch {
                    delay(WIFI_HASH_RETRY_DELAY_MS)
                    insertDeviceState("wifi_change")
                }
            }
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun isScreenLocked(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    private fun mapChargingState(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "not_charging"
        }
    }

    private fun mapChargeType(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "none"
        }
    }

    private fun getRingerMode(): String {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "normal"
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            else -> "normal"
        }
    }

    private fun getAudioOutput(): String {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val hasBluetooth = outputDevices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }
        if (hasBluetooth) return "bluetooth"

        val hasWired = outputDevices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        if (hasWired) return "wired"

        val hasSpeaker = outputDevices.any {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
        return if (hasSpeaker) "speaker" else "none"
    }

    private fun getWifiNetworkHash(): String? {
        return try {
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val rawSsid = wifiManager.connectionInfo?.ssid
            val normalizedSsid = rawSsid
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.takeIf { it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true) }
                ?: return null

            sha256Short(normalizedSsid)
        } catch (_: SecurityException) {
            null
        }
    }

    private suspend fun resolveWifiNetworkHash(): String? {
        // Retry briefly to avoid startup races where connected WiFi is reported before SSID/BSSID is readable.
        repeat(WIFI_HASH_RETRY_ATTEMPTS) {
            val hash = getWifiNetworkHash()
            if (!hash.isNullOrBlank()) {
                return hash
            }
            delay(WIFI_HASH_RETRY_STEP_MS)
        }
        return lastWifiNetworkHash
    }

    private fun sha256Short(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    private suspend fun readLightLuxSnapshot(): Double? {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return null
        return readSingleSensorValue(lightSensor) { values, _ ->
            values[0].toDouble()
        }
    }

    private suspend fun readProximityNearSnapshot(): Int? {
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return null
        return readSingleSensorValue(proximitySensor) { values, sensor ->
            if (values[0] < sensor.maximumRange) 1 else 0
        }
    }

    private suspend fun <T> readSingleSensorValue(
        sensor: Sensor,
        mapper: (FloatArray, Sensor) -> T
    ): T? {
        return withTimeoutOrNull(SENSOR_SNAPSHOT_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null || event.sensor.type != sensor.type) {
                            return
                        }
                        sensorManager.unregisterListener(this, sensor)
                        if (continuation.isActive) {
                            continuation.resume(mapper(event.values, sensor))
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                val registered = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    Handler(Looper.getMainLooper())
                )
                if (!registered) {
                    sensorManager.unregisterListener(listener, sensor)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                    return@suspendCancellableCoroutine
                }

                continuation.invokeOnCancellation {
                    sensorManager.unregisterListener(listener, sensor)
                }
            }
        }
    }

    private fun registerWifiChangeCallback() {
        runCatching {
            val wifiEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_WIFI_CONNECTED)
            val hashEnabled = sharedPreferencesManager
                .getLoggingPreference(Constants.LOGGING_WIFI_HASH)
            lastWifiConnected = if (wifiEnabled) NetworkUtils.isWifiConnected(this) else null
            lastWifiNetworkHash = if (wifiEnabled && hashEnabled && lastWifiConnected == 1) {
                getWifiNetworkHash()
            } else {
                null
            }

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    handleWifiChangeTrigger()
                }

                override fun onLost(network: Network) {
                    handleWifiChangeTrigger()
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        }.onFailure { error ->
            Log.e("LocationService", "Failed to register wifi callback", error)
        }
    }

    private fun unregisterWifiChangeCallback() {
        networkCallback?.let { callback ->
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
            networkCallback = null
        }
    }

    private fun handleWifiChangeTrigger() {
        val wifiEnabled = sharedPreferencesManager
            .getLoggingPreference(Constants.LOGGING_WIFI_CONNECTED)
        if (!wifiEnabled) {
            return
        }
        val hashEnabled = sharedPreferencesManager
            .getLoggingPreference(Constants.LOGGING_WIFI_HASH)
        val wifiConnected = NetworkUtils.isWifiConnected(this)
        val wifiHash = if (hashEnabled && wifiConnected == 1) getWifiNetworkHash() else null
        if (wifiConnected != lastWifiConnected || wifiHash != lastWifiNetworkHash) {
            lastWifiConnected = wifiConnected
            lastWifiNetworkHash = wifiHash
            serviceScope.launch {
                insertDeviceState("wifi_change")
            }
        } else if (wifiConnected == 1 && hashEnabled && wifiHash == null) {
            serviceScope.launch {
                delay(WIFI_HASH_RETRY_DELAY_MS)
                insertDeviceState("wifi_change")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerActivityUpdates() {
        if (isActivityReceiverRegistered) {
            return
        }
        if (!sharedPreferencesManager.getLoggingPreference(Constants.LOGGING_ACTIVITY_RECOGNITION)) {
            latestActivityType = "UNKNOWN"
            latestActivityConfidence = 0
            return
        }
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        if (!isPermissionGranted) {
            latestActivityType = "UNKNOWN"
            latestActivityConfidence = 0
            return
        }

        val intentFilter = IntentFilter(ACTIVITY_UPDATE_ACTION)
        activityUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val result = ActivityRecognitionResult.extractResult(intent ?: return) ?: return
                val probable = result.probableActivities.maxByOrNull { it.confidence } ?: return

                latestActivityType = mapDetectedActivityType(probable.type)
                latestActivityConfidence = probable.confidence

                serviceScope.launch {
                    insertDeviceState("activity_change")
                }
            }
        }

        activityUpdateReceiver?.let { receiver ->
            ContextCompat.registerReceiver(
                this,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isActivityReceiverRegistered = true
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTIVITY_UPDATE_ACTION).setPackage(packageName),
            pendingIntentFlags
        )
        activityUpdatePendingIntent = pendingIntent

        activityRecognitionClient
            .requestActivityUpdates(ACTIVITY_UPDATE_INTERVAL_MS, pendingIntent)
            .addOnFailureListener { error ->
                Log.e("LocationService", "Activity updates registration failed", error)
            }
    }

    private fun unregisterActivityUpdates() {
        activityUpdatePendingIntent?.let { pendingIntent ->
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (isPermissionGranted) {
                activityRecognitionClient
                    .removeActivityUpdates(pendingIntent)
                    .addOnFailureListener { error ->
                        Log.w("LocationService", "Activity updates removal failed", error)
                    }
            }
        }
        activityUpdatePendingIntent = null

        activityUpdateReceiver?.let { receiver ->
            if (isActivityReceiverRegistered) {
                runCatching {
                    unregisterReceiver(receiver)
                }
                isActivityReceiverRegistered = false
            }
        }
        activityUpdateReceiver = null
    }

    private fun mapDetectedActivityType(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.STILL -> "STILL"
            else -> "UNKNOWN"
        }
    }

    private fun warnIfLegacyTableExists() {
        serviceScope.launch(Dispatchers.IO) {
            val warningAlreadyShown = sharedPreferencesManager
                .getSharedPreferences()
                .getBoolean(KEY_LEGACY_WARNING_SHOWN, false)
            if (warningAlreadyShown) {
                return@launch
            }

            val db = appDatabase.openHelper.readableDatabase
            val cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='location_legacy'"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    Log.w(
                        "LocationService",
                        "Legacy table 'location_legacy' exists. Verify migrated row counts before manual drop."
                    )
                    sharedPreferencesManager
                        .getSharedPreferences()
                        .edit()
                        .putBoolean(KEY_LEGACY_WARNING_SHOWN, true)
                        .apply()
                }
            }
        }
    }

    companion object {
        /** True while the foreground collection service is running. Read by the UI to show real state. */
        @Volatile
        var isRunning: Boolean = false
            private set

        private const val ACCEL_PERIOD_US = 40_000
        private const val ACCEL_BATCH_SIZE = 50
        private const val RATE_CALIBRATION_EVENTS = 500
        private const val DEVICE_STATE_STARTUP_WARMUP_MS = 3_000L
        private const val DEVICE_STATE_PERIODIC_INTERVAL_MS = 5 * 60 * 1000L
        private const val WIFI_HASH_RETRY_ATTEMPTS = 3
        private const val WIFI_HASH_RETRY_STEP_MS = 500L
        private const val WIFI_HASH_RETRY_DELAY_MS = 5_000L
        private const val SENSOR_SNAPSHOT_TIMEOUT_MS = 1_000L
        private const val ACTIVITY_UPDATE_INTERVAL_MS = 30_000L
        private const val ACTIVITY_UPDATE_ACTION =
            "ai.aminrezaei.dataloggerapp.ACTION_ACTIVITY_UPDATE"
        private const val KEY_LEGACY_WARNING_SHOWN = "legacy_table_warning_shown"
    }
}
