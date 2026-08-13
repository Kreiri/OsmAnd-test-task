package com.example.osmandtesttask.ui.common.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.osmandtesttask.R
import com.example.osmandtesttask.ui.common.permissions.isnNotificationPermissionGranted
import com.example.osmandtesttask.ui.screens.main.MainActivity

class NotificationUtil(private val context: Context) {
    private val defaultChannelId = "default_channel_id"
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val MAP_DOWNLOAD_NOTIFICATION_ID = 144
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun setupDefaultChannel() {
        if (notificationManager.getNotificationChannel(defaultChannelId) == null) {
            val name = context.getString(R.string.notifications_channel_default)
            val channel = NotificationChannelCompat.Builder(
                defaultChannelId,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(name)
                .build()
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        title: String,
        message: String,
        iconRes: Int? = null,
        intent: Intent? = null,
        ongoing: Boolean = false,
        autoCancel: Boolean = true,
        silent: Boolean = false
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupDefaultChannel()
        }
        val innerIntent = intent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            innerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, defaultChannelId)
            .setContentTitle(title)
            .setContentText(message)
            .apply {
                if (iconRes != null) {
                    setSmallIcon(iconRes)
                }
            }
            .setOngoing(ongoing)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setSilent(silent)
            .setContentIntent(pendingIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun showNotification(id: Int, notification: Notification) {
        if (context.isnNotificationPermissionGranted()) {
            notificationManager.notify(id, notification)
        }
    }
}