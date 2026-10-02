package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseEntity
import ai.aminrezaei.dataloggerapp.ema.EmaWorker
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section F (EMA check-in workflow: submit, skip, duplicate suppression).
 *
 * Runs in-process. Notification-tap navigation is simulated by building the exact same
 * `Intent` (flags + extras) that `EmaNotificationHelper.sendPromptNotification` attaches to
 * its real `PendingIntent`, then dispatching it via `context.startActivity(intent)`. Since
 * `MainActivity.onNewIntent` is protected, this lets Android's own singleTop intent routing
 * invoke the real callback instead of calling it directly, reusing the production code path
 * without any visibility change to app source.
 */
@RunWith(AndroidJUnit4::class)
class EmaWorkflowAndroidTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun insertUnansweredPrompt(): Long {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val dao = DatabaseProvider.getDatabase().emaResponseDao()
        return runBlocking {
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
    }

    private fun openCheckInViaNotificationTap(promptRowId: Long) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constants.EMA_INTENT_ACTION_OPEN, true)
            putExtra(Constants.EMA_INTENT_EXTRA_PROMPT_ROW_ID, promptRowId)
        }
        context.startActivity(intent)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Check-in").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun submit_persistsResponseAndLabelsAndRespectsNotesCap() {
        val promptRowId = insertUnansweredPrompt()
        openCheckInViaNotificationTap(promptRowId)

        composeTestRule.onNodeWithText("Check-in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Walking").performClick()
        composeTestRule.onNodeWithText("At work").performClick()
        composeTestRule.onNodeWithText("Alone").performClick()

        val overlongNotes = "x".repeat(250)
        composeTestRule.onNode(hasSetTextAction()).performTextInput(overlongNotes)
        composeTestRule.onNodeWithText("200/200").assertIsDisplayed()

        composeTestRule.onNodeWithText("Submit").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Check-in").fetchSemanticsNodes().isEmpty()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val saved = runBlocking { DatabaseProvider.getDatabase().emaResponseDao().getById(promptRowId) }

        assertNotNull("Submitted prompt row should still exist", saved)
        assertNotNull("response_timestamp should be set after Submit", saved!!.responseTimestamp)
        assertEquals("Activity label should persist", "Walking", saved.activityLabel)
        assertEquals("Location label should persist", "At work", saved.locationLabel)
        assertEquals("Social label should persist", "Alone", saved.socialLabel)
        assertEquals("Notes should be capped at 200 characters", 200, saved.optionalTags?.length)
        assertEquals("Row should not be marked dismissed on a real Submit", 0, saved.dismissed)
    }

    @Test
    fun skip_marksDismissedWithoutSettingResponseTimestamp() {
        val promptRowId = insertUnansweredPrompt()
        openCheckInViaNotificationTap(promptRowId)

        composeTestRule.onNodeWithText("Check-in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Check-in").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Check-in").assertDoesNotExist()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val saved = runBlocking { DatabaseProvider.getDatabase().emaResponseDao().getById(promptRowId) }

        assertNotNull("Skipped prompt row should still exist", saved)
        assertNull("response_timestamp should remain null after Skip", saved!!.responseTimestamp)
        assertEquals("Row should be marked dismissed after Skip", 1, saved.dismissed)
    }

    @Test
    fun duplicateSuppression_blocksNewPromptWhileOneIsActive_allowsAfterResolved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val dao = DatabaseProvider.getDatabase().emaResponseDao()

        val activeRowId = insertUnansweredPrompt()
        val activeBeforeResolution = runBlocking { dao.getActivePrompt() }
        assertNotNull("An unanswered, non-dismissed prompt should be considered active", activeBeforeResolution)
        assertTrue(
            "EmaWorker should suppress a new prompt while one is active",
            EmaWorker.shouldSuppressNewPrompt(activeBeforeResolution != null)
        )

        runBlocking { dao.markDismissed(activeRowId) }
        val activeAfterResolution = runBlocking { dao.getActivePrompt() }
        assertNull("Dismissing the only prompt should leave no active prompt", activeAfterResolution)
        assertTrue(
            "EmaWorker should allow a new prompt once no active prompt remains",
            !EmaWorker.shouldSuppressNewPrompt(activeAfterResolution != null)
        )
    }
}
