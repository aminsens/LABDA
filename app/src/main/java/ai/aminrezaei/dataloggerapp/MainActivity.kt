package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.navigation.EnhancedNavigationComponent
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.ema.EmaNotificationHelper
import ai.aminrezaei.dataloggerapp.ema.EmaScheduler
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.state.PermissionState
import ai.aminrezaei.dataloggerapp.ui.state.SettingsViewModel
import ai.aminrezaei.dataloggerapp.ui.theme.DataLoggerAppTheme
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.provider.Settings
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Holds a pending EMA navigation intent so it can be consumed inside the Compose tree.
    // Updated from both onCreate (cold start) and onNewIntent (singleTop re-delivery).
    private val pendingEmaIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Core init ---
        DatabaseProvider.init(this)
        EmaNotificationHelper.createChannel(this)   // safe to call repeatedly
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // themes.xml already paints both system bars white; this makes their icons
        // dark to match, and does so at runtime as well so the bars cannot come
        // back as light-on-grey if the device restores a dark system appearance.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        sharedPreferencesManager = SharedPreferencesManager(this)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        settingsViewModel = SettingsViewModel(sharedPreferencesManager)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions -> mainViewModel.onPermissionsResult(permissions) }

        // Store any EMA intent from a cold-start notification tap
        if (intent?.getBooleanExtra(Constants.EMA_INTENT_ACTION_OPEN, false) == true) {
            pendingEmaIntent.value = intent
        }

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            // Consume a pending EMA navigation intent as soon as the nav graph is ready
            val currentPending = pendingEmaIntent.value
            LaunchedEffect(currentPending) {
                currentPending?.let { emaIntent ->
                    val rowId = emaIntent.getLongExtra(Constants.EMA_INTENT_EXTRA_PROMPT_ROW_ID, -1L)
                    if (rowId != -1L) {
                        navController.navigate("ema_response/$rowId")
                    }
                    pendingEmaIntent.value = null   // consume
                }
            }

            DataLoggerAppTheme {
                Box(Modifier.fillMaxSize()) {
                    EnhancedNavigationComponent(
                        navController     = navController,
                        snackbarHostState = remember { SnackbarHostState() },
                        mainViewModel     = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
            ConditionalBackHandler(navController, context)
        }

        // --- Permission flow observer ---
        lifecycleScope.launch {
            mainViewModel.shouldRequestPermissions.collect { permissions ->
                if (permissions.isNotEmpty()) {
                    permissionLauncher.launch(permissions.toTypedArray())
                    Log.d("MainActivity", "Requesting permissions: $permissions")
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.permissionState.collect { state ->
                if (state == PermissionState.NeedsLocationPermissionInfo) {
                    showInitialLocationPermissionDialog()
                    Log.d("MainActivity", "Showing initial location permission dialog")
                }
            }
        }

        // --- EMA scheduler observers ---
        // Re-schedule (or cancel) whenever the EMA toggle changes
        lifecycleScope.launch {
            settingsViewModel.emaPromptsLogging.collect { enabled ->
                if (enabled) {
                    val hours = settingsViewModel.emaIntervalHours.value
                    EmaScheduler.schedule(this@MainActivity, hours)
                    Log.d("MainActivity", "EMA scheduled: interval=${hours}h")
                } else {
                    EmaScheduler.cancel(this@MainActivity)
                    Log.d("MainActivity", "EMA scheduler cancelled")
                }
            }
        }

        // Re-schedule when interval changes while EMA is already ON
        lifecycleScope.launch {
            settingsViewModel.emaIntervalHours.collect { hours ->
                if (settingsViewModel.emaPromptsLogging.value) {
                    EmaScheduler.schedule(this@MainActivity, hours)
                    Log.d("MainActivity", "EMA interval updated: ${hours}h — rescheduled")
                }
            }
        }

        Log.d("MainActivity", "Activity created")
    }

    /**
     * Called when the activity is already running (launchMode=singleTop) and a new EMA
     * notification tap brings it to the foreground. We store the intent so the Compose
     * LaunchedEffect can pick it up and navigate.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Constants.EMA_INTENT_ACTION_OPEN, false)) {
            pendingEmaIntent.value = intent
            Log.d("MainActivity", "onNewIntent: EMA intent received")
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.checkAndUpdatePermissions()
        maybePromptBatteryOptimizationExemption()
        Log.d("MainActivity", "Activity resumed")
    }

    private fun showInitialLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage(
                "This app requires location permission to function properly. " +
                "You will be prompted to grant location permission. After granting, " +
                "please enable 'Allow all the time' in app settings for better functionality."
            )
            .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                mainViewModel.requestPermission("location")
                Log.d("MainActivity", "Initial location permission dialog confirmed")
            }
            .show()
    }

    private fun maybePromptBatteryOptimizationExemption() {
        if (sharedPreferencesManager.isFirstTimeLaunch()) return
        if (sharedPreferencesManager.isBatteryOptimizationPromptShown()) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            sharedPreferencesManager.setBatteryOptimizationPromptShown(true)
            return
        }

        sharedPreferencesManager.setBatteryOptimizationPromptShown(true)
        AlertDialog.Builder(this)
            .setTitle("Allow Unrestricted Battery Use")
            .setMessage(
                "To keep continuous background data collection running, allow LABDA to ignore battery optimization."
            )
            .setPositiveButton("Open Settings") { _, _ -> openBatteryOptimizationSettings() }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun openBatteryOptimizationSettings() {
        val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(settingsIntent)
        } catch (error: Exception) {
            Log.w("MainActivity", "Failed to open battery optimization settings", error)
            startActivity(fallbackIntent)
        }
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun ConditionalBackHandler(navController: NavHostController, context: Context) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val lastBackPressTime = remember { mutableLongStateOf(0L) }
    val doubleTapInterval = TimeUnit.SECONDS.toMillis(2)

    val isOnboardingScreen = when (currentRoute) {
        "Welcome",
        "StudyInfo",
        "notification_permission",
        "location_permission",
        "activity_recognition_permission" -> true
        else -> false
    }

    BackHandler(enabled = isOnboardingScreen) {
        // Disabled on onboarding screens
    }

    val isTopLevelDestination = currentRoute in setOf("Main", "Data", "Settings")

    BackHandler(enabled = !isOnboardingScreen && isTopLevelDestination) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime.longValue < doubleTapInterval) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPressTime.longValue = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }
}
