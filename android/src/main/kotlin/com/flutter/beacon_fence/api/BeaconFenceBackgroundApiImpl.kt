package com.flutter.beacon_fence.api

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.flutter.beacon_fence.FlutterBeaconFenceBackgroundWorker
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import com.flutter.beacon_fence.generated.FlutterBeaconFenceBackgroundApi
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager

class BeaconFenceBackgroundApiImpl(
    private val context: Context,
    private val worker: FlutterBeaconFenceBackgroundWorker
) : FlutterBeaconFenceBackgroundApi {
    companion object {
        @JvmStatic
        private val TAG = "BeaconFenceBackgroundApiImpl"
    }

    override fun triggerApiInitialized() {
        worker.triggerApiReady()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun promoteToForeground() {
        val beaconManager = BeaconManager.getInstanceForApplication(context)
        val settings = androidScannerSettingsWire(beaconManager, true)
        val notification = Notifications.createForegroundServiceNotification(context)
        beaconManager.enableForegroundServiceScanning(notification, 938131)
        NativeBeaconPersistence.saveScannerSettings(context, settings)
        Log.d(TAG, "Promoted background service to foreground service.")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun demoteToBackground() {
        val beaconManager = BeaconManager.getInstanceForApplication(context)
        val settings = androidScannerSettingsWire(beaconManager, false)
        BeaconManager.getInstanceForApplication(context).disableForegroundServiceScanning()
        NativeBeaconPersistence.saveScannerSettings(context, settings)
        Log.d(TAG, "Demoted foreground service back to background service.")
    }

    private fun androidScannerSettingsWire(
        beaconManager: BeaconManager,
        useForeground: Boolean
    ): AndroidScannerSettingsWire {
        NativeBeaconPersistence.getScannerSettings(context)
        return NativeBeaconPersistence.getScannerSettings(context) ?: AndroidScannerSettingsWire(
            beaconManager.foregroundScanPeriod,
            beaconManager.foregroundBetweenScanPeriod,
            beaconManager.backgroundScanPeriod,
            beaconManager.backgroundBetweenScanPeriod,
            useForeground,
        )
    }
}
