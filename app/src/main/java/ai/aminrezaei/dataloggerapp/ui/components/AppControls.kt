package ai.aminrezaei.dataloggerapp.ui.components

import ai.aminrezaei.dataloggerapp.ui.theme.ButtonShape
import ai.aminrezaei.dataloggerapp.ui.theme.HairlineDivider
import ai.aminrezaei.dataloggerapp.ui.theme.OutlineBorder
import ai.aminrezaei.dataloggerapp.ui.theme.SegmentShape
import ai.aminrezaei.dataloggerapp.ui.theme.SwitchGreen
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ControlHeight = 52.dp

// Material's default button padding is 24dp each side. On a half-width button that
// left too little room for "Start Logging", which wrapped onto a second line and
// was then clipped by the fixed height.
private val ButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** Green switch matching the mock. Material's default tracks the theme primary. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SwitchGreen,
            checkedBorderColor = SwitchGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFC8CDD4),
            uncheckedBorderColor = Color(0xFFC8CDD4)
        )
    )
}

/**
 * Connected multi-segment picker (the EMA interval control).
 *
 * A single bordered bar with the selected option filled — not a row of detached
 * pills, which is what the earlier build rendered.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(ButtonShape)
            .border(1.dp, OutlineBorder, ButtonShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(SegmentShape)
                    .background(if (selected) accent else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else TextPrimary
                )
            }

            // Hairline between two adjacent unselected segments only; a filled
            // segment already provides its own visual separation.
            val nextSelected = index + 1 == selectedIndex
            if (index < options.lastIndex && !selected && !nextSelected) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .background(HairlineDivider)
                )
            }
        }
    }
}

/** Filled action button. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(ControlHeight),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        contentPadding = ButtonPadding
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Outlined action button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = TextPrimary,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(ControlHeight),
        enabled = enabled,
        shape = ButtonShape,
        border = BorderStroke(1.dp, OutlineBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = ButtonPadding
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Ring indicator on the Dashboard status banner: a filled disc with a punched-out
 * centre when active, a flat grey disc when idle.
 */
@Composable
fun StatusRing(
    active: Boolean,
    activeColor: Color,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) activeColor else TextTertiary),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(size * 0.34f)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/** Row of buttons sharing the available width evenly. */
@Composable
fun ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
