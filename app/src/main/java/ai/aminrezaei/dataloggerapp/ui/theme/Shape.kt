package ai.aminrezaei.dataloggerapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The mocks use soft-cornered rectangles throughout — never Material3's default
 * stadium/pill shapes. Overriding [Shapes] here means plain `Button`, `Card` and
 * friends pick the right corner radius up automatically, so screens don't each
 * have to pass a `shape` argument.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

/** Cards and grouped setting sections. */
val CardShape = RoundedCornerShape(12.dp)

/** Buttons, option pills and segmented-control segments. */
val ButtonShape = RoundedCornerShape(10.dp)

/** Inner fill of a selected segmented-control segment. */
val SegmentShape = RoundedCornerShape(8.dp)
