package ai.aminrezaei.dataloggerapp.ui.theme

import ai.aminrezaei.dataloggerapp.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Inter, bundled as four static weights in res/font.
 *
 * The device default (Roboto on most builds, but whatever the vendor ships in
 * practice) made the mocks look flat and rendered differently across handsets.
 * Bundling means every participant sees the same thing regardless of phone.
 * Only the weights the scale below actually asks for are shipped — adding a new
 * FontWeight here without a matching file makes Compose synthesise it, which
 * looks noticeably worse than the real cut.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/**
 * Tabular (fixed-width) figures. Inter's default digits are proportional, so a
 * counter ticking 9 -> 10 -> 11 shifts width on every refresh. The Dashboard and
 * Data screens re-read their counts every few seconds, and without this the
 * numbers visibly jitter. Only digit metrics change; letters are unaffected.
 */
private const val TabularFigures = "tnum"

/**
 * Type scale read off the mocks.
 *
 * Note the label styles carry no letter spacing — Material's default 1.25sp
 * tracking on button text spreads labels far wider than the mock shows.
 */
val Typography = Typography(
    // "LABDA" wordmark on Welcome, and the "Settings" page heading.
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TabularFigures
    ),
    // Top-bar title, and the large stat numbers on the Dashboard.
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TabularFigures
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // Card titles and setting-row titles.
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    // Card subtitles and supporting copy.
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    // Row subtitles and captions. The extra leading is for the multi-line ones —
    // the Context data subtitle runs to three lines and at 18sp they crowded.
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    // Button labels.
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
