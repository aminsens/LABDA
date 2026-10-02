package ai.aminrezaei.dataloggerapp.components.navigation

import ai.aminrezaei.dataloggerapp.components.mainlogic.MainScreen
import ai.aminrezaei.dataloggerapp.ui.screens.ActivityRecognitionPermissionScreen
import ai.aminrezaei.dataloggerapp.ui.screens.DataScreen
import ai.aminrezaei.dataloggerapp.ui.screens.EmaResponseScreen
import ai.aminrezaei.dataloggerapp.ui.screens.LocationPermissionScreen
import ai.aminrezaei.dataloggerapp.ui.screens.NotificationPermissionScreen
import ai.aminrezaei.dataloggerapp.ui.screens.PermissionsSettingsScreen
import ai.aminrezaei.dataloggerapp.ui.screens.SettingsScreen
import ai.aminrezaei.dataloggerapp.ui.screens.StudyInfoScreen
import ai.aminrezaei.dataloggerapp.ui.screens.WelcomeScreen
import ai.aminrezaei.dataloggerapp.ui.screens.EmaHistoryScreen
import ai.aminrezaei.dataloggerapp.ui.state.MainViewModel
import ai.aminrezaei.dataloggerapp.ui.state.SettingsViewModel
import ai.aminrezaei.dataloggerapp.ui.theme.ActionBlue
import ai.aminrezaei.dataloggerapp.ui.theme.CardSurface
import ai.aminrezaei.dataloggerapp.ui.theme.HairlineDivider
import ai.aminrezaei.dataloggerapp.ui.theme.PageBackground
import ai.aminrezaei.dataloggerapp.ui.theme.TextTertiary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument

/**
 * The mock shows a plain tinted icon and label with no pill behind the selected
 * item, so the indicator is painted in the bar's own colour to make it invisible.
 *
 * It must not be Color.Transparent: NavigationBarItem fades the indicator in with
 * `indicatorColor.copy(alpha = animationProgress)`, and Transparent is (0,0,0,0),
 * so that produced a solid black pill.
 */
@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ActionBlue,
    selectedTextColor = ActionBlue,
    unselectedIconColor = TextTertiary,
    unselectedTextColor = TextTertiary,
    indicatorColor = CardSurface
)

@Composable
fun EnhancedNavigationComponent(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val isFirstLaunch by mainViewModel.isFirstLaunch.collectAsState()
    val startDestination = if (isFirstLaunch) "Welcome" else "Main"

    val selectedTheme by mainViewModel.selectedTheme.collectAsState()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in setOf("Main", "Data", "Settings", "ema_response/{promptRowId}")

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HairlineDivider)
                    )
                    // White bar, no pill indicator behind the selected item — the
                    // mock shows a plain tinted icon and label.
                    NavigationBar(
                        containerColor = CardSurface,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "Main",
                            onClick = {
                                navController.navigate("Main") {
                                    popUpTo("Main") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            selected = currentRoute == "Data",
                            onClick = {
                                navController.navigate("Data") {
                                    popUpTo("Main")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = "Data") },
                            label = { Text("Data", style = MaterialTheme.typography.labelSmall) },
                            colors = navItemColors()
                        )
                        NavigationBarItem(
                            selected = currentRoute == "Settings",
                            onClick = {
                                navController.navigate("Settings") {
                                    popUpTo("Main")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                            colors = navItemColors()
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("Welcome") {
                WelcomeScreen(
                    onContinue = {
                        navController.navigate("notification_permission") {
                            popUpTo("Welcome") { inclusive = true }
                        }
                    },
                    onLearnMore = {
                        navController.navigate("StudyInfo")
                    }
                )
            }
            composable("StudyInfo") {
                StudyInfoScreen(navController = navController)
            }
            composable("notification_permission") {
                NotificationPermissionScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    onRequestPermission = {
                        mainViewModel.requestPermission("notification")
                    }
                )
            }
            composable("location_permission") {
                LocationPermissionScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    onRequestPermission = {
                        mainViewModel.requestPermission("location")
                    }
                )
            }
            composable("activity_recognition_permission") {
                ActivityRecognitionPermissionScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    onRequestPermission = {
                        mainViewModel.requestPermission("physical_activity")
                    }
                )
            }
            composable("Main") {
                MainScreen(viewModel = mainViewModel, navController = navController)
            }
            composable("Data") {
                DataScreen(navController = navController)
            }
            composable("EmaHistory") {
                EmaHistoryScreen(navController = navController)
            }
            composable("Settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    mainViewModel = mainViewModel,
                    selectedTheme = selectedTheme,
                    navController = navController
                )
            }
            composable("PermissionsSettings") {
                PermissionsSettingsScreen(
                    mainViewModel = mainViewModel,
                    selectedTheme = selectedTheme,
                    navController = navController
                )
            }
            composable(
                route = "ema_response/{promptRowId}",
                arguments = listOf(navArgument("promptRowId") { type = NavType.LongType })
            ) { backStackEntry ->
                val promptRowId = backStackEntry.arguments?.getLong("promptRowId") ?: return@composable
                EmaResponseScreen(
                    promptRowId   = promptRowId,
                    navController = navController
                )
            }
        }
    }
}
