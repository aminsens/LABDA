package ai.aminrezaei.dataloggerapp.components.permissions

import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.ui.state.PermissionState
import ai.aminrezaei.dataloggerapp.utils.PermissionUtils
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionStates(
    val locationPermission: Boolean? = null,
    val activityRecognitionPermission: Boolean? = null,
    val backgroundLocationPermission: Boolean? = null,
    val bodySensorsPermission: Boolean? = null,
    val phoneStatePermission: Boolean? = null,
    val notificationPermission: Boolean? = null,
    val locationEnabled: Boolean = false,
    val physicalActivityEnabled: Boolean = false
)

class PermissionManager(
    private val context: Context,
    private val sharedPreferencesManager: SharedPreferencesManager
) {
    private val _permissionStates = MutableStateFlow(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _shouldRequestPermissions = MutableStateFlow<Set<String>>(emptySet())
    val shouldRequestPermissions: StateFlow<Set<String>> = _shouldRequestPermissions.asStateFlow()

    private val sharedPreferences = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)


    init {
        initializePermissions()
    }

    private fun initializePermissions() {
        _permissionStates.update {
            PermissionStates(
                notificationPermission = checkNotificationPermission(),
                locationPermission = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION),
                backgroundLocationPermission = checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                activityRecognitionPermission = checkPermission(Manifest.permission.ACTIVITY_RECOGNITION),
                bodySensorsPermission = checkPermission(Manifest.permission.BODY_SENSORS),
                phoneStatePermission = checkPermission(Manifest.permission.READ_PHONE_STATE),
                locationEnabled = sharedPreferencesManager.getToggleState("location"),
                physicalActivityEnabled = sharedPreferencesManager.getToggleState("physical_activity")
            )
        }
        updatePermissionState()
        Log.d("PermissionManager", "Permissions initialized")
    }

    fun checkPermission(permission: String): Boolean? {
        return when (ContextCompat.checkSelfPermission(context, permission)) {
            PackageManager.PERMISSION_GRANTED -> true
            PackageManager.PERMISSION_DENIED -> {
                if (sharedPreferencesManager.hasAskedForPermission(permission)) false else null
            }
            else -> null
        }
    }

    fun checkAndUpdatePermissions() {
        _permissionStates.update { currentState ->
            currentState.copy(
                notificationPermission = checkNotificationPermission(),
                locationPermission = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION),
                backgroundLocationPermission = checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                activityRecognitionPermission = checkPermission(Manifest.permission.ACTIVITY_RECOGNITION),
                bodySensorsPermission = checkPermission(Manifest.permission.BODY_SENSORS),
                phoneStatePermission = checkPermission(Manifest.permission.READ_PHONE_STATE)
            )
        }
        updatePermissionState()
        Log.d("PermissionManager", "Permissions checked and updated")
    }

    fun onPermissionsResult(grantResults: Map<String, Boolean>) {
        _permissionStates.update { currentState ->
            grantResults.entries.fold(currentState) { state, (permission, isGranted) ->
                sharedPreferencesManager.setAskedForPermission(permission)
                when (permission) {
                    Manifest.permission.POST_NOTIFICATIONS -> state.copy(notificationPermission = isGranted)
                    Manifest.permission.ACCESS_FINE_LOCATION -> state.copy(locationPermission = isGranted)
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION -> state.copy(backgroundLocationPermission = isGranted)
                    Manifest.permission.ACTIVITY_RECOGNITION -> state.copy(activityRecognitionPermission = isGranted)
                    Manifest.permission.BODY_SENSORS -> state.copy(bodySensorsPermission = isGranted)
                    Manifest.permission.READ_PHONE_STATE -> state.copy(phoneStatePermission = isGranted)
                    else -> state
                }
            }
        }
        _shouldRequestPermissions.value = _shouldRequestPermissions.value - grantResults.keys
        checkAndUpdatePermissions()
    }

    private fun updatePermissionState(state: PermissionStates, permission: String, isGranted: Boolean?): PermissionStates {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> state.copy(notificationPermission = isGranted)
            Manifest.permission.ACCESS_FINE_LOCATION -> state.copy(locationPermission = isGranted)
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> state.copy(backgroundLocationPermission = isGranted)
            Manifest.permission.ACTIVITY_RECOGNITION -> state.copy(activityRecognitionPermission = isGranted)
            Manifest.permission.BODY_SENSORS -> state.copy(bodySensorsPermission = isGranted)
            Manifest.permission.READ_PHONE_STATE -> state.copy(phoneStatePermission = isGranted)
            else -> state
        }
    }

    fun permissionsRequested() {
        _shouldRequestPermissions.value = emptySet()
        Log.d("PermissionManager", "Permissions requested set cleared")
    }

    fun requestPermission(permissionType: String) {
        val permissionsToRequest = when (permissionType) {
            "location" -> PermissionUtils.requestLocationPermissions(context)
            "physical_activity" -> PermissionUtils.requestPhysicalActivityPermissions(context)
            "background_location" -> setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            "body_sensors" -> PermissionUtils.requestBodySensorsPermissions(context)
            "phone_state" -> PermissionUtils.requestPhoneStatePermissions(context)
            "notification" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                _permissionStates.update { it.copy(notificationPermission = true) }
                emptySet()
            }
            else -> emptySet()
        }
        _shouldRequestPermissions.value = permissionsToRequest
        updatePermissionState()
        Log.d("PermissionManager", "$permissionType permissions requested")
    }

    fun toggleFeature(feature: String, enabled: Boolean) {
        _permissionStates.update { currentState ->
            when (feature) {
                "location" -> currentState.copy(locationEnabled = enabled)
                "physical_activity" -> currentState.copy(physicalActivityEnabled = enabled)
                else -> currentState
            }.also {
                sharedPreferencesManager.saveToggleState(feature, enabled)
                if (enabled) {
                    when (feature) {
                        "location" -> {
                            if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) != true) {
                                _permissionState.value = PermissionState.NeedsLocationPermissionInfo
                            } else {
                                requestPermission("location")
                            }
                        }
                        "physical_activity" -> {
                            if (checkPermission(Manifest.permission.ACTIVITY_RECOGNITION) != true) {
                                requestPermission("physical_activity")
                            }
                        }
                    }
                } else {
                    _shouldRequestPermissions.value -= when (feature) {
                        "location" -> setOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        "physical_activity" -> setOf(Manifest.permission.ACTIVITY_RECOGNITION)
                        else -> emptySet()
                    }
                }
                Log.d("PermissionManager", "$feature feature toggled to $enabled")
            }
        }
        checkAndUpdatePermissions()
    }

    private fun updatePermissionState() {
        _permissionState.value = PermissionUtils.updatePermissionState(context, _shouldRequestPermissions.value)
        Log.d("PermissionManager", "Permission state updated to ${_permissionState.value}")
    }

    private fun checkNotificationPermission(): Boolean? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }
}
