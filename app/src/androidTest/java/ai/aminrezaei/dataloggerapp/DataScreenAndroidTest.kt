package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseEntity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section E (Data screen count verification).
 *
 * Precondition: onboarding already complete. Runs in-process so the displayed counts can
 * be cross-checked directly against the same Room DAOs DataScreen itself queries.
 *
 * DataScreen's counts are only computed once per composition (a `LaunchedEffect(Unit)`),
 * and bottom-nav navigation here disposes/recreates the destination each time (no
 * saveState/restoreState on the NavHost), so navigating away and back forces a fresh
 * query — used below to observe count changes after inserting rows mid-test.
 */
@RunWith(AndroidJUnit4::class)
class DataScreenAndroidTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun openData() {
        composeTestRule.onNodeWithContentDescription("Data").performClick()
        composeTestRule.onNodeWithText("Collected Data").assertIsDisplayed()
    }

    private fun openDashboard() {
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
        composeTestRule.onNodeWithText("LABDA").assertIsDisplayed()
    }

    private fun assertCountDisplayed(count: Int) {
        assertTrue(
            "Expected \"$count records\" to be displayed on the Data screen",
            composeTestRule.onAllNodesWithText("$count records").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun dataScreen_displaysCountsMatchingRoomDao() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()

        val movementCount = runBlocking { db.accelerometerDao().getCount() }
        val locationCount = runBlocking { db.locationDao().getCount() }
        val contextCount = runBlocking { db.deviceStateDao().getCount() }
        val emaAnsweredCount = runBlocking { db.emaResponseDao().getAnsweredCount() }

        openData()

        assertCountDisplayed(movementCount)
        assertCountDisplayed(locationCount)
        assertCountDisplayed(contextCount)
        assertCountDisplayed(emaAnsweredCount)
    }

    @Test
    fun dataScreen_excludesUnansweredEmaPrompt_countsOnlyAnsweredOnes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()
        val dao = db.emaResponseDao()

        val answeredBefore = runBlocking { dao.getAnsweredCount() }

        val unansweredId = runBlocking {
            dao.insert(
                EmaResponseEntity(
                    promptTimestamp = System.currentTimeMillis(),
                    responseTimestamp = null,
                    activityLabel = null,
                    locationLabel = null,
                    socialLabel = null,
                    optionalTags = null,
                    latencySeconds = null,
                    dismissed = 0
                )
            )
        }

        openDashboard()
        openData()
        assertCountDisplayed(answeredBefore)

        val answeredTimestamp = System.currentTimeMillis()
        runBlocking {
            dao.updateResponse(
                id = unansweredId,
                responseTs = answeredTimestamp,
                activity = "Sitting",
                location = "At home",
                social = "Alone",
                tags = null,
                latency = 5
            )
        }

        openDashboard()
        openData()
        assertCountDisplayed(answeredBefore + 1)
    }
}
