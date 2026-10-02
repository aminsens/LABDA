package ai.aminrezaei.dataloggerapp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section A (first launch / onboarding).
 *
 * Precondition: the app must be in first-launch state, i.e. run right after
 * `adb shell pm clear ai.aminrezaei.dataloggerapp`, with the notification, location and
 * activity-recognition runtime permissions already granted via ADB (see
 * scripts/device_smoke_test.ps1). Pre-granting lets each onboarding permission screen's
 * LaunchedEffect auto-advance without needing to drive the system permission dialogs.
 * (No in-process GrantPermissionRule backup: androidx.test:rules isn't a project
 * dependency, and ADB pre-granting is required for this test regardless.)
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstLaunch_learnMoreThenBack_returnsToWelcome() {
        composeTestRule.onNodeWithText("Welcome to LABDA").assertIsDisplayed()

        composeTestRule.onNodeWithText("Learn More").performClick()
        composeTestRule.onNodeWithText("Study Information").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Welcome to LABDA").assertIsDisplayed()
    }

    @Test
    fun firstLaunch_getStarted_autoAdvancesThroughPermissionsToDashboardOnce() {
        composeTestRule.onNodeWithText("Welcome to LABDA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").performClick()

        // All three runtime permissions are pre-granted via ADB, so each permission
        // screen auto-navigates forward without a simulated Allow/Continue tap.
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText("LABDA").fetchSemanticsNodes().isNotEmpty()
        }

        // Dashboard reached, with the top-level bottom navigation visible.
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()

        // The bottom-nav icon's content description is not a reliable displayed semantics
        // node; wait for and assert the "Start" button, a unique inactive-Dashboard element
        // (enabled only when collection is not running - see Main.kt).
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithText("Start").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Data").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Reached Dashboard exactly once: no leftover onboarding/permission screen content.
        composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Activity Recognition").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Welcome to LABDA").assertCountEquals(0)
    }
}
