package ai.aminrezaei.dataloggerapp.ui.components

import ai.aminrezaei.dataloggerapp.ui.theme.CardSurface
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.OutlineBorder
import ai.aminrezaei.dataloggerapp.ui.theme.SegmentShape
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Selectable answer chip for the EMA questions.
 *
 * Soft-cornered rectangle, not an oval: unselected is a plain outlined chip,
 * selected picks up the section accent as a pale fill, a coloured border,
 * coloured text and a filled circular check.
 *
 * The label takes the free width so the check sits on the trailing edge, which
 * keeps a column of chips aligned instead of each check floating at a different
 * offset depending on how long its label is.
 */
@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: CategoryAccent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(SegmentShape)
            .background(if (selected) accent.light else CardSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.solid else OutlineBorder,
                shape = SegmentShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) accent.solid else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent.solid),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
