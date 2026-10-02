package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.ui.components.BadgeStyle
import ai.aminrezaei.dataloggerapp.ui.components.NavRow
import ai.aminrezaei.dataloggerapp.ui.components.RowDivider
import ai.aminrezaei.dataloggerapp.ui.components.ScreenScaffold
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.components.SectionHeader
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.HairlineDivider
import ai.aminrezaei.dataloggerapp.ui.theme.SegmentShape
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/** How often the record counts are re-read while this tab is on screen. */
private const val COUNT_REFRESH_MS = 3_000L

/**
 * Collected Data.
 *
 * There was no mock for this tab, so it is composed entirely from the language of
 * the other three: grey section labels, white rounded cards, the shared category
 * accents, and rows that behave the same way as Settings rows. The only row that
 * carries a chevron is the one that actually leads somewhere.
 */
@Composable
fun DataScreen(navController: NavController) {
    var movementCount by remember { mutableStateOf(0) }
    var locationCount by remember { mutableStateOf(0) }
    var contextCount by remember { mutableStateOf(0) }
    var emaCount by remember { mutableStateOf(0) }

    // Collection runs in a foreground service, so the counts go stale while the
    // tab is open. Re-reading them on a timer keeps the totals live instead of
    // frozen at whatever they were when the tab was first opened.
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val db = DatabaseProvider.getDatabase()
                movementCount = db.accelerometerDao().getCount()
                locationCount = db.locationDao().getCount()
                contextCount = db.deviceStateDao().getCount()
                emaCount = db.emaResponseDao().getAnsweredCount()
            }
            delay(COUNT_REFRESH_MS)
        }
    }

    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
    fun records(count: Int) =
        "${numberFormat.format(count)} ${if (count == 1) "record" else "records"}"

    val slices = listOf(
        Slice("Movement", movementCount, Accents.Movement),
        Slice("Location", locationCount, Accents.Location),
        Slice("Context", contextCount, Accents.Context),
        Slice("Check-ins", emaCount, Accents.Ema)
    )
    val total = slices.sumOf { it.count }

    ScreenScaffold(title = "Collected Data") {
        Spacer(modifier = Modifier.height(16.dp))

        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = numberFormat.format(total),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                CompositionBar(slices = slices, total = total)
                Spacer(modifier = Modifier.height(12.dp))
                Legend(slices = slices)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader("By category")
        SectionCard {
            NavRow(
                icon = Icons.Filled.DirectionsWalk,
                accent = Accents.Movement,
                badgeStyle = BadgeStyle.Solid,
                title = "Movement",
                subtitle = "Accelerometer",
                value = records(movementCount),
                showChevron = false,
                onClick = {}
            )
            RowDivider(inset = 16.dp)
            NavRow(
                icon = Icons.Filled.LocationOn,
                accent = Accents.Location,
                badgeStyle = BadgeStyle.Solid,
                title = "Location",
                subtitle = "GPS",
                value = records(locationCount),
                showChevron = false,
                onClick = {}
            )
            RowDivider(inset = 16.dp)
            NavRow(
                icon = Icons.Filled.PhoneAndroid,
                accent = Accents.Context,
                badgeStyle = BadgeStyle.Solid,
                title = "Context",
                subtitle = "Device & usage",
                value = records(contextCount),
                showChevron = false,
                onClick = {}
            )
            RowDivider(inset = 16.dp)
            NavRow(
                icon = Icons.Filled.Chat,
                accent = Accents.Ema,
                badgeStyle = BadgeStyle.Solid,
                title = "Check-in responses",
                subtitle = "Tap to see your answers",
                value = numberFormat.format(emaCount),
                showChevron = true,
                onClick = { navController.navigate("EmaHistory") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class Slice(
    val label: String,
    val count: Int,
    val accent: CategoryAccent
)

/**
 * Smallest share of the bar a non-empty category may occupy. Accelerometer
 * records outnumber everything else by three orders of magnitude, so a strictly
 * proportional bar rendered as a single blue block; this keeps the other
 * categories visible as slivers.
 */
private const val MIN_SLICE_FRACTION = 0.03f

/** Proportional stacked bar showing how the total splits across categories. */
@Composable
private fun CompositionBar(slices: List<Slice>, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(SegmentShape)
            .background(HairlineDivider)
    ) {
        if (total > 0) {
            slices.filter { it.count > 0 }.forEach { slice ->
                val share = (slice.count.toFloat() / total).coerceAtLeast(MIN_SLICE_FRACTION)
                Box(
                    modifier = Modifier
                        .weight(share)
                        .fillMaxHeight()
                        .background(slice.accent.solid)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(slices: List<Slice>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(slice.accent.solid)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}
