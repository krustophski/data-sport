package com.krustophski.data.sport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class BootForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = createNotificationChannel(
            getString(R.string.notification_id_boot_service),
            getString(R.string.notification_channel_name_boot_service)
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_boot_service_title))
            .setContentText(getString(R.string.notification_boot_service_message))
            .setSmallIcon(R.drawable.ic_cyclotrack_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .build().also { it.flags = it.flags or Notification.FLAG_ONGOING_EVENT }

        startForeground(10001, notification)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_LAUNCH_TO_BACKGROUND, true)
        }
        startActivity(launchIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 5000)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        return channelId
    }
}
