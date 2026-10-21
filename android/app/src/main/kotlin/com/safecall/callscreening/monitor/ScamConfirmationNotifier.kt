package com.safecall.callscreening.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.safecall.MainActivity

/**
 * Notification xac nhan - kenh dam bao DUY NHAT cho FR-2/FR-3/FR-4 (UIUX muc "Notification xac
 * nhan"). Heads-up (IMPORTANCE_HIGH), 2 action button "Co"/"Khong" xu ly boi
 * [ScamConfirmationReceiver] (native-to-native, khong mo app).
 */
object ScamConfirmationNotifier {
    const val ACTION_YES = "com.safecall.action.SCAM_CONFIRM_YES"
    const val ACTION_NO = "com.safecall.action.SCAM_CONFIRM_NO"
    const val EXTRA_PROMPT_ID = "promptId"
    const val EXTRA_NORMALIZED_NUMBER = "normalizedNumber"

    private const val CHANNEL_ID = "safecall_scam_confirmation"

    fun post(context: Context, promptId: String, normalizedNumber: String, displayNumber: String, notificationId: Int) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SafeCall — Xác nhận cuộc gọi")
            .setContentText("Cuộc gọi từ $displayNumber có phải lừa đảo không?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                "Có",
                actionPendingIntent(context, ACTION_YES, promptId, normalizedNumber, notificationId),
            )
            .addAction(
                0,
                "Không",
                actionPendingIntent(context, ACTION_NO, promptId, normalizedNumber, notificationId),
            )
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }

    fun cancel(context: Context, notificationId: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(notificationId)
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        promptId: String,
        normalizedNumber: String,
        notificationId: Int,
    ): PendingIntent {
        val intent = Intent(context, ScamConfirmationReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PROMPT_ID, promptId)
            putExtra(EXTRA_NORMALIZED_NUMBER, normalizedNumber)
        }
        // requestCode gom notificationId+action de 2 action cua CUNG notification co PendingIntent khac nhau.
        val requestCode = notificationId * 10 + if (action == ACTION_YES) 1 else 2
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Xác nhận cuộc gọi lừa đảo",
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel) // idempotent - an toan goi lai moi lan post()
    }
}
