package com.safecall.callscreening.monitor

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

/** Ket qua tra ve tu `CallMonitorService` cho overlay hien thi (goi dong bo - thao tac SQLite/endCall noi bo, khong qua mang). */
sealed class RingingYesUiOutcome {
    data class Terminated(val outcome: CallTerminationOutcome) : RingingYesUiOutcome()
    object AlreadyResolved : RingingYesUiOutcome()
    object WriteFailed : RingingYesUiOutcome()
}

sealed class RingingNoUiOutcome {
    object Success : RingingNoUiOutcome()
    object AlreadyResolved : RingingNoUiOutcome()
    object WriteFailed : RingingNoUiOutcome()
}

/**
 * `RingingScamOverlay` (LLD/UIUX, ADR-007) - Window overlay toan man hinh ve tu `CallMonitorService`,
 * `WindowManager.addView`/`TYPE_APPLICATION_OVERLAY`. Vong doi doc lap Activity/Flutter engine -
 * chi phu thuoc vong doi `CallMonitorService` (ADR-007 "tieu cuc": neu service bi kill bat thuong
 * ma khong goi [dismiss], overlay co the "dinh" lai - CallMonitorService.onDestroy() PHAI goi
 * dismiss(), xem ghi chu o do).
 *
 * KHONG tu ghi DB/goi `endCall()` - chi UI, nhan ket qua tu callback [onYesPressed]/[onNoPressed]
 * (do CallMonitorService cung cap, chua toan bo logic resolveRingingYes/No) va tu render dung 4
 * trang thai ket qua (UIUX "Phan hoi sau khi bam Co - 4 trang thai ket qua").
 */
class RingingScamOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private var rootView: ViewGroupHandle? = null

    private class ViewGroupHandle(
        val container: LinearLayout,
        val questionGroup: LinearLayout,
        val processingGroup: LinearLayout,
        val resultGroup: LinearLayout,
        val resultText: TextView,
        val errorBanner: TextView,
        val yesButton: Button,
        val noButton: Button,
    )

    fun isShowing(): Boolean = rootView != null

    fun show(
        rawNumber: String?,
        normalizedNumber: String,
        onYesPressed: () -> RingingYesUiOutcome,
        onNoPressed: () -> RingingNoUiOutcome,
    ) {
        if (rootView != null) dismiss()

        val statusLabel = TextView(context).apply {
            text = "Đang đổ chuông..."
            setTextColor(Color.LTGRAY)
            textSize = 14f
        }
        val numberText = TextView(context).apply {
            text = rawNumber ?: normalizedNumber
            setTextColor(Color.WHITE)
            textSize = 28f
            setPadding(0, 16, 0, 16)
        }
        val questionText = TextView(context).apply {
            text = "Đây có phải cuộc gọi lừa đảo không?"
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        val warningText = TextView(context).apply {
            text = "Nếu chọn \"Có\", cuộc gọi sẽ bị ngắt ngay và không thể khôi phục."
            setTextColor(Color.YELLOW)
            textSize = 13f
            setPadding(0, 12, 0, 12)
        }
        val yesButton = Button(context).apply {
            text = "Có, đây là lừa đảo"
            minimumHeight = dp(56)
        }
        val noButton = Button(context).apply {
            text = "Không phải lừa đảo"
            minimumHeight = dp(56)
        }
        val errorBanner = TextView(context).apply {
            setTextColor(Color.RED)
            visibility = View.GONE
        }
        val questionGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(statusLabel)
            addView(numberText)
            addView(questionText)
            addView(warningText)
            addView(errorBanner)
            addView(yesButton)
            addView(noButton)
        }

        val processingText = TextView(context).apply {
            text = "Đang xử lý..."
            setTextColor(Color.WHITE)
        }
        val processingGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            addView(ProgressBar(context))
            addView(processingText)
        }

        val resultText = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
        }
        val understoodButton = Button(context).apply { text = "Đã hiểu" }
        val resultGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(resultText)
            addView(understoodButton)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(questionGroup)
            addView(processingGroup)
            addView(resultGroup)
            isFocusableInTouchMode = true
        }

        val handle = ViewGroupHandle(
            container, questionGroup, processingGroup, resultGroup, resultText, errorBanner, yesButton, noButton,
        )
        rootView = handle

        container.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                // AC9 (gia dinh): back = bo qua tam thoi, KHONG ghi gi - giong AC7 ve mat du lieu.
                dismiss()
                true
            } else {
                false
            }
        }

        yesButton.setOnClickListener {
            showProcessing(handle)
            when (val outcome = onYesPressed()) {
                is RingingYesUiOutcome.Terminated -> showYesResult(handle, outcome.outcome)
                is RingingYesUiOutcome.AlreadyResolved -> {
                    dismiss()
                    toast("Số này đã được xử lý.")
                }
                is RingingYesUiOutcome.WriteFailed -> showWriteFailedError(handle)
            }
        }
        noButton.setOnClickListener {
            showProcessing(handle)
            when (onNoPressed()) {
                RingingNoUiOutcome.Success -> {
                    dismiss()
                    toast("Đã ghi nhận, sẽ không hỏi lại số này.")
                }
                RingingNoUiOutcome.AlreadyResolved -> {
                    dismiss()
                    toast("Số này đã được xử lý.")
                }
                RingingNoUiOutcome.WriteFailed -> showWriteFailedError(handle)
            }
        }
        understoodButton.setOnClickListener { dismiss() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0, // focusable + touchable (mac dinh) - can de nhan click nut va key event back.
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(container, params)
        container.requestFocus()
    }

    /** AC7 - trang thai cuoc goi doi TRUOC khi tuong tac: dong ngay, KHONG toast/ghi gi.
     * Goi tu CallMonitorService.onDestroy() de dam bao khong "dinh" lai (ADR-007 tieu cuc). */
    fun dismiss() {
        val handle = rootView ?: return
        rootView = null
        try {
            windowManager.removeView(handle.container)
        } catch (e: Exception) {
            // View co the da bi remove (vd process dang shutdown) - khong phai loi can xu ly them.
        }
    }

    private fun showProcessing(handle: ViewGroupHandle) {
        handle.questionGroup.visibility = View.GONE
        handle.processingGroup.visibility = View.VISIBLE
        handle.yesButton.isEnabled = false
        handle.noButton.isEnabled = false
    }

    private fun showWriteFailedError(handle: ViewGroupHandle) {
        handle.processingGroup.visibility = View.GONE
        handle.questionGroup.visibility = View.VISIBLE
        handle.yesButton.isEnabled = true
        handle.noButton.isEnabled = true
        handle.errorBanner.visibility = View.VISIBLE
        handle.errorBanner.text =
            "Không ghi được vào danh sách chặn, vui lòng thử lại. Cuộc gọi vẫn đang đổ chuông bình thường."
    }

    private fun showYesResult(handle: ViewGroupHandle, outcome: CallTerminationOutcome) {
        val (message, autoDismiss) = when (outcome) {
            CallTerminationOutcome.SUCCESS -> "Đã chặn số này và ngắt cuộc gọi." to true
            CallTerminationOutcome.NOT_APPLICABLE_CALL_ENDED ->
                "Đã chặn số này. Cuộc gọi đã tự kết thúc trước khi lệnh ngắt được gửi đi." to true
            CallTerminationOutcome.NOT_APPLICABLE_STATE_CHANGED ->
                "Đã chặn số này cho các lần gọi sau. KHÔNG ngắt được cuộc gọi lần này — cuộc gọi " +
                    "có thể đã được trả lời hoặc thiết bị chưa hỗ trợ tự ngắt cuộc gọi." to false
            CallTerminationOutcome.FAILED_EXCEPTION ->
                "Đã chặn số này cho các lần gọi sau. Có lỗi khi ngắt cuộc gọi lần này — cuộc gọi " +
                    "vẫn đang diễn ra bình thường." to false
        }
        if (autoDismiss) {
            dismiss()
            toast(message)
        } else {
            handle.processingGroup.visibility = View.GONE
            handle.resultGroup.visibility = View.VISIBLE
            handle.resultText.text = message
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
