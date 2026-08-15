package com.example.osmandtesttask.ui.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.osmandtesttask.R
import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.common.formatAsPercentage1
import com.example.osmandtesttask.common.ScopeQualifier
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.MapDownloader
import com.example.osmandtesttask.ui.common.notifications.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MapDownloadService : Service() {
    private val downloader: MapDownloader by inject()
    private val notificationUtil: NotificationUtil by inject()
    private val parentScope by inject<CoroutineScope>(named(ScopeQualifier.APP_SCOPE))
    private val serviceScope = parentScope + SupervisorJob()

    private var isFinishingGracefully = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        doStartForeground()
        serviceScope.launch {
            downloader.throttledState.collect { state ->
                updateNotification(state)
                if (state is DownloaderState.Empty) {
                    isFinishingGracefully = true
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (!isFinishingGracefully) {
            downloader.cancelAll()
        }
        super.onDestroy()
    }

    private fun doStartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID, createNotification())
        }
    }

    private fun updateNotification(state: DownloaderState) {
        val text = composeNotificationText(state)
        val notification = createNotification(text)
        notificationUtil.showNotification(
            NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID,
            notification
        )
    }

    private fun composeNotificationText(state: DownloaderState): String {
        return when (state) {
            is DownloaderState.Processing -> {
                when (val downloadState = state.downloadState) {
                    is DownloadState.Progress -> {
                        val percentText = downloadState.progress.formatAsPercentage1()
                        val name = state.activeDownload.displayName
                        val count = state.enqueuedDownloads.size + 1 // include current in count
                        getString(
                            R.string.notification_map_download_progress_text,
                            count,
                            name,
                            percentText
                        )
                    }

                    else -> "" // todo: text for other states
                }
            }

            else -> "" // todo: text for other states
        }
    }

    private fun createNotification(text: String = ""): Notification {
        val title = getString(R.string.app_name)
        return notificationUtil.createNotification(
            title, text, iconRes = R.drawable.ic_action_import, silent = true
        )
    }

    companion object {
        fun start(context: Context) {
            Logs.d("requesting download service start")
            val intent = Intent(context, MapDownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}