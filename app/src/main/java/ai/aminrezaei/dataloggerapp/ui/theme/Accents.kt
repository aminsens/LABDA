package ai.aminrezaei.dataloggerapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A category's paired colours: [solid] fills icon badges and value text, [light]
 * is the pale tint used for banner backgrounds and selected option pills.
 *
 * Screens reference [Accents] rather than raw colours so a category looks the
 * same everywhere it appears — Dashboard card, Settings row, EMA section,
 * Collected Data row.
 */
data class CategoryAccent(
    val solid: Color,
    val light: Color
)

object Accents {
    val Movement = CategoryAccent(MovementBlue, MovementBlueLight)
    val Location = CategoryAccent(LocationGreen, LocationGreenLight)
    val Context = CategoryAccent(ContextRed, ContextRedLight)
    val Ema = CategoryAccent(EmaPurple, EmaPurpleLight)
    val Success = CategoryAccent(SuccessGreen, SuccessGreenLight)

    /** Grey badge used by permission rows and other non-category icons. */
    val Neutral = CategoryAccent(NeutralBadgeIcon, NeutralBadge)
}
