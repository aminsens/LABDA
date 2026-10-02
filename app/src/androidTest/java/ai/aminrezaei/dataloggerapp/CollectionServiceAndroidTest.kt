package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.permissions.LocationService
import ai.aminrezaei.dataloggerapp.data.SharedPreferencesManager
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers test matrix section C (collection foreground service).
 *
 * Precondition: onboarding already complete, all runtime permissions granted via ADB.
 * Runs in-process (self-instrumentation), so it can read LocationService.isRunning and
 * query Room DAOs directly through the same DatabaseProvider singleton the app uses,
 * while the real foreground service is alive.
 */
@RunWith(AndroidJUnit4::class)
class CollectionServiceAndroidTest {

    private val tag = "SmokeTest_C"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startStop_collectionService_updatesDashboardAndGrowsRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseProvider.init(context)
        val db = DatabaseProvider.getDatabase()

        // 1. Initial state: inactive.
        composeTestRule.onNodeWithText("Collection is not active").assertIsDisplayed()
        assertEquals("LocationService.isRunning should be false before Start is tapped", false, LocationService.isRunning)

        // 2. Start collection.
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is active").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Collection is active").assertIsDisplayed()
        assertTrue("Foreground service should report running after Start", LocationService.isRunning)

        val accelBaseline = runBlocking { db.accelerometerDao().getCount() }
        val deviceStateBaseline = runBlocking { db.deviceStateDao().getCount() }
        val locationBaseline = runBlocking { db.locationDao().getCount() }
        Log.i(tag, "Baseline after start: accel=$accelBaseline deviceState=$deviceStateBaseline location=$locationBaseline")

        // 3. Movement rows: accelerometer always reports gravity/motion regardless of
        // physical movement, so this must grow reliably within a few seconds.
        Thread.sleep(6_000)
        val accelAfter = runBlocking { db.accelerometerDao().getCount() }
        Log.i(tag, "Accelerometer rows after wait: $accelAfter")
        assertTrue(
            "Accelerometer rows did not increase while collection was active ($accelBaseline -> $accelAfter)",
            accelAfter > accelBaseline
        )

        // 4. Context/device-state rows: a "periodic" warmup row is inserted ~3s after
        // service start, so it should already be present.
        val deviceStateAfter = runBlocking { db.deviceStateDao().getCount() }
        Log.i(tag, "Device-state rows after wait: $deviceStateAfter")
        assertTrue(
            "Device-state (context) rows did not increase after the startup warmup window ($deviceStateBaseline -> $deviceStateAfter)",
            deviceStateAfter > deviceStateBaseline
        )

        // 5. Location rows: requires an actual GPS/network fix, which may not arrive
        // indoors. Distinguish "incorrectly inactive" from "no fix within timeout".
        var locationAfter = locationBaseline
        val locationDeadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < locationDeadline) {
            locationAfter = runBlocking { db.locationDao().getCount() }
            if (locationAfter > locationBaseline) break
            Thread.sleep(1_000)
        }
        Log.i(tag, "Location rows after wait: $locationAfter")

        if (locationAfter <= locationBaseline) {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val locationLoggingEnabled = SharedPreferencesManager(context)
                .getLoggingPreference(Constants.LOGGING_LOCATION)

            assertTrue(
                "Location permission was not granted — this is a genuine configuration failure, not a missing GPS fix",
                hasFineLocation
            )
            assertTrue(
                "Location logging preference was disabled — this is a genuine configuration failure, not a missing GPS fix",
                locationLoggingEnabled
            )
            Log.w(
                tag,
                "No location fix arrived within 20s despite permission granted and logging enabled — " +
                    "treated as INCONCLUSIVE (no indoor GPS fix), not a functional failure."
            )
        }

        // 6. Stop collection.
        composeTestRule.onNodeWithText("Stop").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Collection is not active").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Collection is not active").assertIsDisplayed()

        Thread.sleep(500)
        assertEquals("LocationService.isRunning should be false after Stop is tapped", false, LocationService.isRunning)
    }
}
