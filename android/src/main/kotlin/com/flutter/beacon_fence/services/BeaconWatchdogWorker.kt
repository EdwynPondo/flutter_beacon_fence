package com.flutter.beacon_fence.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
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
import com.flutter.beacon_fence.generated.AndroidScanStrategy
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.service.BeaconService

/**
 * Periodically revives the foreground service scan strategy in case the OEM killed the service
 * or the user dismissed its notification while beacons are still monitored.
 */
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
            val settings = NativeBeaconPersistence.getScannerSettings(applicationContext)
            if (settings?.scanStrategy != AndroidScanStrategy.FOREGROUND_SERVICE) {
                return Result.success()
            }

            val beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
            val apiImpl = BeaconFenceApiImpl(applicationContext, beaconManager)
            // Re-applies the settings and only re-arms beacons that are no longer monitored.
            BeaconScannerService(
                applicationContext,
                beaconManager,
                apiImpl
            ).reactivateScanning()

            if (isForegroundServiceRunning()) {
                repostForegroundNotificationIfMissing(settings)
            } else {
                restartForegroundService(beaconManager)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Watchdog run failed: $e")
            Result.retry()
        }
    }

    private fun isForegroundServiceRunning(): Boolean {
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // Deprecated for third party services, but still returns this app's own services.
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == BeaconService::class.java.name && it.foreground
        }
    }

    // Android 12+ usually blocks starting a foreground service from the background, so this
    // can fail. The service then comes back at the next reboot or app launch.
    private fun restartForegroundService(beaconManager: BeaconManager) {
        try {
            if (beaconManager.foregroundServiceStartFailed()) {
                Log.w(TAG, "Foreground service start failed previously, retrying.")
                beaconManager.retryForegroundServiceScanning()
            } else {
                Log.w(TAG, "Foreground service is not running.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restart the foreground service, will try again next run: $e")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun repostForegroundNotificationIfMissing(settings: AndroidScannerSettingsWire) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.activeNotifications.any { it.id == Constants.NOTIFICATION_ID }) {
                return
            }

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
            Log.d(TAG, "Reposted the dismissed foreground service notification.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to repost foreground notification during watchdog run: $e")
        }
    }
}
