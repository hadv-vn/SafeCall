package com.safecall.callscreening.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safecall.callscreening.ContactsRepository
import com.safecall.callscreening.InvalidPhoneNumberFormatException
import com.safecall.callscreening.PhoneNumberNormalizer
import com.safecall.callscreening.db.BlocklistRepository
import com.safecall.callscreening.db.CallHistoryRepository
import com.safecall.callscreening.db.CallHistoryWriteException
import com.safecall.callscreening.db.CallRecordDraft
import com.safecall.callscreening.db.SafeCallDatabaseHelper

/**
 * Foreground service lang nghe trang thai cuoc goi (LLD: `CallMonitorService`) - dieu phoi
 * Luong 2 (FR-2/FR-3/FR-4, hoi xac nhan SAU cuoc goi) VA Luong 4 (FR-8, Phase 6, xac nhan NGAY
 * LUC DO CHUONG + ngat cuoc goi - ADR-005/ADR-007). Day la MOT diem goi `endCall()` DUY NHAT
 * trong toan bo codebase (ADR-005 - de audit).
 */
class CallMonitorService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var telecomManager: TelecomManager
    private lateinit var windowManager: WindowManager
    private lateinit var observer: CallStateObserver
    private var lastState: CallState = CallState.IDLE
    private var notificationIdCounter = 1000

    private val dbHelper by lazy { SafeCallDatabaseHelper(applicationContext) }
    private val normalizer = PhoneNumberNormalizer()
    private val callLogResolver by lazy { CallLogNumberResolver(applicationContext) }
    private val contactsRepository by lazy { ContactsRepository(applicationContext) }
    private val blocklistRepository by lazy { BlocklistRepository(dbHelper) }
    private val callHistoryRepository by lazy { CallHistoryRepository(dbHelper) }
    private val eligibilityEvaluator by lazy {
        PromptEligibilityEvaluator(contactsRepository, blocklistRepository)
    }
    private val ringingEligibilityEvaluator by lazy {
        RingingEligibilityEvaluator(contactsRepository, blocklistRepository)
    }
    private val promptRegistry by lazy { PendingPromptRegistry.getInstance(applicationContext) }
    private val ringingOverlay by lazy { RingingScamOverlay(applicationContext, windowManager) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithOwnNotification()

        telephonyManager = getSystemService(TelephonyManager::class.java)
        telecomManager = getSystemService(TelecomManager::class.java)
        windowManager = getSystemService(WindowManager::class.java)
        observer = CallStateObserverFactory.create(telephonyManager)
        try {
            observer.register { state -> onRawCallStateChanged(state) }
        } catch (e: SecurityException) {
            // Thieu READ_PHONE_STATE - khong the chay tiep, lan truyen bang cach tu dung (LLD bang loi).
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (::observer.isInitialized) observer.unregister()
        // ADR-007 "tieu cuc": service bi kill ma khong dismiss overlay co the lam overlay "dinh"
        // lai vinh vien tren man hinh - BAT BUOC dismiss o day.
        if (ringingOverlay.isShowing()) ringingOverlay.dismiss()
        RingingCallNumberHolder.clear()
        super.onDestroy()
    }

    private fun onRawCallStateChanged(state: CallState) {
        if (state == CallState.RINGING && lastState != CallState.RINGING) {
            handleRingingStarted()
        } else if (state != CallState.RINGING && lastState == CallState.RINGING) {
            // AC7: trang thai cuoc goi doi (tra loi HOAC het chuong) TRUOC KHI nguoi dung tuong
            // tac - dong overlay ngay, KHONG ghi gi (pending_prompts row o lai resolved=0, giong
            // cancelActivePromptFor - vo hai, se duoc sieu thi boi prompt moi neu so goi lai).
            if (ringingOverlay.isShowing()) ringingOverlay.dismiss()
        }
        if (state == CallState.IDLE && lastState != CallState.IDLE) {
            handleCallEnded()
            RingingCallNumberHolder.clear()
        }
        lastState = state
    }

    private fun handleRingingStarted() {
        val ringingNumber = RingingCallNumberHolder.get()
        val normalizedNumber = ringingNumber?.normalizedNumber
        val eligibility = ringingEligibilityEvaluator.evaluate(normalizedNumber)
        if (eligibility !is RingingEligibility.Eligible || normalizedNumber == null) return

        promptRegistry.cancelActivePromptFor(normalizedNumber, PromptKind.RINGING)
        // notification_id=0 - khong co Notification Android thuc cho RINGING (overlay rieng theo
        // ADR-007), nhung cot nay NOT NULL trong schema - dung sentinel vo hai (cancel(0) la no-op).
        val promptId = promptRegistry.create(normalizedNumber, PromptKind.RINGING, null, 0)

        ringingOverlay.show(
            rawNumber = ringingNumber.rawNumber,
            normalizedNumber = normalizedNumber,
            onYesPressed = { resolveRingingYes(promptId, normalizedNumber) },
            onNoPressed = { resolveRingingNo(promptId, normalizedNumber) },
        )
    }

    private fun resolveRingingYes(promptId: String, normalizedNumber: String): RingingYesUiOutcome {
        return when (promptRegistry.resolvePromptAtomically(promptId, "YES", "RINGING_OVERLAY")) {
            ResolveOutcome.ALREADY_RESOLVED -> RingingYesUiOutcome.AlreadyResolved
            ResolveOutcome.WRITE_FAILED -> RingingYesUiOutcome.WriteFailed
            ResolveOutcome.SUCCESS -> {
                val outcome = attemptEndCall(normalizedNumber)
                promptRegistry.setTerminationOutcome(promptId, outcome.name)
                RingingYesUiOutcome.Terminated(outcome)
            }
        }
    }

    private fun resolveRingingNo(promptId: String, normalizedNumber: String): RingingNoUiOutcome {
        return when (promptRegistry.resolvePromptAtomically(promptId, "NO", "RINGING_OVERLAY")) {
            ResolveOutcome.SUCCESS -> RingingNoUiOutcome.Success
            ResolveOutcome.ALREADY_RESOLVED -> RingingNoUiOutcome.AlreadyResolved
            ResolveOutcome.WRITE_FAILED -> RingingNoUiOutcome.WriteFailed
        }
    }

    /** Tai xac thuc NGAY TRUOC khi goi `endCall()` (ADR-005 - ngan rui ro race da biet). */
    private fun isStillRingingWithSameNumber(normalizedNumber: String): Boolean {
        return lastState == CallState.RINGING && RingingCallNumberHolder.get()?.normalizedNumber == normalizedNumber
    }

    private fun attemptEndCall(normalizedNumber: String): CallTerminationOutcome {
        if (!isStillRingingWithSameNumber(normalizedNumber)) {
            return if (lastState == CallState.IDLE) {
                CallTerminationOutcome.NOT_APPLICABLE_CALL_ENDED
            } else {
                // OFFHOOK (da duoc tra loi dung luc) hoac so da doi - UIUX gop ca 2 vao STATE_CHANGED.
                CallTerminationOutcome.NOT_APPLICABLE_STATE_CHANGED
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Thieu quyen (tuy chon) - UIUX: gop chung nhan STATE_CHANGED voi ly do "thiet bi chua ho tro".
            return CallTerminationOutcome.NOT_APPLICABLE_STATE_CHANGED
        }
        return try {
            if (telecomManager.endCall()) {
                CallTerminationOutcome.SUCCESS
            } else {
                CallTerminationOutcome.NOT_APPLICABLE_STATE_CHANGED
            }
        } catch (e: SecurityException) {
            CallTerminationOutcome.FAILED_EXCEPTION
        }
    }

    private fun handleCallEnded() {
        val entry = try {
            callLogResolver.resolveMostRecentCall()
        } catch (e: CallLogReadException) {
            null
        } ?: return

        val normalizedNumber = entry.rawNumber?.let { raw ->
            try {
                normalizer.normalize(raw)
            } catch (e: InvalidPhoneNumberFormatException) {
                null
            }
        }

        val draft = CallRecordDraft(
            rawNumber = entry.rawNumber,
            normalizedNumber = normalizedNumber,
            wasAnswered = entry.wasAnswered,
            startedAtEpochMillis = entry.startedAtEpochMillis,
            endedAtEpochMillis = System.currentTimeMillis(),
            durationSeconds = entry.durationSeconds,
        )
        val callRecordId = try {
            callHistoryRepository.insertCallRecord(draft)
        } catch (e: CallHistoryWriteException) {
            null // LLD bang loi: mat 1 ban ghi lich su chap nhan duoc hon treo hot-path - khong co hot-path o day nhung giu nhat quan nguyen tac
        }

        // FR-8 (Phase 6): backfill ket qua da resolve truoc do qua RingingScamOverlay cho CUNG so,
        // neu co - PHAI chay TRUOC evaluatePromptEligibility (dung thu tu Luong 2 pseudocode).
        if (normalizedNumber != null && callRecordId != null) {
            try {
                promptRegistry.flushPendingTerminationOutcomeIfAny(normalizedNumber, callRecordId)
            } catch (e: CallHistoryWriteException) {
                // Khong hong luong chinh - mat 1 lan backfill hiem gap, khong anh huong an toan chan so.
            }
        }

        val eligibility = eligibilityEvaluator.evaluate(entry, normalizedNumber)
        if (eligibility !is PromptEligibility.Eligible || normalizedNumber == null) return

        promptRegistry.cancelActivePromptFor(normalizedNumber, PromptKind.POST_CALL)
        val notificationId = notificationIdCounter++
        val promptId = promptRegistry.create(normalizedNumber, PromptKind.POST_CALL, callRecordId, notificationId)

        if (callRecordId != null) {
            try {
                callHistoryRepository.updateWasPrompted(callRecordId, true)
            } catch (e: CallHistoryWriteException) {
                // Khong hong luong chinh - prompt van tao duoc, chi thieu co flag hien thi.
            }
        }

        ScamConfirmationNotifier.post(
            applicationContext,
            promptId,
            normalizedNumber,
            displayNumber = entry.rawNumber ?: normalizedNumber,
            notificationId = notificationId,
        )
        CallEventStreamHandler.emitScamPrompt(promptId, normalizedNumber)
    }

    private fun startForegroundWithOwnNotification() {
        val channelId = "safecall_call_monitor_foreground"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "SafeCall đang bảo vệ cuộc gọi", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("SafeCall đang hoạt động")
            .setContentText("Đang theo dõi cuộc gọi đến để phát hiện lừa đảo.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Sua sau khi build that phat hien loi (xem ghi chu AndroidManifest.xml): KHONG dung type
        // "phoneCall" (doi hoi MANAGE_OWN_CALLS/vai tro Dialer, SafeCall khong co). "specialUse"
        // (FOREGROUND_SERVICE_TYPE_SPECIAL_USE) chi dinh nghia tu API 34 - duoi do dung type mac
        // dinh (0 = khong khai bao type cu the, van hop le tren API 29-33).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }
}
