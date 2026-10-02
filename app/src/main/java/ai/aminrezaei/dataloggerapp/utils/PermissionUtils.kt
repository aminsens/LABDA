package ai.aminrezaei.dataloggerapp.utils

import ai.aminrezaei.dataloggerapp.ui.state.PermissionState
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun requestLocationPermissions(context: Context): Set<String> {
        val permissionsToRequest = mutableSetOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toSet()
        return permissionsToRequest
    }

    fun requestBodySensorsPermissions(context: Context): Set<String> {
        val permissionsToRequest = setOf(Manifest.permission.BODY_SENSORS).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toSet()
        return permissionsToRequest
    }

    fun requestPhoneStatePermissions(context: Context): Set<String> {
        val permissionsToRequest = setOf(Manifest.permission.READ_PHONE_STATE).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toSet()
        return permissionsToRequest
    }

    fun requestPhysicalActivityPermissions(context: Context): Set<String> {
        val permissionsToRequest = setOf(Manifest.permission.ACTIVITY_RECOGNITION).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toSet()
        return permissionsToRequest
    }

    fun checkAllPermissions(context: Context, requiredPermissions: List<String>): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun updatePermissionState(context: Context, permissionsToRequest: Set<String>): PermissionState {
        return if (permissionsToRequest.isEmpty()) {
            PermissionState.Granted
        } else {
            PermissionState.Requesting
        }
    }
}
