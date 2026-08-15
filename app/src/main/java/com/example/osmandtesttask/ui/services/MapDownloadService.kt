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
import com.example.osmandtesttask.common.ScopeQualifier
import com.example.osmandtesttask.common.formatFractionAsPercents
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.MapDownloader
import com.example.osmandtesttask.ui.common.extensions.toReadableFileSize
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
        val state = downloader.throttledState.value
        val notification = createNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: DownloaderState) {
        val notification = createNotification(state)
        notificationUtil.showNotification(
            NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID,
            notification
        )
    }

    private fun composeNotificationTexts(state: DownloaderState): Pair<String, String> {
        val text: String
        val title: String
        when (state) {
            is DownloaderState.Processing -> {
                val count = state.enqueuedDownloads.size + 1
                title = if (count > 1) {
                    getString(R.string.notification_map_download_many_title, count)
                } else {
                    getString(R.string.notification_map_download_title)
                }
                val name = state.activeDownload.displayName
                text = when (val downloadState = state.downloadState) {
                    is DownloadState.Progress -> {
                        val percentText = downloadState.progress.formatFractionAsPercents()
                        val total = downloadState.totalBytes.toReadableFileSize(this)
                        val downloaded = downloadState.downloadedBytes.toReadableFileSize(this)
                        getString(
                            R.string.notification_map_download_progress_text,
                            name,
                            percentText,
                            downloaded,
                            total
                        )
                    }

                    DownloadState.Cancelled -> {
                        getString(R.string.notification_map_download_cancelled_text, name)
                    }

                    is DownloadState.Error -> {
                        getString(R.string.notification_map_download_error_text, name)
                    }

                    DownloadState.Finished -> {
                        getString(R.string.notification_map_download_completed_text, name)
                    }
                }
            }

            else -> {
                title = getString(R.string.notification_map_download_title)
                text = getString(R.string.notification_map_download_waiting_text)
            }
        }
        return title to text
    }

    private fun createNotification(state: DownloaderState): Notification {
        val (title, text) = composeNotificationTexts(state)
        return createNotification(title, text)
    }

    private fun createNotification(title: String, text: String): Notification {
        return notificationUtil.createNotification(
            title, text, iconRes = R.drawable.ic_action_import, silent = true
        )
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MapDownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}