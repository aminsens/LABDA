package ai.aminrezaei.dataloggerapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Retained for GradientBox, which predates the mock palette.
val LightPrimary = ActionBlue
val LightSecondary = Color(0xFF00ACC1)
val LightBackground = PageBackground
val LightSurface = CardSurface
val LightOnError = Color(0xFFB00020)

val DarkPrimary = Color(0xFF90CAF9)
val DarkSecondary = Color(0xFF80DEEA)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnError = Color(0xFFFFB4A9)

val AccentColor = Color(0xFFFFD617)

val GoogleBlueLight = Color(0xFF1a73e8)
val GoogleBlueDark = Color(0xFF8ab4f8)

/**
 * The one colour scheme the app ships with, taken from the design mocks.
 *
 * Deliberately light-only. Material You dynamic colour used to be enabled here,
 * which meant every surface, accent and switch was generated from the user's
 * wallpaper and none of the mock colours ever reached the screen.
 */
private val MockLightColorScheme = lightColorScheme(
    primary = ActionBlue,
    onPrimary = Color.White,
    primaryContainer = MovementBlueLight,
    onPrimaryContainer = ActionBlue,

    secondary = EmaPurple,
    onSecondary = Color.White,
    secondaryContainer = EmaPurpleLight,
    onSecondaryContainer = EmaPurple,

    tertiary = LocationGreen,
    onTertiary = Color.White,
    tertiaryContainer = LocationGreenLight,
    onTertiaryContainer = LocationGreen,

    background = PageBackground,
    onBackground = TextPrimary,

    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralBadge,
    onSurfaceVariant = TextSecondary,

    // Material tints every elevated surface with this colour in proportion to its
    // elevation. Left at the default (primary) it turned every white card pale blue.
    // Note this must be the surface colour and NOT Color.Transparent: Material
    // composites `surfaceTint.copy(alpha = ...)` over the surface, and Transparent is
    // (0,0,0,0), so copying an alpha onto it yields black — a 1dp card came out
    // #F2F2F2 instead of white. Tinting white with white is the real no-op.
    surfaceTint = CardSurface,

    outline = OutlineBorder,
    outlineVariant = HairlineDivider,

    error = ContextRed,
    onError = Color.White,
    errorContainer = ContextRedLight,
    onErrorContainer = ContextRed
)

// Unused while the app is light-only, but kept so a dark counterpart can be
// derived from the same accents later rather than from the wallpaper.
private val PlaceholderDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkOnError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

/**
 * Parameters are retained for source compatibility with existing call sites but
 * are intentionally ignored — the app renders the light mock palette in every
 * configuration, including when the system is in dark mode.
 */
@Composable
fun DataLoggerAppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    selectedTheme: Int = 0,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MockLightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
