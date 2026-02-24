package com.flutter.beacon_fence

import android.content.Context
import android.os.Build
import android.util.Log
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.generated.FlutterBeaconFenceApi
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import io.flutter.embedding.engine.plugins.FlutterPlugin
import org.altbeacon.beacon.BeaconManager

class FlutterBeaconFencePlugin : FlutterPlugin {
    private var context: Context? = null

    companion object {
        @JvmStatic
        private val TAG = "FlutterBeaconFencePlugin"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val context = binding.applicationContext

        // Initialize BeaconNotifier
        val beaconNotifier = BeaconNotifier(context)
        
        // Restore scanner settings if available
        val initialScannerSettings = NativeBeaconPersistence.getScannerSettings(context)
        
        // Configure BeaconManager once
        val beaconManager = BeaconManager.getInstanceForApplication(context).apply {
            // Support iBeacon
            beaconParsers.apply {
                if (contains(Constants.IBEACON_PARSER)) return@apply
                add(Constants.IBEACON_PARSER)
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
                    Log.e(TAG, "Failed into updateScanPeriods during init: $e")
                }

                if (!isAnyConsumerBound &&
                    initialScannerSettings.useForegroundService &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                        Log.e(TAG, "Failed into enableForegroundServiceScanning during init: $e")
                    }
                }
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }
        val apiImpl = BeaconFenceApiImpl(context, beaconManager)
        FlutterBeaconFenceApi.setUp(binding.binaryMessenger, apiImpl)
        
        // Resume monitoring for previously saved beacons
        apiImpl.reCreateAfterReboot()
        
        Log.d(TAG, "FlutterBeaconFenceApi setup complete.")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
    }
}
