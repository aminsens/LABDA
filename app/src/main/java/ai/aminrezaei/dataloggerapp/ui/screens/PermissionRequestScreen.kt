package ai.aminrezaei.dataloggerapp.ui.screens

import ai.aminrezaei.dataloggerapp.R
import ai.aminrezaei.dataloggerapp.ui.components.PrimaryButton
import ai.aminrezaei.dataloggerapp.ui.components.SecondaryButton
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.state.PermissionAnimationState
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
import ai.aminrezaei.dataloggerapp.ui.theme.TextPrimary
import ai.aminrezaei.dataloggerapp.ui.theme.TextSecondary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun PermissionRequestScreen(
    navController: NavController,
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onRequestPermission: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    isWaitingForPermission: Boolean,
    isPermissionGranted: Boolean?,
    animationState: PermissionAnimationState,
    onForward: () -> Unit,
    setPermissionInteraction: () -> Unit,
    onGoToSettings: () -> Unit,
    onExitApp: () -> Unit,
    onSkipPermission: () -> Unit
) {
    var showSkipDialog by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(true) }

    LaunchedEffect(animationState) {
        showContent = animationState == PermissionAnimationState.Idle
    }

    LaunchedEffect(isWaitingForPermission, isPermissionGranted) {
        if (!isWaitingForPermission && isPermissionGranted != null) {
            if (!isPermissionGranted) {
                setPermissionInteraction()
                onPermissionResult(false)
            } else {
                onPermissionResult(true)
            }
        }
    }

    // No systemBarsPadding() — the host Scaffold in the navigation component
    // already applies window insets, and applying them twice left dead space.
    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        setPermissionInteraction()
                        onSkipPermission()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        icon()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (isPermissionGranted) {
                                true -> "Permission granted"
                                false -> "Permission denied"
                                null -> description
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        when (animationState) {
                            PermissionAnimationState.Loading -> {
                                CircularProgressIndicator()
                            }
                            else -> {}
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            isPermissionGranted == true || animationState != PermissionAnimationState.Idle -> {
                                PrimaryButton(
                                    text = "Continue",
                                    onClick = onForward,
                                    enabled = animationState == PermissionAnimationState.Idle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            isPermissionGranted == false -> {
                                PrimaryButton(
                                    text = "Go to Settings",
                                    onClick = onGoToSettings,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryButton(
                                    text = "Skip for now",
                                    onClick = { showSkipDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                PrimaryButton(
                                    text = "Allow",
                                    onClick = {
                                        setPermissionInteraction()
                                        onRequestPermission()
                                    },
                                    enabled = !isWaitingForPermission,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryButton(
                                    text = "Skip for now",
                                    onClick = { showSkipDialog = true },
                                    enabled = !isWaitingForPermission,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center),
                visible = animationState == PermissionAnimationState.Celebration || animationState == PermissionAnimationState.Skip,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                when (animationState) {
                    PermissionAnimationState.Celebration -> {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.checkmark_animation))
                        LottieAnimation(
                            composition = composition,
                            iterations = 1,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    PermissionAnimationState.Skip -> {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.crossmark_animation))
                        LottieAnimation(
                            composition = composition,
                            iterations = 1,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip this permission?") },
            text = { Text("LABDA works best when all permissions are granted. You can grant this permission later from Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    setPermissionInteraction()
                    onSkipPermission()
                }) {
                    Text("Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Go back")
                }
            }
        )
    }
}

@Composable
fun NotificationPermissionScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit
) {
    val isWaitingForPermission by viewModel.isWaitingForPermissionResult.collectAsState()
    val animationState by viewModel.animationState.collectAsState()
    val permissionStates by viewModel.permissionStates.collectAsState()
    val isPermissionGranted = permissionStates.notificationPermission

    PermissionRequestScreen(
        navController = navController,
        title = "Notifications",
        description = "LABDA sends short check-in prompts a few times a day. Each takes under a minute to answer. Allow notifications so you don't miss them.",
        icon = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(110.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        onRequestPermission = onRequestPermission,
        onPermissionResult = { granted ->
            viewModel.onPermissionResult("notification", granted, navController)
        },
        isWaitingForPermission = isWaitingForPermission,
        isPermissionGranted = isPermissionGranted,
        animationState = animationState,
        onForward = {
            viewModel.checkAndNavigate("notification", navController)
        },
        setPermissionInteraction = viewModel::setPermissionInteraction,
        onGoToSettings = viewModel::onGoToSettings,
        onSkipPermission = {
            viewModel.onSkipPermission("notification", navController)
        },
        onExitApp = viewModel::onExitApp
    )
}

@Composable
fun LocationPermissionScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit
) {
    val isWaitingForPermission by viewModel.isWaitingForPermissionResult.collectAsState()
    val animationState by viewModel.animationState.collectAsState()
    val permissionStates by viewModel.permissionStates.collectAsState()
    val isPermissionGranted = permissionStates.locationPermission

    PermissionRequestScreen(
        navController = navController,
        title = "Location",
        description = "LABDA records your GPS location while collection is active, so the study can see where activities take place. Location is stored on your device.",
        icon = {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.size(110.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        onRequestPermission = onRequestPermission,
        onPermissionResult = { granted ->
            viewModel.onLocationPermissionResult(granted, navController)
        },
        isWaitingForPermission = isWaitingForPermission,
        isPermissionGranted = isPermissionGranted,
        animationState = animationState,
        onForward = {
            viewModel.checkAndNavigate("location", navController)
        },
        setPermissionInteraction = viewModel::setPermissionInteraction,
        onGoToSettings = viewModel::onGoToSettings,
        onSkipPermission = {
            viewModel.onSkipPermission("location", navController)
        },
        onExitApp = viewModel::onExitApp
    )
}

@Composable
fun ActivityRecognitionPermissionScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit
) {
    val isWaitingForPermission by viewModel.isWaitingForPermissionResult.collectAsState()
    val animationState by viewModel.animationState.collectAsState()
    val permissionStates by viewModel.permissionStates.collectAsState()
    val isPermissionGranted = permissionStates.activityRecognitionPermission

    PermissionRequestScreen(
        navController = navController,
        title = "Activity Recognition",
        description = "This lets your phone detect walking, cycling and stillness automatically, so LABDA can label your activity without any manual input.",
        icon = {
            Icon(
                imageVector = Icons.Filled.DirectionsRun,
                contentDescription = "Activity Recognition",
                modifier = Modifier.size(110.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        onRequestPermission = onRequestPermission,
        onPermissionResult = { granted ->
            viewModel.onPermissionResult("activity_recognition", granted, navController)
        },
        isWaitingForPermission = isWaitingForPermission,
        isPermissionGranted = isPermissionGranted,
        animationState = animationState,
        onForward = {
            viewModel.checkAndNavigate("activity_recognition", navController)
        },
        setPermissionInteraction = viewModel::setPermissionInteraction,
        onGoToSettings = viewModel::onGoToSettings,
        onSkipPermission = {
            viewModel.onSkipPermission("activity_recognition", navController)
        },
        onExitApp = viewModel::onExitApp
    )
}
