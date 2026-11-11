package com.frpboy.seali

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import com.frpboy.seali.App.Companion.context
import com.frpboy.seali.download.DownloaderV2
import com.frpboy.seali.util.NotificationUtil
import com.frpboy.seali.util.ToastUtil
import com.yausername.youtubedl_android.YoutubeDL
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

open class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {
    val downloader = get<DownloaderV2>()

    companion object {
        private const val TAG = "CancelReceiver"
        private const val PACKAGE_NAME_PREFIX = "com.frpboy.seali."
        private const val LEGACY_PACKAGE_NAME_PREFIX = "com.junkfood.seal."

        const val ACTION_CANCEL_TASK = 0
        const val ACTION_ERROR_REPORT = 1

        const val ACTION_KEY = PACKAGE_NAME_PREFIX + "action"
        const val TASK_ID_KEY = PACKAGE_NAME_PREFIX + "taskId"

        const val NOTIFICATION_ID_KEY = PACKAGE_NAME_PREFIX + "notificationId"
        const val ERROR_REPORT_KEY = PACKAGE_NAME_PREFIX + "error_report"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        // PendingIntents created before the namespace move keep their original extra keys.
        val prefix = if (intent.hasExtra(ACTION_KEY)) PACKAGE_NAME_PREFIX else LEGACY_PACKAGE_NAME_PREFIX
        val notificationId = intent.getIntExtra(prefix + "notificationId", 0)
        val action = intent.getIntExtra(prefix + "action", ACTION_CANCEL_TASK)
        Log.d(TAG, "onReceive: $action")
        when (action) {
            ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(prefix + "taskId")
                cancelTask(taskId, notificationId)
            }

            ACTION_ERROR_REPORT -> {
                val errorReport = intent.getStringExtra(prefix + "error_report")
                if (!errorReport.isNullOrEmpty()) copyErrorReport(errorReport, notificationId)
            }
        }
    }

    private fun cancelTask(taskId: String?, notificationId: Int) {
        if (taskId.isNullOrEmpty()) return
        NotificationUtil.cancelNotification(notificationId)
        val res = downloader.cancel(taskId)
        if (res) {
            Log.d(TAG, "Task (id:$taskId) was killed.")
        } else {
            // todo: reserved for custom commands
            YoutubeDL.destroyProcessById(taskId)
            Downloader.onProcessCanceled(taskId)
        }
    }

    private fun copyErrorReport(error: String, notificationId: Int) {
        App.clipboard.setPrimaryClip(ClipData.newPlainText(null, error))
        context.let { ToastUtil.makeToastSuspend(it.getString(R.string.error_copied)) }
        NotificationUtil.cancelNotification(notificationId)
    }
}
