package com.flutter.beacon_fence.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.generated.AndroidScanStrategy
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import com.flutter.beacon_fence.generated.BeaconFenceErrorCode
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage
import com.flutter.beacon_fence.model.AndroidScannerSettingsStorage.AndroidNotificationSettingStore
import com.flutter.beacon_fence.util.InsideRegionGuard
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import com.flutter.beacon_fence.util.Notifications
import com.flutter.beacon_fence.util.toFlutterError
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Settings

/**
 * Single place that applies [AndroidScannerSettingsWire] to AltBeacon.
 *
 * Uses AltBeacon's Settings API, which unbinds and rebinds consumers by itself when the scan
 * strategy changes, so strategies can be switched while beacons are being monitored.
 */
class ScanSettingsApplier(
    private val context: Context,
    private val beaconManager: BeaconManager
) {
    companion object {
        private const val TAG = "ScanSettingsApplier"
    }

    /** The persisted settings, or the plugin defaults when none were configured yet. */
    fun persistedOrDefault(): AndroidScannerSettingsWire =
        NativeBeaconPersistence.getScannerSettings(context) ?: AndroidScannerSettingsStorage.DEFAULT_WIRE

    /** Applies the default settings if none were persisted yet. */
    fun applyDefaultIfUnset() {
        if (NativeBeaconPersistence.getScannerSettings(context) == null) {
            Log.d(TAG, "No scanner settings persisted, applying defaults.")
            apply(AndroidScannerSettingsStorage.DEFAULT_WIRE)
        }
    }

    /**
     * Applies and persists [settings].
     *
     * Throws a FlutterError with [BeaconFenceErrorCode.MISSING_FOREGROUND_SERVICE_PERMISSION] or
     * [BeaconFenceErrorCode.MISSING_LOCATION_PERMISSION] when the foreground service strategy is
     * requested but cannot be started.
     */
    fun apply(settings: AndroidScannerSettingsWire) {
        if (settings.scanStrategy == AndroidScanStrategy.FOREGROUND_SERVICE) {
            checkForegroundServicePermissions()
        }

        val strategy = altBeaconStrategy(settings)
        val strategyChanged = beaconManager.activeSettings.scanStrategy.javaClass != strategy.javaClass
        if (strategyChanged) {
            // AltBeacon rebinds on a strategy change, which may replay region entries.
            InsideRegionGuard.markRebind(context)
        }

        beaconManager.adjustSettings(
            Settings(
                scanPeriods = Settings.ScanPeriods(
                    settings.foregroundScanPeriodMillis,
                    settings.foregroundBetweenScanPeriodMillis,
                    settings.backgroundScanPeriodMillis,
                    settings.backgroundBetweenScanPeriodMillis
                ),
                regionExitPeriodMillis = settings.regionExitPeriodMillis.toInt(),
                scanStrategy = strategy,
                longScanForcingEnabled = settings.scanStrategy == AndroidScanStrategy.INTENT
            )
        )

        NativeBeaconPersistence.saveScannerSettings(context, settings)
        BeaconWatchdogService(context).updateWatchdogState()

        Log.d(TAG, "Applied scanner settings: " +
                "Strategy=${settings.scanStrategy}, " +
                "Foreground(Scan=${settings.foregroundScanPeriodMillis}ms, Between=${settings.foregroundBetweenScanPeriodMillis}ms), " +
                "Background(Scan=${settings.backgroundScanPeriodMillis}ms, Between=${settings.backgroundBetweenScanPeriodMillis}ms), " +
                "RegionExitPeriod=${settings.regionExitPeriodMillis}ms")
    }

    private fun altBeaconStrategy(settings: AndroidScannerSettingsWire): Settings.ScanStrategy {
        // Foreground service notifications and intent scanning need Android 8+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Settings.JobServiceScanStrategy()
        }
        return when (settings.scanStrategy) {
            AndroidScanStrategy.FOREGROUND_SERVICE -> {
                val notificationSettings = settings.notificationsSettings
                    ?: AndroidNotificationSettingStore.DEFAULT_WIRE
                val notification = Notifications.createForegroundServiceNotification(
                    context,
                    notificationSettings.title,
                    notificationSettings.content
                )
                Settings.ForegroundServiceScanStrategy(notification, Constants.NOTIFICATION_ID)
            }
            AndroidScanStrategy.JOB_SCHEDULER -> Settings.JobServiceScanStrategy()
            AndroidScanStrategy.INTENT -> Settings.IntentScanStrategy()
        }
    }

    private fun checkForegroundServicePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            !isPermissionDeclared(Manifest.permission.FOREGROUND_SERVICE)
        ) {
            throw BeaconFenceErrorCode.MISSING_FOREGROUND_SERVICE_PERMISSION.toFlutterError(
                "The foreground service scan strategy requires android.permission.FOREGROUND_SERVICE in AndroidManifest.xml"
            )
        }
        // AltBeacon's service has foregroundServiceType="location", which Android 14+ only
        // allows to start while a location permission is granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !isGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            throw BeaconFenceErrorCode.MISSING_LOCATION_PERMISSION.toFlutterError(
                "The foreground service scan strategy requires a granted location permission on Android 14+"
            )
        }
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun isPermissionDeclared(permission: String): Boolean {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        return packageInfo.requestedPermissions?.contains(permission) == true
    }
}
