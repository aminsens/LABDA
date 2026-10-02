package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.ui.components.DetailScaffold
import ai.aminrezaei.dataloggerapp.ui.components.InfoBlock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * The single explanatory screen: what the study is, what it records, where the
 * records live, which permissions are involved and what the participant controls.
 *
 * This used to be two screens — "Study Information" and "Data & Privacy" — reached
 * from the overflow menu, from Settings and from Permissions. They restated each
 * other almost line for line, so they are merged here and every entry point now
 * lands on this one screen.
 */
@Composable
fun StudyInfoScreen(navController: NavController) {
    DetailScaffold(
        title = "About LABDA",
        onBack = { navController.popBackStack() }
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        InfoBlock(
            heading = "What is LABDA?",
            body = "LABDA is a research data-collection app used in a study of physical activity and daily behaviour. It runs on your phone and records information while you go about your normal day."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoBlock(
            heading = "What data does LABDA collect?",
            body = "• Movement: accelerometer readings that describe how your phone moves.\n" +
                "• Location: GPS position updates while collection is active.\n" +
                "• Device context: battery level, screen on/off, charging state, Wi-Fi connection status, activity recognition (walking, cycling, still), step count, light and proximity.\n" +
                "• Short check-ins: brief surveys about what you are doing, where you are, and who you are with."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoBlock(
            heading = "Where your data is stored",
            body = "All records are stored locally on this device in the app's private database. In this version of the app, no data is uploaded or transmitted anywhere."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoBlock(
            heading = "Permissions",
            body = "You can review and change the permissions LABDA uses (notifications, location and activity recognition) at any time from the Settings screen or from your phone's system settings."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoBlock(
            heading = "Your choices",
            body = "• You can start and stop collection at any time from the Dashboard.\n" +
                "• You can skip any check-in prompt.\n" +
                "• You can turn individual data categories on or off in Settings.\n" +
                "• You can review and change permissions at any time."
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
