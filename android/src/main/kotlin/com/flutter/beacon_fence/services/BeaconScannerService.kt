package com.flutter.beacon_fence.services

import android.content.Context
import android.os.Build
import android.util.Log
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager

class BeaconScannerService(
    private val context: Context,
    private val beaconManager: BeaconManager,
    private val beaconFenceApi: BeaconFenceApiImpl
) {
    companion object {
        private const val TAG = "BeaconScannerService"
    }

    fun reactivateScanning() {
        Log.i(TAG, "Reactivating beacon scanning")

        // Initialize BeaconNotifier
        val beaconNotifier = BeaconNotifier(context)
        
        // Restore scanner settings if available
        val initialScannerSettings = NativeBeaconPersistence.getScannerSettings(context)

        // Configure BeaconManager once
        beaconManager.apply {
            // Support iBeacon
            beaconParsers.apply {
                if (!contains(Constants.IBEACON_PARSER)) {
                    add(Constants.IBEACON_PARSER)
                }
            }

            // Register BeaconNotifier as a monitor notifier to receive enter/exit events
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)
            // Register BeaconNotifier as a range notifier to receive RSSI events
            removeAllRangeNotifiers()
            addRangeNotifier(beaconNotifier)

            if (initialScannerSettings != null) {
                foregroundScanPeriod = initialScannerSettings.foregroundScanPeriodMillis
                foregroundBetweenScanPeriod = initialScannerSettings.foregroundBetweenScanPeriodMillis
                backgroundScanPeriod = initialScannerSettings.backgroundScanPeriodMillis
                backgroundBetweenScanPeriod = initialScannerSettings.backgroundBetweenScanPeriodMillis

                try {
                    updateScanPeriods()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to updateScanPeriods during reactivateScanning: $e")
                }

                if (!isAnyConsumerBound &&
                    initialScannerSettings.useForegroundService &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ) {
                    try {
                        val notificationSettings = initialScannerSettings.notificationsSettings
                            ?: AndroidNotificationSettingStore.DEFAULT_WIRE
                        val notification = Notifications.createForegroundServiceNotification(
                            context,
                            notificationSettings.title,
                            notificationSettings.content
                        )
                        enableForegroundServiceScanning(notification, Constants.NOTIFICATION_ID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to enableForegroundServiceScanning during reactivateScanning: $e")
                    }
                }
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }

        // Resume monitoring for previously saved beacons
        beaconFenceApi.restorePersistedBeacons()
    }
}
