package ai.aminrezaei.dataloggerapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager

object NetworkUtils {

    fun isWifiConnected(context: Context): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) 1 else 0
    }

    fun getSignalBars(context: Context): Int? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.signalStrength?.level
        } catch (_: SecurityException) {
            null
        }
    }

    fun getSignalDbm(context: Context): Int? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val signalStrength = telephonyManager.signalStrength
            signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm
        } catch (_: SecurityException) {
            null
        }
    }

    // Legacy alias retained to avoid breaking old callers.
    fun getSignalStrength(context: Context): Int {
        return getSignalBars(context) ?: 0
    }

    fun isBluetoothConnected(context: Context): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) 1 else 0
    }

}
