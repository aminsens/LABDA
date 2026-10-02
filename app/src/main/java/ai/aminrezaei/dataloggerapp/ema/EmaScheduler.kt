package ai.aminrezaei.dataloggerapp.ema

import ai.aminrezaei.dataloggerapp.utils.Constants
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling and cancellation of the periodic EMA WorkManager job.
 *
 * Call [schedule] whenever EMA is enabled or the interval changes.
 * Call [cancel] when EMA is disabled.
 */
object EmaScheduler {

    private const val TAG = "EmaScheduler"

    /**
     * Enqueues (or replaces) the periodic EMA worker with the given interval.
     * Using [ExistingPeriodicWorkPolicy.UPDATE] means an existing job at a different interval
     * is replaced immediately — no stale scheduling.
     *
     * @param context       Application or Activity context.
     * @param intervalHours Repeat interval in hours (1, 2, 4, or 8).
     */
    fun schedule(context: Context, intervalHours: Int) {
        val safeInterval = intervalHours.toLong().coerceAtLeast(1L)

        val workRequest = PeriodicWorkRequestBuilder<EmaWorker>(safeInterval, TimeUnit.HOURS)
            .addTag(Constants.EMA_WORK_TAG)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                Constants.EMA_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

        Log.d(TAG, "EMA worker scheduled: interval=${safeInterval}h")
    }

    /**
     * Cancels the periodic EMA worker. Called when the user disables EMA prompts.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelAllWorkByTag(Constants.EMA_WORK_TAG)
        Log.d(TAG, "EMA worker cancelled")
    }
}
