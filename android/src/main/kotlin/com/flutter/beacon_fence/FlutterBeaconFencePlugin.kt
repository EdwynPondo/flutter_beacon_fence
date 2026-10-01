package com.flutter.beacon_fence

import android.util.Log
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import com.flutter.beacon_fence.generated.FlutterBeaconFenceApi
import com.flutter.beacon_fence.services.BeaconScannerService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import org.altbeacon.beacon.BeaconManager

class FlutterBeaconFencePlugin : FlutterPlugin {
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    companion object {
        @JvmStatic
        private val TAG = "FlutterBeaconFencePlugin"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        flutterPluginBinding = binding
        val context = binding.applicationContext

        val beaconManager = BeaconManager.getInstanceForApplication(context)
        val apiImpl = BeaconFenceApiImpl(context, beaconManager)
        FlutterBeaconFenceApi.setUp(binding.binaryMessenger, apiImpl)

        BeaconScannerService(context, beaconManager, apiImpl).reactivateScanning()

        Log.d(TAG, "FlutterBeaconFenceApi setup complete.")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        flutterPluginBinding = null
    }
}
