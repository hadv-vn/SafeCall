package com.safecall.callscreening

import android.app.Activity
import android.app.role.RoleManager
import io.flutter.plugin.common.MethodChannel

/**
 * Handler MethodChannel cho ROLE_CALL_SCREENING (LLD: `RoleModule` - `checkRoleStatus()`,
 * `requestRole(activity)`). Khong khai bao <uses-permission> trong Manifest - role duoc xin qua
 * Intent he thong (RoleManager.createRequestRoleIntent), ket qua tra ve qua onActivityResult.
 *
 * minSdkVersion = 29 (ADR-006) > 29 (RoleManager co tu API 29) nen khong can fallback cho API cu hon.
 */
class RoleModule(private val activity: Activity) {

    private var pendingResult: MethodChannel.Result? = null

    private val roleManager: RoleManager?
        get() = activity.getSystemService(RoleManager::class.java)

    fun checkRoleStatus(result: MethodChannel.Result) {
        result.success(isRoleHeld())
    }

    private fun isRoleHeld(): Boolean {
        return roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }

    fun requestRole(result: MethodChannel.Result) {
        val manager = roleManager
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            // Thiet bi khong ho tro ROLE_CALL_SCREENING (hiem, OEM tuy bien) - bao loi ro rang,
            // khong gia vo thanh cong.
            result.error("ROLE_UNAVAILABLE", "ROLE_CALL_SCREENING khong kha dung tren thiet bi nay", null)
            return
        }
        if (isRoleHeld()) {
            result.success(true)
            return
        }
        if (pendingResult != null) {
            result.error("REQUEST_IN_PROGRESS", "Da co yeu cau requestRole dang cho ket qua", null)
            return
        }
        pendingResult = result
        val intent = manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        activity.startActivityForResult(intent, REQUEST_CODE_ROLE)
    }

    /** Goi tu MainActivity.onActivityResult. Tra true neu da xu ly (requestCode khop). */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CODE_ROLE) return false
        val result = pendingResult
        pendingResult = null
        result?.success(resultCode == Activity.RESULT_OK && isRoleHeld())
        return true
    }

    companion object {
        const val REQUEST_CODE_ROLE = 1001
    }
}
