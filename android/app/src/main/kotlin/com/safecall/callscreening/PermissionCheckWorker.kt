package com.safecall.callscreening

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.safecall.MainActivity

/**
 * Tac vu nen dinh ky (LLD v1.4, FR-13/PERMISSION-CHECK) - kiem tra lai 6 quyen quan trong, gui/huy
 * thong bao nhac cap lai qua [PERMISSION_CHECK_STEPS] (notificationId CO DINH 9001-9006, idempotent
 * "mien phi" tu chinh API Notification - ADR-008). Enqueue boi [SafeCallApplication] qua
 * `androidx.work.PeriodicWorkRequest` (24h, KHONG qua plugin Flutter `workmanager` - ADR-008
 * quyet dinh 2, Kotlin thuan, khong khoi dong Flutter engine).
 *
 * UX copy PHAI dung ngon ngu "khoang moi ngay", KHONG hua "dung 24h" (ADR-008 muc
 * Consequences/trade-off, UIUX v1.8) - WorkManager chay best-effort duoi Doze/App Standby.
 */
class PermissionCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (step in PERMISSION_CHECK_STEPS) {
                if (step.checker(applicationContext)) {
                    notificationManager.cancel(step.notificationId)
                } else {
                    postMissingPermissionNotification(step)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure() // khong retry som - chu ky 24h sau tu kiem tra lai (LLD pseudocode doWork)
        }
    }

    private fun postMissingPermissionNotification(step: PermissionCheckStep) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PERMISSION_STEP_ID, step.stepId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            step.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(step.title)
            .setContentText(step.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // UIUX: khong nang heads-up - noi dung khong chua PII
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(step.notificationId, notification)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "permission_check_worker"
        const val CHANNEL_ID = "safecall_permission_check"
        const val EXTRA_PERMISSION_STEP_ID = "permissionStepId"
    }
}

private data class PermissionCheckStep(
    val stepId: String,
    val notificationId: Int,
    val title: String,
    val body: String,
    val checker: (Context) -> Boolean,
)

/** Thu tu/noi dung dung theo UIUX v1.7/1.8 muc "Notification nhac cap lai quyen". */
private val PERMISSION_CHECK_STEPS = listOf(
    PermissionCheckStep(
        stepId = "role_call_screening",
        notificationId = 9001,
        title = "SafeCall — Cần cấp lại Vai trò chặn gọi",
        body = "Vai trò chặn gọi đã bị tắt — SafeCall đang KHÔNG tự động chặn được số lừa đảo nào " +
            "trong danh sách chặn. Nhấn để cấp lại.",
        checker = { PermissionStatusChecker.isRoleHeld(it) },
    ),
    PermissionCheckStep(
        stepId = "call_state_and_log",
        notificationId = 9002,
        title = "SafeCall — Cần cấp lại quyền Trạng thái cuộc gọi",
        body = "Thiếu quyền Trạng thái cuộc gọi/Nhật ký cuộc gọi — SafeCall không phát hiện được " +
            "cuộc gọi vừa kết thúc để hỏi xác nhận lừa đảo, và không ghi được lịch sử cuộc gọi. " +
            "Nhấn để cấp lại.",
        checker = {
            PermissionStatusChecker.isGranted(it, PermissionKeys.READ_PHONE_STATE) &&
                PermissionStatusChecker.isGranted(it, PermissionKeys.READ_CALL_LOG)
        },
    ),
    PermissionCheckStep(
        stepId = "notifications",
        notificationId = 9003,
        title = "SafeCall — Cần mở lại Thông báo",
        body = "Thông báo của SafeCall đang bị tắt — bạn sẽ không nhận được câu hỏi xác nhận lừa " +
            "đảo sau mỗi cuộc gọi. Nhấn để mở lại trong Cài đặt.",
        checker = { PermissionStatusChecker.areNotificationsEffectivelyEnabled(it) },
    ),
    PermissionCheckStep(
        stepId = "system_alert_window",
        notificationId = 9004,
        title = "SafeCall — Cần cấp lại quyền Hiển thị đè lên ứng dụng khác",
        body = "Thiếu quyền này — SafeCall KHÔNG thể hiện cảnh báo lừa đảo lúc cuộc gọi đang đổ " +
            "chuông, dưới bất kỳ hình thức nào. Nhấn để mở Cài đặt.",
        checker = { PermissionStatusChecker.isGranted(it, PermissionKeys.SYSTEM_ALERT_WINDOW) },
    ),
    PermissionCheckStep(
        stepId = "contacts",
        notificationId = 9005,
        title = "SafeCall — Cần cấp lại quyền Danh bạ",
        body = "Thiếu quyền Danh bạ — mọi số gọi đến đều bị coi là số lạ, bạn có thể bị hỏi xác " +
            "nhận lừa đảo ngay cả với người quen. Nhấn để cấp lại.",
        checker = { PermissionStatusChecker.isGranted(it, PermissionKeys.READ_CONTACTS) },
    ),
    PermissionCheckStep(
        stepId = "answer_phone_calls",
        notificationId = 9006,
        title = "SafeCall — Cần cấp lại quyền Ngắt cuộc gọi lúc đổ chuông",
        body = "Thiếu quyền này — khi bạn xác nhận 'Có, đây là lừa đảo' lúc cuộc gọi đang đổ " +
            "chuông, SafeCall vẫn chặn số đó cho lần sau nhưng KHÔNG thể tự ngắt cuộc gọi hiện " +
            "tại. Nhấn để cấp lại.",
        checker = { PermissionStatusChecker.isGranted(it, PermissionKeys.ANSWER_PHONE_CALLS) },
    ),
)
