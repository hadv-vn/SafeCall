package com.safecall.callscreening

import android.content.ComponentName
import android.content.ContentValues
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safecall.callscreening.db.SafeCallDatabaseHelper
import java.io.FileInputStream
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test FR-12 AC3 (chan so an danh/Private Caller ID) - tiem cuoc goi vao Telecom
 * framework THUC tren AVD/thiet bi qua mot ConnectionService test harness MANAGED
 * ([TestConnectionService], KHONG self-managed), buoc Telecom goi dung pipeline
 * `CallScreeningService` THUC cua SafeCall (khong mock) - dong gap test da ghi trong
 * `docs/testing/ManualQATestScript-SafeCall-Phase1.md` ("adb emu gsm call khong mo phong duoc so
 * an danh - de lai cho thiet bi that"). Doi lap voi unit test
 * `src/test/kotlin/com/safecall/callscreening/CallScreeningServiceTest.kt` (mock BUOC 1 qua MockK
 * truc tiep tren `Call.Details.handlePresentation`, KHONG dong vao/trung lap voi file do) - test
 * nay xac nhan hanh vi tren CHINH Telecom framework, khong qua mock.
 *
 * ================================= DIEU KIEN TIEN QUYET =================================
 * KHONG tu dong hoa duoc tu trong test nay - phai dam bao TRUOC khi chay tren AVD/thiet bi:
 *   1. App `com.safecall` da duoc gan ROLE_CALL_SCREENING (pre-req da co san cho toan bo
 *      ManualQATestScript, KHONG phai rieng cho test nay):
 *      `adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.safecall`
 *   2. Viec dang ky + enable PhoneAccount cua [TestConnectionService] duoc TEST TU LAM trong
 *      [setUp]/[tearDown] (qua shell `telecom set-phone-account-enabled`, pattern xac nhan tu app
 *      nguon mo "Phony") - KHONG can thao tac tay nao them ngoai (1).
 * ==========================================================================================
 *
 * Xac minh ket qua: doc TRUC TIEP bang `call_history` qua [SafeCallDatabaseHelper] (dung chung
 * file DB voi app thuc, giong CallScreeningService.kt san pham) - KHONG qua
 * `CallHistoryRepository` vi repository nay KHONG co method doc (chi `insertCallRecord`/
 * `updateWasPrompted`/`recordUserConfirmation` - doc call_history la trach nhiem tang Dart/
 * CallHistoryScreen trong san pham thuc, khong co API Kotlin san co cho viec nay). Tuong tu,
 * KHONG co setter cong khai nao cho `blockAnonymousEnabled` (Settings screen ghi truc tiep qua
 * sqflite ben Dart) - test tu UPDATE thang cot `blocklist_meta.block_anonymous_enabled` qua cung
 * [SafeCallDatabaseHelper].
 *
 * Gioi han da biet (KHONG phai bug san pham - ghi lai minh bach): khong co hook/IdlingResource
 * nao de biet CHINH XAC khi nao CallScreeningService.onScreenCall da chay xong (IPC noi bo cua
 * Telecom, khong quan sat duoc tu process test) - 3 test "BI CHAN" cho (poll) toi khi thay ban ghi
 * call_history xuat hien trong [AWAIT_TIMEOUT_MS]; test "KHONG BI CHAN" phai cho CO DINH
 * [SETTLE_DELAY_MS] roi moi khang dinh "khong co gi duoc ghi" - day la trade-off chap nhan duoc
 * cho instrumented test loai nay (khong co hook de thay the), co the gay flaky NHE tren thiet bi
 * cham qua muc.
 */
@RunWith(AndroidJUnit4::class)
class AnonymousCallBlockingInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val testContext = instrumentation.context
    private val targetContext = instrumentation.targetContext

    private val telecomManager by lazy {
        targetContext.getSystemService(TelecomManager::class.java)
            ?: error("TelecomManager khong co san tren thiet bi nay")
    }

    // Dung targetContext (package "com.safecall") - DB file CHUNG voi app thuc (xem
    // SafeCallDatabaseHelper.DATABASE_FILE_NAME), giong dung context ma CallScreeningService san
    // pham dung (`applicationContext`).
    private val dbHelper by lazy { SafeCallDatabaseHelper(targetContext) }

    // ComponentName dung testContext (package cua CHINH androidTest APK, vd "com.safecall.test")
    // vi TestConnectionService duoc khai bao trong AndroidManifest.xml cua androidTest, KHONG phai
    // manifest cua app "com.safecall".
    private val accountHandle by lazy {
        PhoneAccountHandle(ComponentName(testContext, TestConnectionService::class.java), ACCOUNT_ID)
    }

    @Before
    fun setUp() {
        // DB CHUNG voi app that (khong rieng cho test) - don dep truoc de khong bi anh huong du
        // lieu con sot lai tu lan chay/thiet bi truoc.
        dbHelper.writableDatabase.delete("call_history", null, null)

        val account = PhoneAccount.builder(accountHandle, "SafeCall Test Harness")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .build()
        telecomManager.registerPhoneAccount(account)

        // Enable 1 lan qua shell - KHONG co API cong khai nao de app tu enable PhoneAccount cua
        // chinh minh (xac nhan tu pattern app nguon mo "Phony"). Idempotent - chay lai nhieu lan
        // (moi test mot lan) khong gay loi.
        val componentFlat = ComponentName(testContext, TestConnectionService::class.java).flattenToShortString()
        runShellCommand("telecom set-phone-account-enabled $componentFlat $ACCOUNT_ID 0")
    }

    @After
    fun tearDown() {
        TestConnectionService.endActiveConnection()
        try {
            telecomManager.unregisterPhoneAccount(accountHandle)
        } catch (e: Exception) {
            // Chi don dep - khong de loi teardown che mat ket qua assertion thuc su cua test.
        }
        dbHelper.writableDatabase.delete("call_history", null, null)
    }

    // =====================================================================================
    // FR-12 AC3 - blockAnonymousEnabled=true: ca 3 gia tri handlePresentation deu bi chan NGAY,
    // ghi call_history voi decisionReason=BLOCK_TOGGLE_ANONYMOUS (doi xung unit test BUOC 1).
    // =====================================================================================

    @Test
    fun presentationRestricted_blockAnonymousEnabled_blocksAndRecordsBlockToggleAnonymous() {
        setBlockAnonymousEnabled(true)

        addIncomingCall(TelecomManager.PRESENTATION_RESTRICTED)

        val record = awaitCallHistoryRecord()
            ?: failTest(
                "Khong thay ban ghi call_history sau khi tiem cuoc goi PRESENTATION_RESTRICTED " +
                    "voi blockAnonymousEnabled=true trong vong $AWAIT_TIMEOUT_MS ms",
            )
        assertEquals(1, record.wasBlocked)
        assertEquals("BLOCK_TOGGLE_ANONYMOUS", record.decisionReason)
    }

    @Test
    fun presentationUnknown_blockAnonymousEnabled_blocksAndRecordsBlockToggleAnonymous() {
        setBlockAnonymousEnabled(true)

        addIncomingCall(TelecomManager.PRESENTATION_UNKNOWN)

        val record = awaitCallHistoryRecord()
            ?: failTest(
                "Khong thay ban ghi call_history sau khi tiem cuoc goi PRESENTATION_UNKNOWN " +
                    "voi blockAnonymousEnabled=true trong vong $AWAIT_TIMEOUT_MS ms",
            )
        assertEquals(1, record.wasBlocked)
        assertEquals("BLOCK_TOGGLE_ANONYMOUS", record.decisionReason)
    }

    @Test
    fun presentationPayphone_blockAnonymousEnabled_blocksAndRecordsBlockToggleAnonymous() {
        setBlockAnonymousEnabled(true)

        addIncomingCall(TelecomManager.PRESENTATION_PAYPHONE)

        val record = awaitCallHistoryRecord()
            ?: failTest(
                "Khong thay ban ghi call_history sau khi tiem cuoc goi PRESENTATION_PAYPHONE " +
                    "voi blockAnonymousEnabled=true trong vong $AWAIT_TIMEOUT_MS ms",
            )
        assertEquals(1, record.wasBlocked)
        assertEquals("BLOCK_TOGGLE_ANONYMOUS", record.decisionReason)
    }

    // =====================================================================================
    // FR-12 AC3 - blockAnonymousEnabled=false: so an danh KHONG bi chan, CallScreeningService
    // KHONG tu ghi call_history tai thoi diem screening cho nhanh allow (xem comment
    // CallScreeningService.onScreenCall - chi CallMonitorService ghi SAU khi cuoc goi ket thuc).
    // =====================================================================================

    @Test
    fun presentationRestricted_blockAnonymousDisabled_doesNotBlock() {
        setBlockAnonymousEnabled(false)

        addIncomingCall(TelecomManager.PRESENTATION_RESTRICTED)

        // Khong co tin hieu "pipeline da chay xong ma khong lam gi" nao khac de cho (xem ghi chu
        // "Gioi han da biet" dau file) - doi 1 khoang co dinh truoc khi khang dinh.
        Thread.sleep(SETTLE_DELAY_MS)
        val record = latestCallHistoryRecord()
        assertNull("Khong duoc ghi call_history khi blockAnonymousEnabled=false (so an danh)", record)
    }

    // ---------------------------------------------------------------------------------------

    private fun setBlockAnonymousEnabled(enabled: Boolean) {
        val values = ContentValues().apply { put("block_anonymous_enabled", if (enabled) 1 else 0) }
        dbHelper.writableDatabase.update("blocklist_meta", values, "id = 1", null)
    }

    private fun addIncomingCall(presentation: Int) {
        val extras = Bundle().apply { putInt(TestConnectionService.EXTRA_PRESENTATION, presentation) }
        telecomManager.addNewIncomingCall(accountHandle, extras)
    }

    private data class CallHistoryRow(val wasBlocked: Int, val decisionReason: String?)

    private fun latestCallHistoryRecord(): CallHistoryRow? {
        dbHelper.readableDatabase.rawQuery(
            "SELECT was_blocked, decision_reason FROM call_history ORDER BY id DESC LIMIT 1",
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return CallHistoryRow(wasBlocked = cursor.getInt(0), decisionReason = cursor.getString(1))
        }
    }

    private fun awaitCallHistoryRecord(timeoutMs: Long = AWAIT_TIMEOUT_MS): CallHistoryRow? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            latestCallHistoryRecord()?.let { return it }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return latestCallHistoryRecord()
    }

    /** `Assert.fail(String)` cua JUnit khai bao kieu tra ve la `Unit` (tu Java `void`) khi goi tu
     * Kotlin - KHONG dung truc tiep duoc o ben phai toan tu `?:` (can kieu `Nothing`). Luon throw
     * `AssertionError` TRUOC khi toi dong `throw` thu 2 (khong bao gio chay toi, chi de thoa man
     * type checker cua Kotlin). */
    private fun failTest(message: String): Nothing {
        Assert.fail(message)
        throw AssertionError(message)
    }

    /** `UiAutomation.executeShellCommand` tra ve ParcelFileDescriptor NGAY (non-blocking) - PHAI
     * doc den EOF de dam bao process shell con da thuc su chay xong truoc khi tiep tuc. */
    private fun runShellCommand(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
        }
    }

    companion object {
        private const val ACCOUNT_ID = "safecall_test_harness_account"
        private const val AWAIT_TIMEOUT_MS = 10_000L
        private const val POLL_INTERVAL_MS = 200L
        private const val SETTLE_DELAY_MS = 3_000L
    }
}
