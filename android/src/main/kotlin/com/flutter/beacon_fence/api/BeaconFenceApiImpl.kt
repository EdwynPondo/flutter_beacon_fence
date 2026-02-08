package com.flutter.beacon_fence.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.generated.ActiveBeaconWire
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import com.flutter.beacon_fence.generated.BeaconWire
import com.flutter.beacon_fence.generated.FlutterError
import com.flutter.beacon_fence.util.ActiveBeaconWires
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.Identifier
import androidx.core.content.edit
import com.flutter.beacon_fence.generated.BeaconFenceErrorCode
import com.flutter.beacon_fence.generated.FlutterBeaconFenceApi

class BeaconFenceApiImpl(
    private val context: Context,
    private val beaconManager: BeaconManager
) : FlutterBeaconFenceApi {
    companion object {
        @JvmStatic
        private val TAG = "BeaconFenceApiImpl"
    }

    override fun initialize(callbackDispatcherHandle: Long) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .edit {
                putLong(Constants.BEACON_CALLBACK_DISPATCHER_HANDLE_KEY, callbackDispatcherHandle)
            }
        
        Log.d(TAG, "Initialized consolidated BeaconFenceApi.")
    }

    override fun reCreateAfterReboot() {
        val beacons = NativeBeaconPersistence.getAllBeacons(context)
        for (beacon in beacons) {
            createBeaconHelper(beacon, false, null)
        }

        Log.d(TAG, "${beacons.size} beacons re-created.")
    }

    override fun createBeacon(
        beacon: BeaconWire,
        callback: (Result<Unit>) -> Unit
    ) {
        createBeaconHelper(beacon, true, callback)
    }

    override fun getBeaconIds(): List<String> {
        return NativeBeaconPersistence.getAllBeaconIds(context)
    }

    override fun getBeacons(): List<ActiveBeaconWire> {
        val beacons = NativeBeaconPersistence.getAllBeacons(context)
        return beacons.map { ActiveBeaconWires.fromBeaconWire(it) }.toList()
    }

    override fun removeBeaconById(id: String, callback: (Result<Unit>) -> Unit) {
        try {
            // Find the region to stop monitoring. AltBeacon uses Region objects.
            val allBeacons = NativeBeaconPersistence.getAllBeacons(context)
            val beaconToRemove = allBeacons.find { it.id == id }
            
            if (beaconToRemove == null) {
                callback.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.BEACON_NOT_FOUND.raw.toString(), "Beacon not found")))
                return
            }

            val region = convertBeaconWire(beaconToRemove)
            beaconManager.stopMonitoring(region)
            beaconManager.stopRangingBeacons(region)
            
            NativeBeaconPersistence.removeBeacon(context, id)
            Log.d(TAG, "Removed Beacon ID=$id.")
            callback.invoke(Result.success(Unit))
        } catch (e: Exception) {
            callback.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    override fun removeAllBeacons(callback: (Result<Unit>) -> Unit) {
        try {
            val beacons = NativeBeaconPersistence.getAllBeacons(context)
            for (beacon in beacons) {
                val region = convertBeaconWire(beacon)
                beaconManager.stopMonitoring(region)
                beaconManager.stopRangingBeacons(region)
            }
            NativeBeaconPersistence.removeAllBeacons(context)
            Log.d(TAG, "Removed all beacons.")
            callback.invoke(Result.success(Unit))
        } catch (e: Exception) {
            callback.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    override fun configureAndroidMonitor(
        settings: AndroidScannerSettingsWire,
        callback: (Result<Unit>) -> Unit
    ) {
        try {
            beaconManager.foregroundScanPeriod = settings.foregroundScanPeriodMillis
            beaconManager.foregroundBetweenScanPeriod = settings.foregroundBetweenScanPeriodMillis
            beaconManager.backgroundScanPeriod = settings.backgroundScanPeriodMillis
            beaconManager.backgroundBetweenScanPeriod = settings.backgroundBetweenScanPeriodMillis
            
            try {
                beaconManager.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Failed into updateScanPeriods: $e")
                // usage of updateScanPeriods sometimes throws if not bound, 
                // but setting the fields should be enough for next scan cycle.
            }
            
            
            if (settings.useForegroundService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notification = Notifications.createForegroundServiceNotification(context)
                // Using a unique ID for AltBeacon's foreground service
                beaconManager.enableForegroundServiceScanning(notification, 938131)
            } else {
                beaconManager.disableForegroundServiceScanning()
            }
            
            Log.d(TAG, "Configured Android scan periods: " +
                    "Foreground(Scan=${settings.foregroundScanPeriodMillis}ms, Between=${settings.foregroundBetweenScanPeriodMillis}ms), " +
                    "Background(Scan=${settings.backgroundScanPeriodMillis}ms, Between=${settings.backgroundBetweenScanPeriodMillis}ms), " +
                    "UseForegroundService=${settings.useForegroundService}")
            
            NativeBeaconPersistence.saveScannerSettings(context, settings)
            
            callback.invoke(Result.success(Unit))
        } catch (e: Exception) {
            callback.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    private fun createBeaconHelper(
        beacon: BeaconWire,
        cache: Boolean,
        callback: ((Result<Unit>) -> Unit)?
    ) {
        try {
            // Check Bluetooth permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // For simplified implementation, we'll just check Manifest.permission.BLUETOOTH on older versions
                // and BLUETOOTH_SCAN on 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                     if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                         callback?.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.MISSING_BLUETOOTH_PERMISSION.raw.toString(), "Missing BLUETOOTH_SCAN permission")))
                         return
                     }
                }
            }

            val region = convertBeaconWire(beacon)
            beaconManager.startMonitoring(region)
            // Remove startRangingBeacons here to match iOS behavior:
            // Ranging starts only when we actually enter the region.
            
            if (cache) {
                NativeBeaconPersistence.saveBeacon(context, beacon)
            }
            
            Log.d(TAG, "Successfully started monitoring Beacon ID=${beacon.id}.")
            callback?.invoke(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start monitoring Beacon ID=${beacon.id}: $e")
            callback?.invoke(Result.failure(FlutterError(BeaconFenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    private fun convertBeaconWire(beacon: BeaconWire): Region {
        // AltBeacon monitoring
        // We need a Region object
        // uuid major minor are nullable in BeaconWire?
        // In iBeacon: Region(id, uuid, major, minor)
        // If major is null, it's a wildcard.
        return Region(
            beacon.id,
            Identifier.parse(beacon.uuid),
            beacon.major?.let { Identifier.fromInt(it.toInt()) },
            beacon.minor?.let { Identifier.fromInt(it.toInt()) }
        )
    }
}
