package ai.aminrezaei.dataloggerapp.ui.components

import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CardShape
import ai.aminrezaei.dataloggerapp.ui.theme.CardSurface
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.HairlineDivider
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Standard page frame for the bottom-nav destinations.
 *
 * Note there is no `systemBarsPadding()` here on purpose. The `Scaffold` in the
 * navigation component already supplies window insets via its `innerPadding`;
 * screens that also applied their own inset padding ended up with the status bar
 * and navigation bar counted twice, which is what produced the thick dead space
 * above the title and below the bottom nav.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    showMenu: Boolean = false,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        AppTopBar(title = title, navController = navController, showMenu = showMenu)

        val body = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)

        Column(modifier = if (scrollable) body.verticalScroll(rememberScrollState()) else body) {
            content()
        }
    }
}

/** Page frame for pushed detail screens. */
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        DetailTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

/** Small grey label above a group of cards, e.g. "Data collection". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextTertiary,
        modifier = modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
}

/** White rounded container that groups related rows. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Gap between a row title and the supporting line under it.
 *
 * Two stacked Text composables sit flush against each other — the only thing
 * separating them is whatever leading their line heights happen to leave, which
 * is a couple of dp and reads as one wrapped paragraph rather than a title with
 * a caption. Every title/subtitle pair uses this so the spacing stays uniform.
 */
val SubtitleGap = 3.dp

/** Hairline rule separating rows inside a [SectionCard]. */
@Composable
fun RowDivider(inset: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(1.dp)
            .background(HairlineDivider)
    )
}

/**
 * Tappable row: optional leading badge, title, optional subtitle, optional
 * trailing value, chevron. Covers permission rows, quiet hours, Data & privacy
 * and the Collected Data categories.
 */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: CategoryAccent = Accents.Neutral,
    badgeStyle: BadgeStyle = BadgeStyle.Tinted,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = TextSecondary,
    showChevron: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconBadge(icon = icon, accent = accent, style = badgeStyle, size = 40.dp, iconSize = 20.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(SubtitleGap))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Row with a leading badge and a trailing switch. */
@Composable
fun ToggleRow(
    icon: ImageVector,
    accent: CategoryAccent,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, accent = accent, size = 40.dp, iconSize = 20.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(SubtitleGap))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Heading + body paragraph block used by the long-form info screens. */
@Composable
fun InfoBlock(heading: String, body: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = heading, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
