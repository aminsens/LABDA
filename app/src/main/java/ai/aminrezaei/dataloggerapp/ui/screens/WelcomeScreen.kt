package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.R
import ai.aminrezaei.dataloggerapp.ui.components.BadgeStyle
import ai.aminrezaei.dataloggerapp.ui.components.IconBadge
import ai.aminrezaei.dataloggerapp.ui.components.PrimaryButton
import ai.aminrezaei.dataloggerapp.ui.components.RowDivider
import ai.aminrezaei.dataloggerapp.ui.components.SecondaryButton
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.components.SubtitleGap
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.CategoryAccent
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
import ai.aminrezaei.dataloggerapp.ui.theme.SuccessGreen
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No systemBarsPadding() here — the host Scaffold already applies window insets.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_adaptive_fore),
            contentDescription = "LABDA Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "LABDA",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Welcome to the study app",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "This app helps researchers understand how movement and everyday context relate to health and well-being.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "It collects data in the background and may occasionally ask you short questions.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Let's get you set up",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                SetupRow(
                    icon = Icons.Filled.Shield,
                    accent = Accents.Location,
                    title = "Permissions",
                    subtitle = "Allow access to sensors and notifications."
                )
                RowDivider()
                SetupRow(
                    icon = Icons.Filled.Tune,
                    accent = Accents.Movement,
                    title = "Logging preferences",
                    subtitle = "Choose what data to collect and how we use it."
                )
                RowDivider()
                SetupRow(
                    icon = Icons.Filled.Chat,
                    accent = Accents.Ema,
                    title = "EMA prompts",
                    subtitle = "Tell us how often you'd like to receive short surveys."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Get Started",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryButton(
            text = "Learn More",
            onClick = onLearnMore,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Your data is private and secure. You can change your settings any time in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupRow(
    icon: ImageVector,
    accent: CategoryAccent,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            accent = accent,
            style = BadgeStyle.Tinted,
            size = 40.dp,
            iconSize = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(SubtitleGap))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(22.dp)
        )
    }
}
