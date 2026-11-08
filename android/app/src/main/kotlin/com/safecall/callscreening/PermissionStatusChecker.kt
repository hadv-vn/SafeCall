package com.safecall.callscreening

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Tach phan "chi kiem tra trang thai quyen" ra khoi [PermissionsModule]/[RoleModule] (can
 * [android.app.Activity]) de dung CHUNG boi [PermissionCheckWorker] (chi co [Context], FR-13,
 * LLD v1.4 muc "Xac nhan ranh gioi Context vs Activity"). KHONG doi hanh vi/tri-state hien co cua
 * 2 class do (NOT_REQUESTED/DENIED qua SharedPreferences van giu nguyen ben PermissionsModule) -
 * object nay chi gom phan core-check dung API nhan [Context].
 */
object PermissionStatusChecker {

    fun isGranted(context: Context, permissionKey: String): Boolean {
        if (permissionKey == PermissionKeys.SYSTEM_ALERT_WINDOW) return Settings.canDrawOverlays(context)
        if (PermissionKeys.isImplicitlyGrantedBelowRuntimeModel(permissionKey)) return true
        val androidPermission = PermissionKeys.RUNTIME_PERMISSION_MAP[permissionKey] ?: return false
        return ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
    }

    fun isRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    /**
     * CHI dung boi [PermissionCheckWorker] - rong hon "co quyen runtime khong": gop ca trang thai
     * nguoi dung tat Thong bao cho app o cap OS. KHONG dung trong PermissionsModule/onboarding -
     * giu nguyen hanh vi onboarding hien tai (chi kiem tra quyen POST_NOTIFICATIONS runtime).
     */
    fun areNotificationsEffectivelyEnabled(context: Context): Boolean {
        return isGranted(context, PermissionKeys.POST_NOTIFICATIONS) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
