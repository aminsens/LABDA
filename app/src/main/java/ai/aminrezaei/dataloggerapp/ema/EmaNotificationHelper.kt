package ai.aminrezaei.dataloggerapp.ema

import ai.aminrezaei.dataloggerapp.MainActivity
import ai.aminrezaei.dataloggerapp.R
import ai.aminrezaei.dataloggerapp.utils.Constants
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object EmaNotificationHelper {

    /**
     * Creates the EMA notification channel. Call once from MainActivity.onCreate().
     * Safe to call repeatedly — Android deduplicates by channel ID.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.EMA_NOTIFICATION_CHANNEL_ID,
                "EMA Check-ins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic activity check-in prompts for the LABDA study"
                enableVibration(true)
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Fires a heads-up notification for a new EMA prompt.
     * @param promptRowId  The Room-generated row ID for this prompt (used for routing and
     *                     notification cancellation).
     */
    fun sendPromptNotification(context: Context, promptRowId: Long) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent — opens MainActivity and tells it to navigate to the EMA screen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constants.EMA_INTENT_ACTION_OPEN, true)
            putExtra(Constants.EMA_INTENT_EXTRA_PROMPT_ROW_ID, promptRowId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            promptRowId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent — fires EmaDismissReceiver to mark dismissed in DB
        val dismissIntent = Intent(context, EmaDismissReceiver::class.java).apply {
            putExtra(Constants.EMA_INTENT_EXTRA_PROMPT_ROW_ID, promptRowId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            // Use a different request code from the open intent to avoid collision
            (promptRowId + 100_000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            Constants.EMA_NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.labda)
            .setContentTitle("Quick check-in")
            .setContentText("How are you right now? Tap to answer 3 short questions.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("How are you right now? Tap to answer 3 short questions about your current activity, location, and company.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        // Use promptRowId as notification ID so each prompt has a unique, cancellable notification
        manager.notify(notificationId(promptRowId), notification)
    }

    /** Cancels the notification for a given prompt row ID. */
    fun cancelNotification(context: Context, promptRowId: Long) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId(promptRowId))
    }

    private fun notificationId(promptRowId: Long): Int =
        Constants.EMA_NOTIFICATION_ID_BASE + (promptRowId % 1000).toInt()
}
