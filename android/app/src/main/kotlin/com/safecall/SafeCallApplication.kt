package com.safecall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safecall.callscreening.PermissionCheckWorker
import java.util.concurrent.TimeUnit

/**
 * Application thuan (FR-13, LLD v1.4 muc "Tich hop SafeCallApplication - dut diem") - KHONG
 * FlutterApplication (xac nhan merged manifest: "${applicationName}" resolve thanh
 * android.app.Application thuan, embedding v2 khong bat buoc FlutterApplication). onCreate() tao
 * notification channel + enqueue [PermissionCheckWorker] dinh ky 24h - logic nay khong can Flutter
 * engine (ADR-005/ADR-008).
 */
class SafeCallApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createPermissionCheckNotificationChannel()
        enqueuePermissionCheckWorker()
    }

    private fun enqueuePermissionCheckWorker() {
        val request = PeriodicWorkRequestBuilder<PermissionCheckWorker>(24, TimeUnit.HOURS).build()
        // KEEP (khong REPLACE) - onCreate() chay MOI LAN process khoi dong, REPLACE se reset lai
        // dong ho 24h moi lan mo app, pha vo tinh "dinh ky du app co mo hay khong" (ADR-008).
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PermissionCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun createPermissionCheckNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                PermissionCheckWorker.CHANNEL_ID,
                "Nhắc cấp lại quyền SafeCall",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
