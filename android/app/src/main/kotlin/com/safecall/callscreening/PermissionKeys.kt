package com.safecall.callscreening

import android.Manifest
import android.os.Build

/**
 * Khoa quyen dung chung giua Dart (PermissionsBridge) va Kotlin (PermissionsModule) -
 * PHAI khop tuyet doi voi cac hang so chuoi o lib/permissions/permission_keys.dart.
 */
object PermissionKeys {
    const val READ_PHONE_STATE = "READ_PHONE_STATE"
    const val READ_CALL_LOG = "READ_CALL_LOG"
    const val READ_CONTACTS = "READ_CONTACTS"
    const val POST_NOTIFICATIONS = "POST_NOTIFICATIONS"

    /** (Moi - Phase 6/FR-8/ADR-007) Bat buoc cho TOAN BO FR-8 - xin qua Settings, KHONG qua dialog runtime thuong. */
    const val SYSTEM_ALERT_WINDOW = "SYSTEM_ALERT_WINDOW"

    /** (Moi - Phase 6/FR-8/ADR-005) Tuy chon (suy giam nhe nhang) - chi anh huong nhanh ngat cuoc goi. */
    const val ANSWER_PHONE_CALLS = "ANSWER_PHONE_CALLS"

    /** Anh xa khoa -> permission string Android thuc, chi gom quyen runtime THUONG (xin qua
     * ActivityCompat.requestPermissions) - KHONG gom [SYSTEM_ALERT_WINDOW] (xin qua Settings, xu
     * ly rieng trong PermissionsModule, khong qua map nay). */
    val RUNTIME_PERMISSION_MAP: Map<String, String> = buildMap {
        put(READ_PHONE_STATE, Manifest.permission.READ_PHONE_STATE)
        put(READ_CALL_LOG, Manifest.permission.READ_CALL_LOG)
        put(READ_CONTACTS, Manifest.permission.READ_CONTACTS)
        // POST_NOTIFICATIONS chi ton tai nhu runtime permission tu API 33 (Manifest.permission.POST_NOTIFICATIONS
        // chi duoc dinh nghia o API 33+ trong SDK, nhung hang so string ("android.permission.POST_NOTIFICATIONS")
        // on dinh qua cac API level - dung literal de tranh phu thuoc compileSdk cho logic runtime).
        put(POST_NOTIFICATIONS, "android.permission.POST_NOTIFICATIONS")
        // ANSWER_PHONE_CALLS co tu API 26, la runtime permission thuong - xin qua dialog binh thuong.
        put(ANSWER_PHONE_CALLS, Manifest.permission.ANSWER_PHONE_CALLS)
    }

    /**
     * POST_NOTIFICATIONS khong phai runtime permission truoc API 33 (Build.VERSION_CODES.TIRAMISU) -
     * coi nhu da cap (GRANTED) mac dinh tren API 29-32, khong goi ActivityCompat cho truong hop nay.
     */
    fun isImplicitlyGrantedBelowRuntimeModel(key: String): Boolean {
        return key == POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }
}

/** Trang thai quyen tra ve qua MethodChannel - khop enum PermissionStatus ben Dart. */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED;

    override fun toString(): String = name
}
