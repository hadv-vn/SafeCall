package com.safecall.callscreening

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import java.util.concurrent.Executors
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test FR-13 AC1-AC4 (PERMISSION-CHECK) - goi `PermissionCheckWorker.doWork()` THUC
 * qua `TestWorkerBuilder` (androidx.work:work-testing) tren AVD/thiet bi, dong gap test da ghi
 * trong RTM/Dashboard ("PermissionCheckWorker.doWork() chua chay qua WorkManager thuc"). Doi lap
 * voi unit test `PermissionStatusCheckerTest.kt` (mock qua MockK) - test nay xac nhan hanh vi tren
 * CHINH NotificationManager/PackageManager thuc cua he thong, khong qua mock.
 *
 * ================================= DIEU KIEN TIEN QUYET (BAT BUOC) =================================
 * KHONG duoc goi `pm revoke`/`pm grant` TU BEN TRONG test (@Before/@Test/@After) - da xac nhan
 * thuc te tren AVD: thu hoi 1 runtime permission cua process DANG CHAY se khien he dieu hanh
 * CHU DONG GIET process do ngay (ly do an ninh chuan cua Android, khong phai bug) - logcat:
 * "ActivityManager: Killing ...: permissions revoked" - vi instrumentation chay LONG trong
 * process cua app duoc test, lenh nay tu giet luon tien trinh dang chay chinh test nay, crash
 * toan bo instrumentation ("Process crashed", INSTRUMENTATION_CODE: 0).
 *
 * Vi vay PHAI thay doi quyen READ_CONTACTS TU BEN NGOAI (adb, truoc khi goi `am instrument`),
 * va 2 test duoi day PHAI duoc goi RIENG qua 2 lan `am instrument -e class` khac nhau:
 *
 *   1) adb shell pm revoke com.safecall android.permission.READ_CONTACTS
 *      adb shell am instrument -w \
 *        -e class com.safecall.callscreening.PermissionCheckWorkerInstrumentedTest#missingContactsPermission_postsNotificationRepeatedly \
 *        com.safecall.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   2) adb shell pm grant com.safecall android.permission.READ_CONTACTS
 *      adb shell am instrument -w \
 *        -e class com.safecall.callscreening.PermissionCheckWorkerInstrumentedTest#grantedContactsPermission_cancelsNotification \
 *        com.safecall.test/androidx.test.runner.AndroidJUnitRunner
 * ======================================================================================================
 *
 * Chon buoc "contacts" (notificationId 9005) lam dai dien - khong anh huong role CALL_SCREENING
 * (can giu nguyen cho FR-11/FR-12) va khong can dialog UI nao.
 *
 * KHONG cover AC5 (tap thong bao dieu huong dung buoc - can Espresso + Activity thuc, ngoai pham
 * vi test Worker nay) va AC6 (lich WorkManager periodic 24h thuc qua Doze/App Standby - khong test
 * duoc "dung gio tuyet doi", da ghi nhan trong Test Plan).
 */
@RunWith(AndroidJUnit4::class)
class PermissionCheckWorkerInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    private val notificationManager by lazy {
        targetContext.getSystemService(NotificationManager::class.java)
            ?: error("NotificationManager khong co san tren thiet bi nay")
    }

    /**
     * Gia dinh TIEN QUYET: READ_CONTACTS da bi `pm revoke` TU BEN NGOAI truoc khi test nay chay
     * (xem DIEU KIEN TIEN QUYET o dau file) - test KHONG tu revoke.
     */
    @Test
    fun missingContactsPermission_postsNotificationRepeatedly() {
        notificationManager.cancel(CONTACTS_NOTIFICATION_ID)

        // AC2 - phat hien thieu quyen -> dang thong bao dung notificationId 9005.
        runWorkerOnce()
        assertTrue(
            "Notification 9005 (Danh ba) phai duoc dang khi thieu READ_CONTACTS",
            isNotificationActive(CONTACTS_NOTIFICATION_ID),
        )

        // AC3 - chu ky tiep theo van thieu quyen -> KHONG im lang, van con thong bao (notify() lai,
        // khong phai chi 1 lan). Tu cancel() truoc de phan biet ro "notify() lai" voi "con sot lai
        // tu lan truoc".
        notificationManager.cancel(CONTACTS_NOTIFICATION_ID)
        runWorkerOnce()
        assertTrue(
            "Notification 9005 phai duoc notify() LAI o chu ky thu 2 neu quyen van thieu",
            isNotificationActive(CONTACTS_NOTIFICATION_ID),
        )
    }

    /**
     * Gia dinh TIEN QUYET: READ_CONTACTS da duoc `pm grant` TU BEN NGOAI truoc khi test nay chay.
     * Tu dang 1 thong bao "gia lap da ton tai tu chu ky truoc" (khong the mang trang thai qua 2
     * lan `am instrument` rieng) truoc khi goi doWork(), de kiem AC4 that su la doWork() CHU DONG
     * cancel(), khong phai "vi tu dau da khong co gi".
     */
    @Test
    fun grantedContactsPermission_cancelsNotification() {
        postFakeExistingNotification()
        assertTrue(
            "Tien dieu kien test sai: notification 9005 phai TON TAI truoc khi goi doWork()",
            isNotificationActive(CONTACTS_NOTIFICATION_ID),
        )

        // AC4 - quyen da duoc cap lai -> doWork() tu cancel() thong bao, khong con hien.
        runWorkerOnce()
        assertFalse(
            "Notification 9005 phai duoc cancel() sau khi READ_CONTACTS da duoc cap lai",
            isNotificationActive(CONTACTS_NOTIFICATION_ID),
        )
    }

    private fun runWorkerOnce() {
        val worker = TestWorkerBuilder.from(
            targetContext,
            PermissionCheckWorker::class.java,
            Executors.newSingleThreadExecutor(),
        ).build()
        val result = worker.doWork()
        assertTrue(
            "doWork() phai tra ve Result.success() (khong co exception nao duoc nem trong 6 buoc kiem tra)",
            result is ListenableWorker.Result.Success,
        )
    }

    private fun isNotificationActive(notificationId: Int): Boolean =
        notificationManager.activeNotifications.any { it.id == notificationId }

    private fun postFakeExistingNotification() {
        val notification = NotificationCompat.Builder(targetContext, PermissionCheckWorker.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Test setup - gia lap thong bao chu ky truoc")
            .setContentText("Se bi doWork() cancel() neu AC4 dung")
            .build()
        notificationManager.notify(CONTACTS_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CONTACTS_NOTIFICATION_ID = 9005
    }
}
