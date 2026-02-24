package com.flutter.beacon_fence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager

class BeaconFenceRebootBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "BeaconFenceRebootBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Boot completed broadcast received. Re-creating beacons!")
        
        // Initialize BeaconNotifier
        val beaconNotifier = BeaconNotifier(context)
        
        // Restore scanner settings if available
        val initialScannerSettings = NativeBeaconPersistence.getScannerSettings(context)
        
        // Configure BeaconManager
        val beaconManager = BeaconManager.getInstanceForApplication(context).apply {
            // Support iBeacon
            beaconParsers.apply {
                if (contains(Constants.IBEACON_PARSER)) return@apply
                add(Constants.IBEACON_PARSER)
            }
            // Register BeaconNotifier
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)

            if (initialScannerSettings != null) {
                foregroundScanPeriod = initialScannerSettings.foregroundScanPeriodMillis
                foregroundBetweenScanPeriod = initialScannerSettings.foregroundBetweenScanPeriodMillis
                backgroundScanPeriod = initialScannerSettings.backgroundScanPeriodMillis
                backgroundBetweenScanPeriod = initialScannerSettings.backgroundBetweenScanPeriodMillis
                try {
                    updateScanPeriods()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed into updateScanPeriods during init: $e")
                }

                try {
                    if (!isAnyConsumerBound &&
                        initialScannerSettings.useForegroundService &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val notificationSettings = initialScannerSettings.notificationsSettings
                            ?: AndroidNotificationSettingStore.DEFAULT_WIRE
                        val notification = Notifications.createForegroundServiceNotification(
                            context,
                            notificationSettings.title,
                            notificationSettings.content
                        )
                        enableForegroundServiceScanning(notification, Constants.NOTIFICATION_ID)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed into updateScanPeriods during init: $e")
                }
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }
        
        BeaconFenceApiImpl(context, beaconManager).reCreateAfterReboot()
    }
}
