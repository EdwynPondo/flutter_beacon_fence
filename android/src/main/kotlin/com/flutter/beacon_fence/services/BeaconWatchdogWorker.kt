package com.flutter.beacon_fence.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager

class BeaconWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "BeaconWatchdogWorker"
    }

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        return try {
            val beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
            val apiImpl = BeaconFenceApiImpl(applicationContext, beaconManager)
            BeaconScannerService(
                applicationContext,
                beaconManager,
                apiImpl
            ).reactivateScanning()
            repostForegroundNotification()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // reactivateScanning() cannot repost the foreground notification once scanning is
    // already active (AltBeacon's enableForegroundServiceScanning throws if any consumer
    // is bound), so a dismissed notification is repaired here directly instead, bypassing
    // BeaconManager entirely.
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun repostForegroundNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val settings = NativeBeaconPersistence.getScannerSettings(applicationContext) ?: return
            if (!settings.useForegroundService) return

            // POST_NOTIFICATIONS is only a runtime permission on API 33+; below that,
            // whether notifications are allowed is governed by the app-level notification
            // toggle instead, which checkSelfPermission can't see.
            val notificationsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            }
            if (!notificationsAllowed) return

            val notificationSettings = settings.notificationsSettings
                ?: AndroidNotificationSettingStore.DEFAULT_WIRE
            val notification = Notifications.createForegroundServiceNotification(
                applicationContext,
                notificationSettings.title,
                notificationSettings.content
            )
            NotificationManagerCompat.from(applicationContext)
                .notify(Constants.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to repost foreground notification during watchdog run: $e")
        }
    }
}