package com.safecall.callscreening.monitor

import io.flutter.plugin.common.EventChannel

/**
 * StreamHandler singleton cho EventChannel `com.safecall/call_events` (LLD: `CallEventBridge`,
 * "duong tang cuong"). [emitScamPrompt] la no-op neu khong co sink dang lang nghe (app khong
 * foreground) - dung HLD "chi khi app foreground", KHONG phai nguon su that (Notification la
 * nguon su that duy nhat, xem [ScamConfirmationNotifier]).
 */
object CallEventStreamHandler : EventChannel.StreamHandler {
    private var sink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        sink = events
    }

    override fun onCancel(arguments: Any?) {
        sink = null
    }

    fun emitScamPrompt(promptId: String, normalizedNumber: String) {
        sink?.success(
            mapOf(
                "promptId" to promptId,
                "normalizedNumber" to normalizedNumber,
            ),
        )
    }
}
