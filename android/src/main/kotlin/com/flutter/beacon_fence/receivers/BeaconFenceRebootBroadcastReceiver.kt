package com.flutter.beacon_fence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.services.BeaconScannerService
import org.altbeacon.beacon.BeaconManager

class BeaconFenceRebootBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BeaconFenceRebootBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Broadcast received with action: $action. Reactivating beacon scanning...")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val beaconManager = BeaconManager.getInstanceForApplication(context)
            val apiImpl = BeaconFenceApiImpl(context, beaconManager)
            BeaconScannerService(context, beaconManager, apiImpl).reactivateScanning()
        }
    }
}
