package ai.aminrezaei.dataloggerapp.ema

import ai.aminrezaei.dataloggerapp.components.datamanagement.DatabaseProvider
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the "Dismiss" action button on EMA prompt notifications.
 * Marks the prompt as dismissed in the database and cancels the notification.
 */
class EmaDismissReceiver : BroadcastReceiver() {

    // A short-lived scope for the DB write; tied to the receiver's lifecycle
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val promptRowId = intent.getLongExtra(Constants.EMA_INTENT_EXTRA_PROMPT_ROW_ID, -1L)
        if (promptRowId == -1L) {
            Log.w(TAG, "EmaDismissReceiver: missing PROMPT_ROW_ID extra — ignoring")
            return
        }

        Log.d(TAG, "Dismissing EMA prompt rowId=$promptRowId")

        // Cancel the notification immediately (synchronous — fine on main thread)
        EmaNotificationHelper.cancelNotification(context, promptRowId)

        // Write dismissed=1 to DB on IO thread
        receiverScope.launch {
            runCatching {
                DatabaseProvider.init(context.applicationContext)
                DatabaseProvider.getDatabase()
                    .emaResponseDao()
                    .markDismissed(promptRowId)
                Log.d(TAG, "EMA prompt $promptRowId marked dismissed in DB")
            }.onFailure { error ->
                Log.e(TAG, "Failed to mark EMA prompt $promptRowId as dismissed", error)
            }
        }
    }

    companion object {
        private const val TAG = "EmaDismissReceiver"
    }
}
