package com.safecall.callscreening

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.MethodChannel

/**
 * Handler quyen runtime (LLD: `PermissionsModule` - `checkPermissionStatus(permission)`,
 * `requestPermission(activity, permission)`).
 *
 * [PermissionKeys.SYSTEM_ALERT_WINDOW] (Phase 6/ADR-007) la TRUONG HOP DAC BIET - khong xin qua
 * dialog runtime thuong (`ActivityCompat.requestPermissions`) ma qua man hinh Settings rieng
 * (`ACTION_MANAGE_OVERLAY_PERMISSION`), KHONG co callback tu dong tra ket qua - Dart phia tren
 * PHAI tu kiem tra lai qua `checkPermissionStatus`/`checkAllPermissionsStatus` khi app resume
 * (UIUX: "Settings redirect + Pending-return step").
 *
 * Phan biet NOT_REQUESTED vs DENIED (chi ap dung cho quyen runtime thuong): Android khong co API
 * truc tiep cho "chua tung xin" - tu luu flag trong SharedPreferences ngay truoc khi goi
 * requestPermissions (quyet dinh trien khai cua agent, LLD khong chi dinh chi tiet nay).
 */
class PermissionsModule(private val activity: Activity) {

    private var pendingResult: MethodChannel.Result? = null
    private var pendingPermissionKey: String? = null

    private val prefs: SharedPreferences
        get() = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun checkPermissionStatus(result: MethodChannel.Result, permissionKey: String) {
        result.success(statusOf(permissionKey).toString())
    }

    fun checkAllPermissionsStatus(result: MethodChannel.Result) {
        val keys = PermissionKeys.RUNTIME_PERMISSION_MAP.keys + PermissionKeys.SYSTEM_ALERT_WINDOW
        val statuses = keys.associateWith { key -> statusOf(key).toString() }
        result.success(statuses)
    }

    // Core-check (co quyen hay khong) tach sang PermissionStatusChecker.isGranted (dung Context,
    // LLD v1.4) - dung chung voi PermissionCheckWorker (FR-13). Phan tri-state NOT_REQUESTED vs
    // DENIED (qua SharedPreferences) VA truong hop SYSTEM_ALERT_WINDOW (khong co NOT_REQUESTED,
    // chi GRANTED/DENIED) giu nguyen y het hanh vi cu - khong doi.
    private fun statusOf(permissionKey: String): PermissionStatus {
        if (PermissionStatusChecker.isGranted(activity, permissionKey)) return PermissionStatus.GRANTED
        if (permissionKey == PermissionKeys.SYSTEM_ALERT_WINDOW) return PermissionStatus.DENIED
        if (!PermissionKeys.RUNTIME_PERMISSION_MAP.containsKey(permissionKey)) return PermissionStatus.DENIED
        val wasRequested = prefs.getBoolean(requestedFlagKey(permissionKey), false)
        return if (wasRequested) PermissionStatus.DENIED else PermissionStatus.NOT_REQUESTED
    }

    fun requestPermission(result: MethodChannel.Result, permissionKey: String) {
        if (permissionKey == PermissionKeys.SYSTEM_ALERT_WINDOW) {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                )
                activity.startActivity(intent)
            }
            // Khong co ket qua dong bo (khac cac quyen runtime thuong) - tra trang thai HIEN TAI
            // ngay, Dart PHAI tu kiem tra lai khi app resume (xem ghi chu dau class).
            result.success(statusOf(permissionKey).toString())
            return
        }
        if (PermissionKeys.isImplicitlyGrantedBelowRuntimeModel(permissionKey)) {
            result.success(PermissionStatus.GRANTED.toString())
            return
        }
        val androidPermission = PermissionKeys.RUNTIME_PERMISSION_MAP[permissionKey]
        if (androidPermission == null) {
            result.error("UNKNOWN_PERMISSION", "Khong nhan dien duoc khoa quyen: $permissionKey", null)
            return
        }
        if (ContextCompat.checkSelfPermission(activity, androidPermission) == PackageManager.PERMISSION_GRANTED) {
            result.success(PermissionStatus.GRANTED.toString())
            return
        }
        if (pendingResult != null) {
            result.error("REQUEST_IN_PROGRESS", "Da co yeu cau requestPermission dang cho ket qua", null)
            return
        }
        pendingResult = result
        pendingPermissionKey = permissionKey
        prefs.edit().putBoolean(requestedFlagKey(permissionKey), true).apply()
        ActivityCompat.requestPermissions(activity, arrayOf(androidPermission), REQUEST_CODE_PERMISSION)
    }

    /** Goi tu MainActivity.onRequestPermissionsResult. Tra true neu da xu ly (requestCode khop). */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE_PERMISSION) return false
        val result = pendingResult
        val key = pendingPermissionKey
        pendingResult = null
        pendingPermissionKey = null
        if (result == null || key == null) return true
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        result.success((if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED).toString())
        return true
    }

    private fun requestedFlagKey(permissionKey: String) = "requested_$permissionKey"

    companion object {
        const val REQUEST_CODE_PERMISSION = 2001
        private const val PREFS_NAME = "safecall_permissions_state"
    }
}
