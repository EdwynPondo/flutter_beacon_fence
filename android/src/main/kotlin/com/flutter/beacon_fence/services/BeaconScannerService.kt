package com.flutter.beacon_fence.services

import android.content.Context
import android.util.Log
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.util.BeaconNotifier
import com.flutter.beacon_fence.util.NativeBeaconPersistence
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

        // Configure BeaconManager once
        beaconManager.apply {
            // Only parse iBeacon. Every extra parser widens the hardware scan filters,
            // waking the CPU for advertisements no region can match.
            beaconParsers.apply {
                if (size != 1 || !contains(Constants.IBEACON_PARSER)) {
                    clear()
                    add(Constants.IBEACON_PARSER)
                }
            }

            // Register BeaconNotifier as a monitor notifier to receive enter/exit events
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)
            // Register BeaconNotifier as a range notifier to receive RSSI events
            removeAllRangeNotifiers()
            addRangeNotifier(beaconNotifier)
        }

        // Restore scanner settings, falling back to the defaults when beacons exist but no
        // settings were ever configured.
        val hasBeacons = NativeBeaconPersistence.getAllBeaconIds(context).isNotEmpty()
        if (hasBeacons || NativeBeaconPersistence.getScannerSettings(context) != null) {
            val applier = ScanSettingsApplier(context, beaconManager)
            try {
                applier.apply(applier.persistedOrDefault())
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to apply scanner settings during reactivateScanning: $e")
            }
        }

        // Resume monitoring for previously saved beacons
        beaconFenceApi.restorePersistedBeacons()
    }
}
