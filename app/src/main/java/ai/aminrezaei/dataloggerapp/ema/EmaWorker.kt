package ai.aminrezaei.dataloggerapp.ema

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.components.datamanagement.EmaResponseEntity
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

/**
 * Periodic WorkManager worker that fires EMA prompts.
 *
 * On each execution:
 * 1. Safety-guard: skip if EMA toggle is OFF in SharedPrefs.
 * 2. Quiet-hours check: skip silently if current local time is within the quiet window.
 * 3. Insert a new prompt row in [ema_responses] to get a stable row ID.
 * 4. Dispatch a heads-up notification via [EmaNotificationHelper].
 */
class EmaWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        // 1. Safety guard — honour the toggle even if WorkManager fires after toggle was turned off
        val emaEnabled = prefs.getBoolean(Constants.LOGGING_EMA_PROMPTS, false)
        if (!emaEnabled) {
            Log.d(TAG, "EMA disabled in prefs — skipping prompt")
            return Result.success()
        }

        // 2. Quiet-hours check
        val quietStart = prefs.getString(Constants.KEY_EMA_QUIET_HOURS_START, "22:00") ?: "22:00"
        val quietEnd   = prefs.getString(Constants.KEY_EMA_QUIET_HOURS_END,   "07:00") ?: "07:00"
        if (isInQuietHours(quietStart, quietEnd)) {
            Log.d(TAG, "Within quiet hours ($quietStart–$quietEnd) — skipping prompt")
            return Result.success()
        }

        // 3. Skip if an unanswered, non-dismissed prompt is already active
        DatabaseProvider.init(applicationContext)
        val dao = DatabaseProvider.getDatabase().emaResponseDao()
        val existing = dao.getActivePrompt()
        if (shouldSuppressNewPrompt(existing != null)) {
            Log.d(TAG, "Active EMA prompt already exists (id=${existing?.id}) — skipping new prompt")
            return Result.success()
        }

        // 4. Insert prompt row
        return runCatching {
            val promptTimestamp = System.currentTimeMillis()
            val entity = EmaResponseEntity(
                promptTimestamp   = promptTimestamp,
                responseTimestamp = null,
                activityLabel     = null,
                locationLabel     = null,
                socialLabel       = null,
                optionalTags      = null,
                latencySeconds    = null,
                dismissed         = 0
            )
            val rowId = dao.insert(entity)
            Log.d(TAG, "EMA prompt row inserted: id=$rowId ts=$promptTimestamp")

            // 5. Fire notification
            EmaNotificationHelper.sendPromptNotification(applicationContext, rowId)
            Result.success()
        }.getOrElse { error ->
            Log.e(TAG, "EmaWorker failed", error)
            Result.retry()
        }
    }

    /**
     * Returns true if the current local clock time falls within [quietStart, quietEnd].
     * Handles overnight ranges (e.g. 22:00 – 07:00) correctly.
     */
    private fun isInQuietHours(quietStart: String, quietEnd: String): Boolean {
        return try {
            val (startH, startM) = quietStart.split(":").map { it.toInt() }
            val (endH,   endM)   = quietEnd.split(":").map { it.toInt() }

            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startMinutes = startH * 60 + startM
            val endMinutes   = endH   * 60 + endM

            if (startMinutes <= endMinutes) {
                // Same-day range (e.g. 08:00 – 20:00)
                nowMinutes in startMinutes..endMinutes
            } else {
                // Overnight range (e.g. 22:00 – 07:00)
                nowMinutes >= startMinutes || nowMinutes <= endMinutes
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse quiet hours ($quietStart/$quietEnd) — defaulting to not quiet", e)
            false
        }
    }

    companion object {
        private const val TAG = "EmaWorker"

        /**
         * Pure decision function (extracted for unit testing): a new prompt should be
         * suppressed when there is already an active (unanswered, non-dismissed) prompt.
         */
        fun shouldSuppressNewPrompt(hasActivePrompt: Boolean): Boolean = hasActivePrompt
    }
}
