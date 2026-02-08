package com.flutter.beacon_fence.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class Notifications {
    companion object {
        // TODO: Make notification details customizable by plugin user.
        @RequiresApi(Build.VERSION_CODES.O)
        fun createForegroundServiceNotification(context: Context): Notification {
            val channelId = "flutter_beacon_fence_plugin_channel"
            val channel = NotificationChannel(
                channelId,
                "Beacon Events",
                // This has to be at least IMPORTANCE_LOW.
                // Source: https://developer.android.com/develop/background-work/services/foreground-services#start
                NotificationManager.IMPORTANCE_LOW
            )

            @SuppressLint("DiscouragedApi") // Can't use R syntax in Flutter plugin.
            val imageId = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            return NotificationCompat.Builder(context, channelId)
                .setContentTitle("Listening for sessions")
                .setContentText("We will keep you updated")
                .setSmallIcon(imageId)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
}
