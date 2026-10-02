package ai.aminrezaei.dataloggerapp.ui.components

import ai.aminrezaei.dataloggerapp.R
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.HairlineDivider
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TopBarSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Header used on Dashboard, Settings, Collected Data and the EMA Check-in screen:
 * app logo, title, and — on Settings only — an overflow menu.
 *
 * [title] is the name of the page, not the app: only the Dashboard says "LABDA".
 * The other tabs carry their own heading here instead of repeating the app name
 * above a second, larger heading in the body.
 *
 * The overflow menu needs both a [navController] to navigate with and [showMenu],
 * so screens that have no use for it do not carry a duplicate entry point.
 */
@Composable
fun AppTopBar(
    title: String = "LABDA",
    navController: NavController? = null,
    showMenu: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(TopBarSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogo(size = 40.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (navController != null && showMenu) {
                OverflowMenu(navController = navController)
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HairlineDivider)
        )
    }
}

/**
 * Header for pushed detail screens (Study info, Data & privacy, permission
 * detail, EMA history) — back arrow instead of the logo, same surface and rule.
 */
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(TopBarSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HairlineDivider)
        )
    }
}

/**
 * The visible artwork occupies roughly 55% of an adaptive-icon foreground; the
 * rest is the transparent safe zone the launcher crops into. Drawing the bitmap
 * at this multiple of the requested box makes the mark itself fill the box, with
 * the dead margin simply overflowing (harmlessly, since it is transparent).
 */
private const val LogoArtScale = 1.8f

/**
 * The LABDA wordmark graphic, sized so that [size] is the size of the *visible*
 * mark rather than of the padded icon canvas.
 *
 * Deliberately points at the raster adaptive foreground: both `R.mipmap.ic_launcher`
 * and `R.drawable.labda` are adaptive-icon XML, which `painterResource` cannot
 * decode — it throws at runtime.
 */
@Composable
fun AppLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_adaptive_fore),
            contentDescription = "LABDA logo",
            modifier = Modifier.requiredSize(size * LogoArtScale)
        )
    }
}

@Composable
private fun OverflowMenu(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "Data & privacy" used to sit here too, but it led to a screen that
            // repeated About LABDA almost word for word; the two are now one entry.
            MenuEntry("About LABDA") {
                expanded = false
                navController.navigate("StudyInfo")
            }
            MenuEntry("Permissions") {
                expanded = false
                navController.navigate("PermissionsSettings")
            }
        }
    }
}

@Composable
private fun MenuEntry(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        },
        onClick = onClick
    )
}

/** How an [IconBadge] renders its accent. */
enum class BadgeStyle {
    /** Solid accent circle, white glyph — the mock default for category icons. */
    Solid,

    /** Pale tinted circle, accent-coloured glyph. Used on the Welcome setup rows. */
    Tinted
}

/** Circular category icon used across Dashboard, Settings, EMA and Collected Data. */
@Composable
fun IconBadge(
    icon: ImageVector,
    accent: CategoryAccent,
    modifier: Modifier = Modifier,
    style: BadgeStyle = BadgeStyle.Solid,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null
) {
    val container = when (style) {
        BadgeStyle.Solid -> accent.solid
        BadgeStyle.Tinted -> accent.light
    }
    val tint = when (style) {
        BadgeStyle.Solid -> Color.White
        BadgeStyle.Tinted -> accent.solid
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
