package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseEntity
import ai.aminrezaei.dataloggerapp.ui.components.DetailTopBar
import ai.aminrezaei.dataloggerapp.ui.components.IconBadge
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Past check-ins.
 *
 * Reached from the "Check-in responses" row on the Collected Data tab, and landed
 * on directly after submitting a check-in so an answer is never swallowed.
 */
@Composable
fun EmaHistoryScreen(navController: NavController) {
    var responses by remember { mutableStateOf<List<EmaResponseEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            responses = DatabaseProvider.getDatabase().emaResponseDao()
                .getAll()
                .filter { it.responseTimestamp != null }
                .sortedByDescending { it.responseTimestamp }
            loaded = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        DetailTopBar(title = "Check-in responses", onBack = { navController.popBackStack() })

        if (loaded && responses.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(responses, key = { it.id }) { response ->
                    ResponseCard(response)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(icon = Icons.Filled.Chat, accent = Accents.Ema, size = 64.dp, iconSize = 30.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No check-ins yet",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Answers you submit will be listed here.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResponseCard(response: EmaResponseEntity) {
    SectionCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = response.responseTimestamp?.let { formatWhen(it) } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                response.latencySeconds?.takeIf { it > 0 }?.let { latency ->
                    Text(
                        text = "answered in ${formatLatency(latency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            val labels = listOfNotNull(
                response.activityLabel?.let { it to Accents.Ema },
                response.locationLabel?.let { it to Accents.Location },
                response.socialLabel?.let { it to Accents.Movement }
            )

            if (labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    labels.forEach { (text, accent) -> LabelChip(text, accent) }
                }
            }

            response.optionalTags?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LabelChip(text: String, accent: CategoryAccent) {
    Box(
        modifier = Modifier
            .clip(SegmentShape)
            .background(accent.light)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent.solid
        )
    }
}

private fun formatWhen(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(timestampMs))

    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> "Today, $time"
        sameYear && dayDiff == 1 -> "Yesterday, $time"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(timestampMs))
    }
}

private fun formatLatency(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> "${seconds / 3600}h"
}
