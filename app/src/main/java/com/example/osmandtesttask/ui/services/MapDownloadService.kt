package com.example.osmandtesttask.ui.services

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.osmandtesttask.R
import com.example.osmandtesttask.common.Logs
import com.example.osmandtesttask.common.formatAsPercentage1
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.IMapDownloader
import com.example.osmandtesttask.ui.common.notifications.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicLong

class MapDownloadService : Service() {
    private val downloader: IMapDownloader by inject()
    private val notificationUtil: NotificationUtil by inject()
    private val serviceScope by inject<CoroutineScope>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        doStartForeground()
        serviceScope.launch {
            val lastStateUpdateTime = AtomicLong(0L)
            downloader.state
                .conflate()
                .filter { state ->
                    if (state == DownloaderState.Empty) {
                        true
                    } else {
                        val time = SystemClock.elapsedRealtime()
                        val accept = time - lastStateUpdateTime.get() >= NOTIFICATION_UPDATE_DELAY
                        if (accept) {
                            lastStateUpdateTime.set(time)
                        }
                        accept
                    }
                }
                .collect { state ->
                Logs.d("service received state: ${state::class.simpleName}")
                updateNotification(state)
                if (state is DownloaderState.Empty) stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        downloader.cancelAll()
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
        notificationUtil.showNotification(NotificationUtil.MAP_DOWNLOAD_NOTIFICATION_ID, notification)
    }

    private fun composeNotificationText(state: DownloaderState): String {
        return when(state) {
            is DownloaderState.Processing -> {
                when(val downloadState = state.downloadState) {
                    is DownloadState.Progress -> {
                        val percentText = (downloadState.bytesRead.toFloat() / downloadState.totalBytes).formatAsPercentage1()
                        val name = state.activeDownload.displayName
                        val count = state.enqueuedDownloads.size + 1 // include current in count
                        getString(R.string.notification_map_download_progress_text, count, name, percentText)

                    }
                    else -> "" // todo
                }
            }
            else -> "" // todo
        }
    }

    private fun createNotification(text: String = ""): Notification {
        val title = getString(R.string.app_name)
        return notificationUtil.createNotification(title, text, iconRes = R.drawable.ic_action_import, silent = true)
    }

    companion object {
        private const val NOTIFICATION_UPDATE_DELAY = 500L
        fun start(context: Context) {
            Logs.d("requesting download service start")
            val intent = Intent(context, MapDownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}