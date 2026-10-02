package ai.aminrezaei.dataloggerapp.ui.state

import ai.aminrezaei.dataloggerapp.components.permissions.LocationService
import ai.aminrezaei.dataloggerapp.components.permissions.PermissionManager
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val sharedPreferencesManager: SharedPreferencesManager = SharedPreferencesManager(context)
    private val permissionManager = PermissionManager(context, sharedPreferencesManager)

    // Replace individual permission StateFlows with a single StateFlow for all permissions
    val permissionStates = permissionManager.permissionStates

    // Keep these unchanged as they are still individual StateFlows in PermissionManager
    val permissionState = permissionManager.permissionState
    val shouldRequestPermissions = permissionManager.shouldRequestPermissions

    private val _dataInsertDelay = MutableStateFlow(sharedPreferencesManager.getDataInsertDelay())
    val dataInsertDelay: StateFlow<Long> = _dataInsertDelay.asStateFlow()

    private val _selectedTheme = MutableStateFlow(sharedPreferencesManager.getSelectedTheme())
    val selectedTheme: StateFlow<Int> = _selectedTheme.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(sharedPreferencesManager.isFirstTimeLaunch())
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _isWaitingForPermissionResult = MutableStateFlow(false)
    val isWaitingForPermissionResult: StateFlow<Boolean> = _isWaitingForPermissionResult.asStateFlow()

    // Reflects the actual foreground collection service state, polled from LocationService.
    private val _serviceRunning = MutableStateFlow(LocationService.isRunning)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _animationState = MutableStateFlow<PermissionAnimationState>(PermissionAnimationState.Idle)
    val animationState: StateFlow<PermissionAnimationState> = _animationState.asStateFlow()

    private var isPermissionInteractionOccurred = false
    private var isRequestingBackgroundLocation = false

    init {
        Log.d("MainViewModel", "ViewModel initialized")
    }

    fun completeFirstLaunch() {
        _isFirstLaunch.value = false
        sharedPreferencesManager.setFirstTimeLaunch(false)
        permissionManager.checkAndUpdatePermissions()
    }

    fun setState(key: String, value: Any) {
        when (key) {
            "data_insert_delay" -> {
                _dataInsertDelay.value = value as Long
                sharedPreferencesManager.saveDataInsertDelay(value)
                Log.d("MainViewModel", "Data insert delay set to $value ms")
            }
            "selected_theme" -> {
                _selectedTheme.value = value as Int
                sharedPreferencesManager.saveSelectedTheme(value)
                Log.d("MainViewModel", "Selected theme set to $value")
            }
        }
    }

    fun toggleFeature(feature: String, enabled: Boolean) {
        permissionManager.toggleFeature(feature, enabled)
    }

    fun checkAndUpdatePermissions() {
        permissionManager.checkAndUpdatePermissions()
    }

    fun requestPermission(permissionType: String) {
        when (permissionType) {
            "notification" -> permissionManager.requestPermission("notification")
            "location" -> permissionManager.requestPermission("location")
            "background_location" -> permissionManager.requestPermission("background_location")
            "physical_activity", "activity_recognition" -> permissionManager.requestPermission("physical_activity")
            "body_sensors" -> permissionManager.requestPermission("body_sensors")
            "phone_state" -> permissionManager.requestPermission("phone_state")
        }
        _isWaitingForPermissionResult.value = true
    }

    fun onPermissionsResult(grantResults: Map<String, Boolean>) {
        permissionManager.onPermissionsResult(grantResults)
        _isWaitingForPermissionResult.value = false
    }

    fun permissionsRequested() {
        permissionManager.permissionsRequested()
    }

    @SuppressLint("InlinedApi")
    private fun toManifestPermission(permission: String): String {
        return when (permission) {
            "notification", Manifest.permission.POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
            "location", Manifest.permission.ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
            "background_location", Manifest.permission.ACCESS_BACKGROUND_LOCATION -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            "physical_activity", "activity_recognition", Manifest.permission.ACTIVITY_RECOGNITION -> Manifest.permission.ACTIVITY_RECOGNITION
            "phone_state", Manifest.permission.READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
            "body_sensors", Manifest.permission.BODY_SENSORS -> Manifest.permission.BODY_SENSORS
            else -> permission
        }
    }

    private fun permissionAlias(permission: String): String {
        return when (permission) {
            "physical_activity" -> "activity_recognition"
            Manifest.permission.POST_NOTIFICATIONS -> "notification"
            Manifest.permission.ACCESS_FINE_LOCATION -> "location"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "background_location"
            Manifest.permission.ACTIVITY_RECOGNITION -> "activity_recognition"
            Manifest.permission.READ_PHONE_STATE -> "phone_state"
            Manifest.permission.BODY_SENSORS -> "body_sensors"
            else -> permission
        }
    }

    fun checkAndNavigate(permission: String, navController: NavController) {
        val nextScreen = when (permissionAlias(permission)) {
            "notification" -> "location_permission"
            "location", "background_location" -> "activity_recognition_permission"
            "activity_recognition" -> {
                completeFirstLaunch()
                "Main"
            }
            else -> null
        }

        nextScreen?.let { screen ->
            navController.navigate(screen) {
                if (screen == "Main") {
                    popUpTo("Welcome") { inclusive = true }
                } else {
                    popUpTo("Welcome") { saveState = true }
                }
            }
        }
    }

    /** Re-read the foreground service running flag so the Dashboard shows real state. */
    fun refreshServiceState() {
        _serviceRunning.value = LocationService.isRunning
    }

    fun onGoToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", getApplication<Application>().packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun onPermissionResult(permission: String, isGranted: Boolean, navController: NavController) {
        val manifestPermission = toManifestPermission(permission)
        permissionManager.onPermissionsResult(mapOf(manifestPermission to isGranted))
        _isWaitingForPermissionResult.value = false

        viewModelScope.launch {
            if (isGranted && isPermissionInteractionOccurred) {
                _animationState.value = PermissionAnimationState.Loading
                delay(500)
                _animationState.value = PermissionAnimationState.Celebration
                delay(1800)
            } else if (!isGranted && isPermissionInteractionOccurred) {
                _animationState.value = PermissionAnimationState.Loading
                delay(500)
                _animationState.value = PermissionAnimationState.Skip
                delay(1500)
            } else {
                _animationState.value = PermissionAnimationState.Idle
                delay(500)
            }

            _animationState.value = PermissionAnimationState.Idle
            checkAndNavigate(permissionAlias(permission), navController)
        }

        isPermissionInteractionOccurred = false
    }

    fun resetAnimationState() {
        _animationState.value = PermissionAnimationState.Idle
        isPermissionInteractionOccurred = false
    }

    fun setPermissionInteraction() {
        isPermissionInteractionOccurred = true
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("MainViewModel", "ViewModel cleared")
    }

    fun onLocationPermissionResult(isGranted: Boolean, navController: NavController) {
        permissionManager.onPermissionsResult(mapOf(Manifest.permission.ACCESS_FINE_LOCATION to isGranted))
        _isWaitingForPermissionResult.value = false

        viewModelScope.launch {
            if (isGranted && isPermissionInteractionOccurred) {
                _animationState.value = PermissionAnimationState.Loading
                delay(500)
                _animationState.value = PermissionAnimationState.Celebration
                delay(1800) // Wait for the celebration animation to complete
                _animationState.value = PermissionAnimationState.Idle

                if (!isRequestingBackgroundLocation) {
                    isRequestingBackgroundLocation = true
                    requestPermission("background_location")
                } else {
                    isRequestingBackgroundLocation = false
                    checkAndNavigate("location", navController)
                }
            } else {
                _animationState.value = PermissionAnimationState.Idle
                delay(500)
                checkAndNavigate("location", navController)
            }
        }

        isPermissionInteractionOccurred = false
    }

    fun onSkipPermission(permission: String, navController: NavController) {
        viewModelScope.launch {
            _animationState.value = PermissionAnimationState.Skip
            delay(1500) // Wait for the skip animation to complete
            _animationState.value = PermissionAnimationState.Idle

            // Update the permission state to reflect that it's been skipped
            permissionManager.onPermissionsResult(mapOf(toManifestPermission(permission) to false))

            // Navigate to the next screen
            checkAndNavigate(permissionAlias(permission), navController)
        }
    }

    fun onExitApp() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(intent)
    }

}

sealed class PermissionState {
    data object NotRequested : PermissionState()
    data object Requesting : PermissionState()
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object NeedsBackgroundPermission : PermissionState()
    data object NeedsLocationPermissionInfo : PermissionState()
}

sealed class PermissionAnimationState {
    data object Idle : PermissionAnimationState()
    data object Loading : PermissionAnimationState()
    data object Celebration : PermissionAnimationState()
    data object Skip : PermissionAnimationState()

}
