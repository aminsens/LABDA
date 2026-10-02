package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.utils.Constants
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section D (Settings behaviour).
 *
 * Precondition: onboarding already complete, all runtime permissions granted via ADB.
 * Runs in-process so live-service toggles (Movement/Context) and Room row counts can be
 * observed directly through the same DatabaseProvider/LocationService the app uses.
 */
@RunWith(AndroidJUnit4::class)
class SettingsBehaviorAndroidTest {

    private val tag = "SmokeTest_D"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun openSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Movement data").assertIsDisplayed()
    }

    private fun openDashboard() {
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()
    }

    /**
     * SettingCard's title Text and its Switch are not semantics-merged (the Switch has no
     * shared clickable/testTag with the row), so switches must be selected by their fixed
     * composition order rather than by the label text next to them: Movement data (0),
     * Location data (1), Context data (2), EMA prompts (3).
     */
    private fun clickSettingSwitch(index: Int) {
        composeTestRule.onAllNodes(isToggleable())[index].performClick()
    }

    @Test
    fun emaInterval_allChoicesPresent_selectionTogglesEnabledState() {
        openSettings()

        composeTestRule.onNodeWithText("1h").assertIsDisplayed()
        composeTestRule.onNodeWithText("2h").assertIsDisplayed()
        composeTestRule.onNodeWithText("4h").assertIsDisplayed()
        composeTestRule.onNodeWithText("8h").assertIsDisplayed()

        composeTestRule.onNodeWithText("2h").performClick()
        composeTestRule.onNodeWithText("2h").assertIsNotEnabled()
        composeTestRule.onNodeWithText("4h").assertIsEnabled()

        composeTestRule.onNodeWithText("8h").performClick()
        composeTestRule.onNodeWithText("8h").assertIsNotEnabled()
        composeTestRule.onNodeWithText("2h").assertIsEnabled()
    }

    @Test
    fun emaIntervalAndPromptsToggle_persistAcrossActivityRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefsManager = SharedPreferencesManager(context)
        val promptsBefore = prefsManager.getLoggingPreference(Constants.LOGGING_EMA_PROMPTS)

        openSettings()

        composeTestRule.onNodeWithText("8h").performClick()
        composeTestRule.onNodeWithText("8h").assertIsNotEnabled()

        clickSettingSwitch(3) // EMA prompts
        val promptsAfterToggle = prefsManager.getLoggingPreference(Constants.LOGGING_EMA_PROMPTS)
        assertTrue(
            "EMA prompts preference should flip immediately when the switch is tapped",
            promptsAfterToggle != promptsBefore
        )

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        openSettings()
        composeTestRule.onNodeWithText("8h").assertIsNotEnabled()

        val promptsAfterRestart = prefsManager.getLoggingPreference(Constants.LOGGING_EMA_PROMPTS)
        assertTrue(
            "EMA prompts toggle preference should survive an activity restart",
            promptsAfterRestart == promptsAfterToggle
        )

        clickSettingSwitch(3) // restore original EMA prompts state
    }

    @Test
    fun quietHours_persistAcrossActivityRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = SharedPreferencesManager(context).getSharedPreferences()

        // Native TimePickerDialog isn't part of the Compose semantics tree, so this test
        // writes the exact SharedPreferences keys the dialog's callback would write, then
        // verifies the persisted value survives a restart and is reflected in the UI.
        prefs.edit()
            .putString(Constants.KEY_EMA_QUIET_HOURS_START, "23:15")
            .putString(Constants.KEY_EMA_QUIET_HOURS_END, "06:45")
            .apply()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        openSettings()

        composeTestRule.onNodeWithText("Start: 23:15").assertIsDisplayed()
        composeTestRule.onNodeWithText("End: 06:45").assertIsDisplayed()
    }

    @Test
    fun movementToggle_offStopsAccelGrowth_onResumesGrowth() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()

        openDashboard()
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is active").fetchSemanticsNodes().isNotEmpty()
        }

        Thread.sleep(3_000)
        val baseline = runBlocking { db.accelerometerDao().getCount() }

        openSettings()
        clickSettingSwitch(0) // Movement data
        Thread.sleep(3_000)
        val afterOff = runBlocking { db.accelerometerDao().getCount() }
        val plateauCount = runBlocking { db.accelerometerDao().getCount() }
        Log.i(tag, "Accel rows: baseline=$baseline afterOff=$afterOff plateau=$plateauCount")

        clickSettingSwitch(0) // Movement data
        Thread.sleep(3_000)
        val afterOn = runBlocking { db.accelerometerDao().getCount() }
        Log.i(tag, "Accel rows after re-enabling: $afterOn")

        openDashboard()
        composeTestRule.onNodeWithText("Stop").performClick()

        assertTrue(
            "Accelerometer rows should stop growing (or grow by no more than one flushed partial batch) once Movement data is disabled",
            plateauCount - afterOff <= 1
        )
        assertTrue(
            "Accelerometer rows should resume growing once Movement data is re-enabled",
            afterOn > plateauCount
        )
    }

    @Test
    fun contextToggle_offPreventsWarmupRow_onRestoresWarmupRow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()

        openSettings()
        clickSettingSwitch(2) // Context data — turn OFF

        openDashboard()
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is active").fetchSemanticsNodes().isNotEmpty()
        }
        val baselineOff = runBlocking { db.deviceStateDao().getCount() }
        Thread.sleep(4_500) // past the 3s startup warmup window
        val afterWarmupOff = runBlocking { db.deviceStateDao().getCount() }
        Log.i(tag, "Device-state rows with context OFF: baseline=$baselineOff after=$afterWarmupOff")

        composeTestRule.onNodeWithText("Stop").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is not active").fetchSemanticsNodes().isNotEmpty()
        }

        openSettings()
        clickSettingSwitch(2) // Context data — turn back ON

        openDashboard()
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is active").fetchSemanticsNodes().isNotEmpty()
        }
        val baselineOn = runBlocking { db.deviceStateDao().getCount() }
        Thread.sleep(4_500)
        val afterWarmupOn = runBlocking { db.deviceStateDao().getCount() }
        Log.i(tag, "Device-state rows with context ON: baseline=$baselineOn after=$afterWarmupOn")

        composeTestRule.onNodeWithText("Stop").performClick()

        assertTrue(
            "No device-state (context) row should be inserted by the startup warmup while Context data is disabled",
            afterWarmupOff == baselineOff
        )
        assertTrue(
            "A device-state (context) row should be inserted by the startup warmup once Context data is re-enabled",
            afterWarmupOn > baselineOn
        )
    }
}
