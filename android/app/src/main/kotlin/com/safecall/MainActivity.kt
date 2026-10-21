package com.safecall

import android.content.Intent
import androidx.core.content.ContextCompat
import com.safecall.callscreening.ContactsLookupModule
import com.safecall.callscreening.PermissionsModule
import com.safecall.callscreening.RoleModule
import com.safecall.callscreening.monitor.CallEventStreamHandler
import com.safecall.callscreening.monitor.CallMonitorService
import com.safecall.callscreening.monitor.PendingPromptRegistry
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

/**
 * Entry point Flutter. Dispatch MethodChannel/EventChannel:
 * - [PERMISSIONS_CHANNEL]: RoleModule/PermissionsModule (Phase 1).
 * - [CONTACTS_CHANNEL]: ContactsLookupModule (Phase 2, FR-5/AC8).
 * - [CALL_EVENTS_CHANNEL] (EventChannel) + [CALL_EVENTS_RESPONSE_CHANNEL] (MethodChannel):
 *   CallEventBridge/ScamConfirmationDialog phia Dart - "duong tang cuong" FR-2/3/4 (Phase 4).
 * - onStart(): khoi dong `CallMonitorService` (foreground service, Phase 4) moi lan Activity vao
 *   foreground - an toan goi lai nhieu lan (Android khong goi lai onCreate() cho service dang chay).
 */
class MainActivity : FlutterActivity() {

    private lateinit var roleModule: RoleModule
    private lateinit var permissionsModule: PermissionsModule
    private lateinit var contactsLookupModule: ContactsLookupModule

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        roleModule = RoleModule(this)
        permissionsModule = PermissionsModule(this)
        contactsLookupModule = ContactsLookupModule(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, PERMISSIONS_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "checkRoleStatus" -> roleModule.checkRoleStatus(result)
                    "requestRole" -> roleModule.requestRole(result)
                    "checkPermissionStatus" -> {
                        val key = call.argument<String>("permission")
                        if (key == null) {
                            result.error("INVALID_ARGUMENT", "Thieu tham so 'permission'", null)
                        } else {
                            permissionsModule.checkPermissionStatus(result, key)
                        }
                    }
                    "requestPermission" -> {
                        val key = call.argument<String>("permission")
                        if (key == null) {
                            result.error("INVALID_ARGUMENT", "Thieu tham so 'permission'", null)
                        } else {
                            permissionsModule.requestPermission(result, key)
                        }
                    }
                    "checkAllPermissionsStatus" -> permissionsModule.checkAllPermissionsStatus(result)
                    else -> result.notImplemented()
                }
            }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CONTACTS_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "isNumberInContacts" -> {
                        val number = call.argument<String>("normalizedNumber")
                        if (number == null) {
                            result.error("INVALID_ARGUMENT", "Thieu tham so 'normalizedNumber'", null)
                        } else {
                            contactsLookupModule.isNumberInContacts(result, number)
                        }
                    }
                    "countContactsMatchingPrefix" -> {
                        val prefix = call.argument<String>("normalizedPrefix")
                        if (prefix == null) {
                            result.error("INVALID_ARGUMENT", "Thieu tham so 'normalizedPrefix'", null)
                        } else {
                            contactsLookupModule.countContactsMatchingPrefix(result, prefix)
                        }
                    }
                    else -> result.notImplemented()
                }
            }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CALL_EVENTS_CHANNEL)
            .setStreamHandler(CallEventStreamHandler)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CALL_EVENTS_RESPONSE_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "sendDialogResponse" -> {
                        val promptId = call.argument<String>("promptId")
                        val action = call.argument<String>("action")
                        if (promptId == null || action == null) {
                            result.error("INVALID_ARGUMENT", "Thieu 'promptId'/'action'", null)
                        } else {
                            val outcome = PendingPromptRegistry.getInstance(this)
                                .resolvePromptAtomically(promptId, action, "DIALOG")
                            result.success(outcome.name)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // Khoi dong tu day (khong doi xac nhan quyen rieng) - neu thieu READ_PHONE_STATE, service
        // tu stopSelf() ngay trong onCreate() (SecurityException da duoc bat o do).
        ContextCompat.startForegroundService(this, Intent(this, CallMonitorService::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        roleModule.onActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsModule.onRequestPermissionsResult(requestCode, grantResults)
    }

    companion object {
        const val PERMISSIONS_CHANNEL = "com.safecall/permissions"
        const val CONTACTS_CHANNEL = "com.safecall/contacts"
        const val CALL_EVENTS_CHANNEL = "com.safecall/call_events"
        const val CALL_EVENTS_RESPONSE_CHANNEL = "com.safecall/call_events_response"
    }
}
