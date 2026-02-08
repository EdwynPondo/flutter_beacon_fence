package com.flutter.beacon_fence

import android.content.Context
import android.os.Build
import android.util.Log
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.generated.FlutterBeaconFenceApi
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import io.flutter.embedding.engine.plugins.FlutterPlugin
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

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
                clear()
                add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
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
                    if (initialScannerSettings.useForegroundService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val notification = Notifications.createForegroundServiceNotification(context)
                        enableForegroundServiceScanning(notification, 938131)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed into updateScanPeriods during init: $e")
                }
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }
        val apiImpl = BeaconFenceApiImpl(context, beaconManager)
        FlutterBeaconFenceApi.setUp(binding.binaryMessenger, apiImpl)
        Log.d(TAG, "FlutterBeaconFenceApi setup complete.")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
    }
}
