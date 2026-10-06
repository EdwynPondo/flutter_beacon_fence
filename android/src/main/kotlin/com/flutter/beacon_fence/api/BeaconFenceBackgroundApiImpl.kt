package com.flutter.beacon_fence.api

import android.content.Context
import android.util.Log
import com.flutter.beacon_fence.FlutterBeaconFenceBackgroundWorker
import com.flutter.beacon_fence.generated.AndroidNotificationsSettingsWire
import com.flutter.beacon_fence.generated.AndroidScanStrategy
import com.flutter.beacon_fence.generated.FlutterBeaconFenceBackgroundApi
import com.flutter.beacon_fence.services.ScanSettingsApplier
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

    override fun promoteToForeground(settings: AndroidNotificationsSettingsWire?) {
        val applier = ScanSettingsApplier(context, BeaconManager.getInstanceForApplication(context))
        val current = applier.persistedOrDefault()
        applier.apply(
            current.copy(
                scanStrategy = AndroidScanStrategy.FOREGROUND_SERVICE,
                notificationsSettings = settings ?: current.notificationsSettings
            )
        )
        Log.d(TAG, "Promoted scanning to the foreground service strategy.")
    }

    override fun demoteToBackground() {
        val applier = ScanSettingsApplier(context, BeaconManager.getInstanceForApplication(context))
        applier.apply(applier.persistedOrDefault().copy(scanStrategy = AndroidScanStrategy.JOB_SCHEDULER))
        Log.d(TAG, "Demoted scanning to the job scheduler strategy.")
    }
}
