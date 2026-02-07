package com.flutter.beacon_fence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

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
                clear()
                add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
            }
            // Register BeaconNotifier
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)
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
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }
        
        BeaconFenceApiImpl(context, beaconManager).reCreateAfterReboot()
    }
}
