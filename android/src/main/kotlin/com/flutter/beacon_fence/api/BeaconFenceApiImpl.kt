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
import com.flutter.beacon_fence.util.ActiveBeaconWires
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.InsideRegionGuard
import com.flutter.beacon_fence.util.toFlutterError
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.Identifier
import androidx.core.content.edit
import com.flutter.beacon_fence.generated.BeaconFenceErrorCode
import com.flutter.beacon_fence.generated.FlutterBeaconFenceApi
import com.flutter.beacon_fence.generated.FlutterError
import com.flutter.beacon_fence.services.ScanSettingsApplier
import com.flutter.beacon_fence.services.BeaconWatchdogService

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

    fun restorePersistedBeacons() {
        Log.d(TAG, "restoreAfterReboot: Fetching persisted beacons...")
        val beacons = NativeBeaconPersistence.getAllBeacons(context)
        val monitoredIds = beaconManager.monitoredRegions.map { it.uniqueId }.toSet()
        val missing = beacons.filter { it.id !in monitoredIds }
        Log.d(TAG, "restoreAfterReboot: Found ${beacons.size} beacons, re-creating ${missing.size} not monitored.")
        for (beacon in missing) {
            createBeaconHelper(beacon, false, null)
        }

        Log.d(TAG, "restoreAfterReboot: ${missing.size} beacons processing complete.")
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
                callback.invoke(Result.failure(BeaconFenceErrorCode.BEACON_NOT_FOUND.toFlutterError("Beacon not found")))
                return
            }

            val region = convertBeaconWire(beaconToRemove)
            beaconManager.stopMonitoring(region)
            beaconManager.stopRangingBeacons(region)
            
            NativeBeaconPersistence.removeBeacon(context, id)
            InsideRegionGuard.markOutside(context, id)
            BeaconWatchdogService(context).updateWatchdogState()
            Log.d(TAG, "Removed Beacon ID=$id.")
            callback.invoke(Result.success(Unit))
        } catch (e: Throwable) {
            Log.e(TAG, "removeBeaconById: Failed to remove Beacon ID=$id: $e")
            callback.invoke(Result.failure(e.asFlutterError()))
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
            InsideRegionGuard.clear(context)
            BeaconWatchdogService(context).updateWatchdogState()
            Log.d(TAG, "Removed all beacons.")
            callback.invoke(Result.success(Unit))
        } catch (e: Throwable) {
            Log.e(TAG, "removeAllBeacons: Failed to remove beacons: $e")
            callback.invoke(Result.failure(e.asFlutterError()))
        }
    }

    override fun configureAndroidMonitor(
        settings: AndroidScannerSettingsWire,
        callback: (Result<Unit>) -> Unit
    ) {
        try {
            ScanSettingsApplier(context, beaconManager).apply(settings)
            callback.invoke(Result.success(Unit))
        } catch (e: Throwable) {
            Log.e(TAG, "configureAndroidMonitor: Failed to configure monitor: $e")
            callback.invoke(Result.failure(e.asFlutterError()))
        }
    }

    private fun createBeaconHelper(
        beacon: BeaconWire,
        cache: Boolean,
        callback: ((Result<Unit>) -> Unit)?
    ) {
        try {
            Log.d(TAG, "createBeaconHelper: Starting monitoring for Beacon ID=${beacon.id}")
            // Check Bluetooth permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // For simplified implementation, we'll just check Manifest.permission.BLUETOOTH on older versions
                // and BLUETOOTH_SCAN on 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                     if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "createBeaconHelper: Missing BLUETOOTH_SCAN permission for Beacon ID=${beacon.id}")
                        callback?.invoke(Result.failure(BeaconFenceErrorCode.MISSING_BLUETOOTH_PERMISSION.toFlutterError("Missing BLUETOOTH_SCAN permission")))
                        return
                     }
                }
            }

            if (cache) {
                // Apply the default scan strategy when the app never configured one.
                ScanSettingsApplier(context, beaconManager).applyDefaultIfUnset()
            }

            val region = convertBeaconWire(beacon)
            beaconManager.startMonitoring(region)
            // Remove startRangingBeacons here to match iOS behavior:
            // Ranging starts only when we actually enter the region.
            
            if (cache) {
                NativeBeaconPersistence.saveBeacon(context, beacon)
            }
            BeaconWatchdogService(context).updateWatchdogState()
            
            Log.d(TAG, "createBeaconHelper: Successfully started monitoring Beacon ID=${beacon.id}.")
            callback?.invoke(Result.success(Unit))
        } catch (e: Throwable) {
            Log.e(TAG, "createBeaconHelper: Failed to start monitoring Beacon ID=${beacon.id}: $e")
            callback?.invoke(Result.failure(e.asFlutterError()))
        }
    }

    private fun convertBeaconWire(beacon: BeaconWire): Region {
        // AltBeacon monitoring
        // We need a Region object
        // uuid major minor are nullable in BeaconWire?
        // In iBeacon: Region(id, uuid, major, minor)
        // If major is null, it's a wildcard.
        val uuid = try {
            Identifier.parse(beacon.uuid)
        } catch (e: IllegalArgumentException) {
            throw BeaconFenceErrorCode.INVALID_ARGUMENTS.toFlutterError("Invalid UUID format: ${beacon.uuid}")
        }
        val major = beacon.major?.let { parseBeaconInt(it, "major") }
        val minor = beacon.minor?.let { parseBeaconInt(it, "minor") }
        return Region(beacon.id, uuid, major, minor)
    }

    private fun parseBeaconInt(value: Long, name: String): Identifier {
        return try {
            Identifier.fromInt(value.toInt())
        } catch (e: IllegalArgumentException) {
            throw BeaconFenceErrorCode.INVALID_ARGUMENTS.toFlutterError("Invalid $name value: $value")
        }
    }

    private fun Throwable.asFlutterError(): FlutterError =
        this as? FlutterError ?: BeaconFenceErrorCode.PLUGIN_INTERNAL.toFlutterError(toString())
}
