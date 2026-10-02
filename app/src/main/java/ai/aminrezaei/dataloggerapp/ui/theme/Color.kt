package ai.aminrezaei.dataloggerapp.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ---------------------------------------------------------------------------
// Mock palette
//
// Every colour the participant-facing UI uses is declared here. Screens must not
// invent one-off colours: if something needs a new shade it belongs in this file
// so the four mock screens and everything downstream stay in sync.
// ---------------------------------------------------------------------------

// Per-category accents. Solid variants fill the circular icon badges (white glyph
// on top); Light variants are the pale tints used for banner/section backgrounds.
val MovementBlue = Color(0xFF1565C0)
val MovementBlueLight = Color(0xFFE8F0FC)
val LocationGreen = Color(0xFF1E9E52)
val LocationGreenLight = Color(0xFFE6F5EB)
val ContextRed = Color(0xFFD32F2F)
val ContextRedLight = Color(0xFFFCE8E8)
val EmaPurple = Color(0xFF6D34CE)
val EmaPurpleLight = Color(0xFFF1EBFC)

// Status / feedback
val SuccessGreen = Color(0xFF1E8E3E)
val SuccessGreenLight = Color(0xFFE7F5EA)
val StopRed = Color(0xFFB3221B)
val SwitchGreen = Color(0xFF2FB94D)

// Status-banner fill and outline. The mock draws the active banner as a very pale
// wash with a visible tinted edge, not as a flat block of colour — at the
// SuccessGreenLight strength used for badges it reads as a solid green panel and
// competes with the stat cards below it.
val StatusBannerActive = Color(0xFFF2F9F4)
val StatusBannerActiveBorder = Color(0xFFC8E6D0)
val StatusBannerIdle = Color(0xFFF7F8FA)
val StatusBannerIdleBorder = Color(0xFFE2E5EA)

// Neutrals. PageBackground sits behind the cards; CardSurface is the card itself.
val PageBackground = Color(0xFFF5F6F8)
val CardSurface = Color(0xFFFFFFFF)
val TopBarSurface = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFF1A1C1E)
val TextSecondary = Color(0xFF5F6570)
val TextTertiary = Color(0xFF8A9099)

val HairlineDivider = Color(0xFFECEEF1)
val OutlineBorder = Color(0xFFDCDFE4)

// Neutral badge used by permission rows and other non-category icons.
val NeutralBadge = Color(0xFFEDEFF2)
val NeutralBadgeIcon = Color(0xFF5F6570)

// Primary action blue (Get Started, active bottom-nav item).
val ActionBlue = Color(0xFF1A56C4)
