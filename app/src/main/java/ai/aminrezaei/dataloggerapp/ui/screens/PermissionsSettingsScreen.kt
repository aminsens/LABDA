package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.ui.components.BadgeStyle
import ai.aminrezaei.dataloggerapp.ui.components.DetailScaffold
import ai.aminrezaei.dataloggerapp.ui.components.IconBadge
import ai.aminrezaei.dataloggerapp.ui.components.NavRow
import ai.aminrezaei.dataloggerapp.ui.components.PrimaryButton
import ai.aminrezaei.dataloggerapp.ui.components.RowDivider
import ai.aminrezaei.dataloggerapp.ui.components.SecondaryButton
import ai.aminrezaei.dataloggerapp.ui.components.SectionCard
import ai.aminrezaei.dataloggerapp.ui.components.SectionHeader
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.theme.Accents
import ai.aminrezaei.dataloggerapp.ui.theme.SuccessGreen
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun PermissionsSettingsScreen(
    mainViewModel: MainViewModel,
    selectedTheme: Int,
    navController: NavController
) {
    val permissionStates by mainViewModel.permissionStates.collectAsState()

    DetailScaffold(
        title = "Permissions",
        onBack = { navController.popBackStack() }
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("Required access")
        SectionCard {
            PermissionCard(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                isGranted = permissionStates.notificationPermission,
                onRequestPermission = { mainViewModel.requestPermission("notification") }
            )
            RowDivider(inset = 16.dp)
            PermissionCard(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                isGranted = permissionStates.locationPermission == true &&
                    permissionStates.backgroundLocationPermission == true,
                onRequestPermission = {
                    when {
                        permissionStates.locationPermission != true ->
                            mainViewModel.requestPermission("location")

                        permissionStates.backgroundLocationPermission != true ->
                            mainViewModel.requestPermission("background_location")
                    }
                }
            )
            RowDivider(inset = 16.dp)
            PermissionCard(
                icon = Icons.Filled.DirectionsRun,
                title = "Activity Recognition",
                isGranted = permissionStates.activityRecognitionPermission,
                onRequestPermission = { mainViewModel.requestPermission("physical_activity") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionCard {
            NavRow(
                icon = Icons.Filled.Shield,
                accent = Accents.Location,
                badgeStyle = BadgeStyle.Tinted,
                title = "About LABDA",
                subtitle = "What LABDA collects, where it is stored, and your choices",
                onClick = { navController.navigate("StudyInfo") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    isGranted: Boolean?,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            accent = Accents.Neutral,
            style = BadgeStyle.Tinted,
            size = 40.dp,
            iconSize = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text = when (isGranted) {
                    true -> "Allowed"
                    false -> "Denied"
                    null -> "Not requested yet"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isGranted == true) SuccessGreen else TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        val onClick = {
            onRequestPermission()
            Toast.makeText(
                context,
                "$title permission ${
                    when (isGranted) {
                        true -> "already granted"
                        false -> "denied"
                        null -> "requested"
                    }
                }",
                Toast.LENGTH_SHORT
            ).show()
        }

        // A granted permission needs no call to action, so it gets the quiet button.
        // Labels are unchanged from the previous build.
        if (isGranted == true) {
            SecondaryButton(text = "Granted", onClick = onClick)
        } else {
            PrimaryButton(
                text = if (isGranted == false) "Denied" else "Request",
                onClick = onClick
            )
        }
    }
}
