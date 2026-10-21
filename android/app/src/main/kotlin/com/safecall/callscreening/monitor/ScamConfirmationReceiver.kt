package com.safecall.callscreening.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Xu ly tap action button tren Notification xac nhan - kenh dam bao (LLD: `ScamConfirmationReceiver`,
 * `onReceive(context, intent)`). Native-to-native hoan toan, hoat dong du app background/killed.
 */
class ScamConfirmationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val promptId = intent.getStringExtra(ScamConfirmationNotifier.EXTRA_PROMPT_ID) ?: return
        val action = when (intent.action) {
            ScamConfirmationNotifier.ACTION_YES -> "YES"
            ScamConfirmationNotifier.ACTION_NO -> "NO"
            else -> return
        }

        // Fire-and-forget tu quan diem UI (notification da bi huy ngay trong resolvePromptAtomically
        // khi co ket qua) - ban than cong viec la cac thao tac SQLite dong bo, nhanh, khong can
        // chay tren background thread rieng (BroadcastReceiver.onReceive co ngan sach ngan nhung
        // du cho vai truy van SQLite don gian).
        PendingPromptRegistry.getInstance(context).resolvePromptAtomically(promptId, action, "NOTIFICATION")
    }
}
