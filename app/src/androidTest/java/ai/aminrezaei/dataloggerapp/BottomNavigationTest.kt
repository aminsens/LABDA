package ai.aminrezaei.dataloggerapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section B (top-level bottom navigation).
 *
 * Precondition: onboarding must already be complete (isFirstTimeLaunch = false), so the
 * app starts directly on the Dashboard ("Main") instead of the Welcome screen. Run this
 * after OnboardingFlowTest in the same `am instrument` session — see
 * scripts/device_smoke_test.ps1.
 */
@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNav_switchesBetweenDestinations_withoutDuplicatingStack() {
        // Starts on Dashboard.
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Data").performClick()
        composeTestRule.onNodeWithText("Collected Data").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Movement data").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()

        // Repeated selection of the already-active destination must be a no-op, not push
        // a new copy of it onto the back stack.
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        }
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()

        // Navigating away and directly back must land straight on Dashboard, not step
        // through a stale duplicated entry first.
        composeTestRule.onNodeWithContentDescription("Data").performClick()
        composeTestRule.onNodeWithText("Collected Data").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()
    }
}
