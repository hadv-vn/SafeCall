package com.safecall.callscreening

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [PermissionStatusChecker] (LLD v1.4, FR-13) - object thuan nhan [Context], dung
 * chung boi [PermissionCheckWorker]/`PermissionsModule`/`RoleModule`. Mock cac ham static
 * (`Settings.canDrawOverlays`, `ContextCompat.checkSelfPermission`,
 * `NotificationManagerCompat.from`) qua `mockkStatic` - khong dung Robolectric.
 */
class PermissionStatusCheckerTest {

    private val context = mockk<Context>()

    @Before
    fun setUp() {
        mockkStatic(Settings::class)
        mockkStatic(ContextCompat::class)
        mockkStatic(NotificationManagerCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isGranted SYSTEM_ALERT_WINDOW delegates to Settings canDrawOverlays`() {
        every { Settings.canDrawOverlays(context) } returns true
        assertTrue(PermissionStatusChecker.isGranted(context, PermissionKeys.SYSTEM_ALERT_WINDOW))

        every { Settings.canDrawOverlays(context) } returns false
        assertFalse(PermissionStatusChecker.isGranted(context, PermissionKeys.SYSTEM_ALERT_WINDOW))
    }

    @Test
    fun `isGranted unknown key returns false`() {
        assertFalse(PermissionStatusChecker.isGranted(context, "KHONG_TON_TAI"))
    }

    @Test
    fun `isGranted runtime permission delegates to ContextCompat checkSelfPermission`() {
        every {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(PermissionStatusChecker.isGranted(context, PermissionKeys.READ_CONTACTS))

        every {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED
        assertFalse(PermissionStatusChecker.isGranted(context, PermissionKeys.READ_CONTACTS))
    }

    @Test
    fun `isRoleHeld true chi khi ca isRoleAvailable va isRoleHeld deu true`() {
        val roleManager = mockk<RoleManager>()
        every { context.getSystemService(RoleManager::class.java) } returns roleManager

        every { roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) } returns true
        every { roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) } returns true
        assertTrue(PermissionStatusChecker.isRoleHeld(context))

        every { roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) } returns false
        assertFalse(PermissionStatusChecker.isRoleHeld(context))
    }

    @Test
    fun `isRoleHeld false khi khong co RoleManager tren thiet bi`() {
        every { context.getSystemService(RoleManager::class.java) } returns null
        assertFalse(PermissionStatusChecker.isRoleHeld(context))
    }

    @Test
    fun `areNotificationsEffectivelyEnabled can ca quyen runtime VA kenh OS`() {
        every {
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
        } returns PackageManager.PERMISSION_GRANTED
        val notificationManagerCompat = mockk<NotificationManagerCompat>()
        every { NotificationManagerCompat.from(context) } returns notificationManagerCompat

        every { notificationManagerCompat.areNotificationsEnabled() } returns true
        assertTrue(PermissionStatusChecker.areNotificationsEffectivelyEnabled(context))

        // Quyen runtime co nhung kenh thong bao OS bi tat - van phai coi la KHONG hieu qua.
        every { notificationManagerCompat.areNotificationsEnabled() } returns false
        assertFalse(PermissionStatusChecker.areNotificationsEffectivelyEnabled(context))
    }
}
