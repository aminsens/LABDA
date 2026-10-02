package ai.aminrezaei.dataloggerapp.components.mainlogic

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.permissions.LocationService
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.ui.components.AppTopBar
import ai.aminrezaei.dataloggerapp.ui.components.ButtonRow
import ai.aminrezaei.dataloggerapp.ui.components.IconBadge
import ai.aminrezaei.dataloggerapp.ui.components.PrimaryButton
import ai.aminrezaei.dataloggerapp.ui.components.SecondaryButton
import ai.aminrezaei.dataloggerapp.ui.components.StatusRing
import ai.aminrezaei.dataloggerapp.ui.components.SubtitleGap
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CardShape
import ai.aminrezaei.dataloggerapp.ui.theme.CardSurface
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.StatusBannerActive
import ai.aminrezaei.dataloggerapp.ui.theme.StatusBannerActiveBorder
import ai.aminrezaei.dataloggerapp.ui.theme.StatusBannerIdle
import ai.aminrezaei.dataloggerapp.ui.theme.StatusBannerIdleBorder
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
import ai.aminrezaei.dataloggerapp.ui.theme.StopRed
import ai.aminrezaei.dataloggerapp.ui.theme.SuccessGreen
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DashboardState(
    val serviceRunning: Boolean = false,
    val collectingSinceMs: Long = 0L,
    val measuredHz: Float = 0f,
    val accelerometerRows: Int = 0,
    val locationRows: Int = 0,
    val deviceStateRows: Int = 0,
    val emaAnsweredRows: Int = 0
)

/**
 * Dashboard.
 *
 * Deliberately does not scroll: everything the screen shows has to fit inside the
 * viewport. The two stat rows carry the layout weight, so they absorb whatever
 * height is left over after the banner, the buttons and the storage card.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferencesManager = remember { SharedPreferencesManager(context) }

    // Poll the real foreground-service state while this screen is visible.
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshServiceState()
            delay(1_000)
        }
    }

    val summary by produceState(initialValue = DashboardState(), context) {
        while (true) {
            value = loadDashboardState(context)
            delay(5_000)
        }
    }

    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        // The Dashboard is the only place the app name appears in the bar; the
        // other tabs name themselves. No overflow menu here — it lives in Settings.
        AppTopBar(title = "LABDA")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusBanner(
                running = serviceRunning,
                collectingSinceMs = summary.collectingSinceMs
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Filled.DirectionsWalk,
                    accent = Accents.Movement,
                    title = "Movement",
                    subtitle = "Accelerometer",
                    value = numberFormat.format(summary.accelerometerRows),
                    valueLabel = "Samples",
                    onClick = { navigateToData(navController) }
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Filled.LocationOn,
                    accent = Accents.Location,
                    title = "Location",
                    subtitle = "GPS",
                    value = numberFormat.format(summary.locationRows),
                    valueLabel = "Points",
                    onClick = { navigateToData(navController) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Filled.PhoneAndroid,
                    accent = Accents.Context,
                    title = "Context",
                    subtitle = "Device & usage",
                    value = numberFormat.format(summary.deviceStateRows),
                    valueLabel = "Events",
                    onClick = { navigateToData(navController) }
                )
                StatCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Filled.Chat,
                    accent = Accents.Ema,
                    title = "EMA",
                    subtitle = "Surveys",
                    value = numberFormat.format(summary.emaAnsweredRows),
                    valueLabel = "Completed",
                    onClick = { navController.navigate("EmaHistory") }
                )
            }

            ButtonRow {
                SecondaryButton(
                    text = "Start Logging",
                    icon = Icons.Filled.PlayArrow,
                    enabled = !serviceRunning,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sharedPreferencesManager.saveCollectionStartedAtMs(System.currentTimeMillis())
                        val serviceIntent = Intent(context, LocationService::class.java)
                        context.startForegroundService(serviceIntent)
                        viewModel.refreshServiceState()
                    }
                )
                PrimaryButton(
                    text = "Stop Logging",
                    icon = Icons.Filled.Stop,
                    containerColor = StopRed,
                    enabled = serviceRunning,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sharedPreferencesManager.saveCollectionStartedAtMs(0L)
                        val serviceIntent = Intent(context, LocationService::class.java)
                        context.stopService(serviceIntent)
                        viewModel.refreshServiceState()
                    }
                )
            }

            LocalStorageCard()
        }
    }
}

private fun navigateToData(navController: NavHostController) {
    navController.navigate("Data") {
        popUpTo("Main")
        launchSingleTop = true
    }
}

@Composable
private fun StatusBanner(running: Boolean, collectingSinceMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (running) StatusBannerActive else StatusBannerIdle
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (running) StatusBannerActiveBorder else StatusBannerIdleBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusRing(active = running, activeColor = SuccessGreen, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (running) "Data collection is active" else "Data collection is not active",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (running) SuccessGreen else TextSecondary
                )
                if (running && collectingSinceMs > 0L) {
                    Text(
                        text = "Collecting since ${formatTimeOnly(collectingSinceMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * One tile of the 2x2 grid.
 *
 * The badge and chevron share the top line so the title and subtitle get the full
 * card width — with the chevron sitting beside them, "Accelerometer" wrapped.
 */
@Composable
private fun StatCard(
    icon: ImageVector,
    accent: CategoryAccent,
    title: String,
    subtitle: String,
    value: String,
    valueLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = icon, accent = accent, size = 38.dp, iconSize = 20.dp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(SubtitleGap))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent.solid
                )
                Spacer(modifier = Modifier.height(SubtitleGap))
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun LocalStorageCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stored on this device",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(SubtitleGap))
                Text(
                    text = "No cloud sync is configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private suspend fun loadDashboardState(context: android.content.Context): DashboardState {
    return withContext(Dispatchers.IO) {
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()
        val prefs = SharedPreferencesManager(context)
        DashboardState(
            serviceRunning = LocationService.isRunning,
            collectingSinceMs = prefs.getCollectionStartedAtMs(),
            measuredHz = prefs.getMeasuredAccelerometerHz(),
            accelerometerRows = db.accelerometerDao().getCount(),
            locationRows = db.locationDao().getCount(),
            deviceStateRows = db.deviceStateDao().getCount(),
            emaAnsweredRows = db.emaResponseDao().getAnsweredCount()
        )
    }
}

private fun formatTimeOnly(timestampMs: Long): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(Date(timestampMs))
}
