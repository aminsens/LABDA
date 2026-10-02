package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.ema.EmaNotificationHelper
import ai.aminrezaei.dataloggerapp.ui.components.ButtonRow
import ai.aminrezaei.dataloggerapp.ui.components.IconBadge
import ai.aminrezaei.dataloggerapp.ui.components.OptionChip
import ai.aminrezaei.dataloggerapp.ui.components.PrimaryButton
import ai.aminrezaei.dataloggerapp.ui.components.ScreenScaffold
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.components.SecondaryButton
import ai.aminrezaei.dataloggerapp.ui.components.SubtitleGap
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.EmaPurple
import ai.aminrezaei.dataloggerapp.ui.theme.OutlineBorder
import ai.aminrezaei.dataloggerapp.ui.theme.SegmentShape
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// ExtraSensory-aligned label options
// ---------------------------------------------------------------------------

private val ACTIVITY_OPTIONS = listOf(
    "Sitting", "Standing", "Walking", "Running"
)

private val LOCATION_OPTIONS = listOf(
    "At home", "At work", "Outside", "In transit"
)

private val SOCIAL_OPTIONS = listOf(
    "Alone", "Family", "Friends", "Co-workers"
)

// ---------------------------------------------------------------------------
// EmaResponseScreen
// ---------------------------------------------------------------------------

/**
 * Full-screen EMA response UI.
 *
 * Opened when the participant taps the EMA heads-up notification.
 * Loads the prompt row from the DB to retrieve [promptTimestamp], then lets the
 * participant select an activity label, location label, and social context before
 * submitting. Latency is computed automatically on submit.
 *
 * @param promptRowId   Room-generated row ID passed through the navigation argument.
 * @param navController Used to pop back after submit/dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmaResponseScreen(
    promptRowId: Long,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { DatabaseProvider.getDatabase().emaResponseDao() }

    // Loaded from DB; used for latency calculation
    var promptTimestamp by remember { mutableLongStateOf(0L) }

    // User selections
    var selectedActivity by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var selectedSocial   by remember { mutableStateOf<String?>(null) }
    var optionalTags     by remember { mutableStateOf("") }

    // Load prompt timestamp once
    LaunchedEffect(promptRowId) {
        withContext(Dispatchers.IO) {
            promptTimestamp = dao.getById(promptRowId)?.promptTimestamp ?: System.currentTimeMillis()
        }
    }

    ScreenScaffold(title = "EMA Check-in") {
        Spacer(modifier = Modifier.height(14.dp))

        // Intro card
        SectionCard {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = Icons.Filled.Chat,
                    accent = Accents.Ema,
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "How are you doing right now?",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(SubtitleGap))
                    Text(
                        text = "Your response helps us understand your context.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 1 — Activity
        LabelSection(
            title = "1. What are you doing?",
            options = ACTIVITY_OPTIONS,
            selected = selectedActivity,
            onSelect = { selectedActivity = it },
            accent = Accents.Ema
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2 — Location context
        LabelSection(
            title = "2. Where are you?",
            options = LOCATION_OPTIONS,
            selected = selectedLocation,
            onSelect = { selectedLocation = it },
            accent = Accents.Location
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3 — Social context
        LabelSection(
            title = "3. Who are you with?",
            options = SOCIAL_OPTIONS,
            selected = selectedSocial,
            onSelect = { selectedSocial = it },
            accent = Accents.Movement
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Optional notes
        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.width(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Optional notes",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(SubtitleGap))
                        Text(
                            text = "Add any additional context (optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = optionalTags,
                    onValueChange = { if (it.length <= 200) optionalTags = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SegmentShape,
                    placeholder = {
                        Text(
                            text = "Enter your notes here…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EmaPurple,
                        unfocusedBorderColor = OutlineBorder
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                Text(
                    text = "${optionalTags.length}/200",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action row
        ButtonRow {
            SecondaryButton(
                text = "Skip",
                icon = Icons.Filled.Close,
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        runCatching { dao.markDismissed(promptRowId) }
                        EmaNotificationHelper.cancelNotification(context, promptRowId)
                        withContext(Dispatchers.Main) { navController.popBackStack() }
                    }
                }
            )
            PrimaryButton(
                text = "Submit",
                icon = Icons.Filled.Check,
                containerColor = EmaPurple,
                modifier = Modifier.weight(1f),
                onClick = {
                    val responseTs = System.currentTimeMillis()
                    val latency = if (promptTimestamp > 0L)
                        ((responseTs - promptTimestamp) / 1000).toInt()
                    else null

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            dao.updateResponse(
                                id          = promptRowId,
                                responseTs  = responseTs,
                                activity    = selectedActivity,
                                location    = selectedLocation,
                                social      = selectedSocial,
                                tags        = optionalTags.trim().ifEmpty { null },
                                latency     = latency ?: 0
                            )
                        }
                        EmaNotificationHelper.cancelNotification(context, promptRowId)
                        withContext(Dispatchers.Main) {
                            // Land on the history list so a submitted check-in is
                            // visible straight away instead of vanishing.
                            navController.popBackStack()
                            navController.navigate("EmaHistory")
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Reusable option-group section
// ---------------------------------------------------------------------------

/**
 * Options are laid out as an even two-column grid rather than left to right with
 * wrapping. A FlowRow packed the four answers 3-then-1, which read as one answer
 * being different in kind from the other three.
 */
@Composable
private fun LabelSection(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    accent: CategoryAccent
) {
    SectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { option ->
                            OptionChip(
                                text = option,
                                selected = selected == option,
                                onClick = { onSelect(option) },
                                accent = accent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keeps a trailing odd option half-width instead of
                        // stretching it across the whole row.
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
