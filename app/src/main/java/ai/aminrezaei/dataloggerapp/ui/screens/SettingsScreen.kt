package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.ui.components.BadgeStyle
import ai.aminrezaei.dataloggerapp.ui.components.NavRow
import ai.aminrezaei.dataloggerapp.ui.components.RowDivider
import ai.aminrezaei.dataloggerapp.ui.components.ScreenScaffold
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.components.SectionHeader
import ai.aminrezaei.dataloggerapp.ui.components.SegmentedControl
import ai.aminrezaei.dataloggerapp.ui.components.ToggleRow
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.state.SettingsViewModel
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.EmaPurple
import ai.aminrezaei.dataloggerapp.ui.theme.SuccessGreen
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private val INTERVAL_OPTIONS = listOf(1, 2, 4, 8)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    selectedTheme: Int,
    navController: NavController
) {
    val context = LocalContext.current

    // Read from the installed package rather than BuildConfig so the row cannot
    // drift from what is actually on the device.
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    val accelerometer by viewModel.accelerometerLogging.collectAsState()
    val location by viewModel.locationLogging.collectAsState()
    val emaPrompts by viewModel.emaPromptsLogging.collectAsState()
    val emaIntervalHours by viewModel.emaIntervalHours.collectAsState()
    val emaQuietStart by viewModel.emaQuietStart.collectAsState()
    val emaQuietEnd by viewModel.emaQuietEnd.collectAsState()

    // Grouped context toggle - collect all sub-toggles so the switch reflects real state.
    val steps by viewModel.stepsLogging.collectAsState()
    val battery by viewModel.batteryLogging.collectAsState()
    val screen by viewModel.screenLogging.collectAsState()
    val wifiConnected by viewModel.wifiConnectedLogging.collectAsState()
    val wifiHash by viewModel.wifiHashLogging.collectAsState()
    val signal by viewModel.signalLogging.collectAsState()
    val ringerMode by viewModel.ringerModeLogging.collectAsState()
    val audioOutput by viewModel.audioOutputLogging.collectAsState()
    val activityRecognition by viewModel.activityRecognitionLogging.collectAsState()
    val light by viewModel.lightLogging.collectAsState()
    val proximity by viewModel.proximityLogging.collectAsState()
    val contextData = listOf(
        steps, battery, screen, wifiConnected, wifiHash, signal,
        ringerMode, audioOutput, activityRecognition, light, proximity
    ).all { it }

    val permissionStates by mainViewModel.permissionStates.collectAsState()

    // Settings is the one tab that keeps the overflow menu: everything in it is a
    // shortcut to somewhere this screen already links to, so anywhere else it was
    // pure duplication.
    ScreenScaffold(title = "Settings", navController = navController, showMenu = true) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("Data collection")
        SectionCard {
            ToggleRow(
                icon = Icons.Filled.DirectionsWalk,
                accent = Accents.Movement,
                title = "Movement data",
                subtitle = "Accelerometer readings from your phone",
                checked = accelerometer,
                onCheckedChange = { checked ->
                    viewModel.toggleAccelerometerLogging(checked)
                    Toast.makeText(context, "Movement data ${if (checked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
            RowDivider(inset = 16.dp)
            ToggleRow(
                icon = Icons.Filled.LocationOn,
                accent = Accents.Location,
                title = "Location data",
                subtitle = "GPS position while collection is active",
                checked = location,
                onCheckedChange = { checked ->
                    viewModel.toggleLocationLogging(checked)
                    Toast.makeText(context, "Location data ${if (checked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
            RowDivider(inset = 16.dp)
            ToggleRow(
                icon = Icons.Filled.PhoneAndroid,
                accent = Accents.Context,
                title = "Context data",
                subtitle = "Battery, screen, charging, Wi-Fi, activity, steps, light and proximity",
                checked = contextData,
                onCheckedChange = { checked ->
                    viewModel.toggleContextDataLogging(checked)
                    Toast.makeText(context, "Context data ${if (checked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader("Check-ins")
        SectionCard {
            ToggleRow(
                icon = Icons.Filled.Chat,
                accent = Accents.Ema,
                title = "EMA prompts",
                subtitle = "Short check-in surveys during the day",
                checked = emaPrompts,
                onCheckedChange = { checked ->
                    viewModel.toggleEmaPromptsLogging(checked)
                    Toast.makeText(context, "EMA prompts ${if (checked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            )
            RowDivider(inset = 16.dp)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EMA interval",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedControl(
                    options = INTERVAL_OPTIONS.map { "${it}h" },
                    selectedIndex = INTERVAL_OPTIONS.indexOf(emaIntervalHours).coerceAtLeast(0),
                    onSelect = { index -> viewModel.setEmaIntervalHours(INTERVAL_OPTIONS[index]) },
                    accent = EmaPurple,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            RowDivider(inset = 16.dp)
            QuietHoursRow(
                quietStart = emaQuietStart,
                quietEnd = emaQuietEnd,
                onSetQuietStart = viewModel::setEmaQuietStart,
                onSetQuietEnd = viewModel::setEmaQuietEnd
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader("Permissions")
        SectionCard {
            PermissionNavRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                isAllowed = permissionStates.notificationPermission == true,
                onClick = { navController.navigate("PermissionsSettings") }
            )
            RowDivider(inset = 16.dp)
            PermissionNavRow(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                isAllowed = permissionStates.locationPermission == true &&
                    permissionStates.backgroundLocationPermission == true,
                onClick = { navController.navigate("PermissionsSettings") }
            )
            RowDivider(inset = 16.dp)
            PermissionNavRow(
                icon = Icons.Filled.DirectionsRun,
                title = "Activity",
                isAllowed = permissionStates.activityRecognitionPermission == true,
                onClick = { navController.navigate("PermissionsSettings") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader("About")
        SectionCard {
            // One destination, not two: "About LABDA" and "Data & privacy" used to
            // be separate screens that said the same thing in different words.
            NavRow(
                icon = Icons.Filled.Shield,
                accent = Accents.Location,
                badgeStyle = BadgeStyle.Tinted,
                title = "About LABDA",
                subtitle = "What LABDA collects, where it is stored, and your choices",
                onClick = { navController.navigate("StudyInfo") }
            )
            RowDivider(inset = 16.dp)
            NavRow(
                title = "Version",
                value = versionName,
                showChevron = false,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isAllowed: Boolean,
    onClick: () -> Unit
) {
    NavRow(
        icon = icon,
        accent = Accents.Neutral,
        badgeStyle = BadgeStyle.Tinted,
        title = title,
        value = if (isAllowed) "Allowed" else "Not allowed",
        valueColor = if (isAllowed) SuccessGreen else TextSecondary,
        onClick = onClick
    )
}

@Composable
private fun QuietHoursRow(
    quietStart: String,
    quietEnd: String,
    onSetQuietStart: (String) -> Unit,
    onSetQuietEnd: (String) -> Unit
) {
    val context = LocalContext.current

    fun showEndPicker() {
        val parts = quietEnd.split(":").mapNotNull { it.toIntOrNull() }
        val h = parts.getOrElse(0) { 7 }
        val m = parts.getOrElse(1) { 0 }
        TimePickerDialog(context, { _, hour, minute ->
            onSetQuietEnd("%02d:%02d".format(hour, minute))
        }, h, m, true).show()
    }

    fun showStartPicker() {
        val parts = quietStart.split(":").mapNotNull { it.toIntOrNull() }
        val h = parts.getOrElse(0) { 22 }
        val m = parts.getOrElse(1) { 0 }
        TimePickerDialog(context, { _, hour, minute ->
            onSetQuietStart("%02d:%02d".format(hour, minute))
            showEndPicker()
        }, h, m, true).show()
    }

    NavRow(
        title = "Quiet hours",
        value = "$quietStart – $quietEnd",
        onClick = { showStartPicker() }
    )
}
