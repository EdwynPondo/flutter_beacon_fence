package com.flutter.beacon_fence.api

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.FlutterBeaconFenceForegroundService
import com.flutter.beacon_fence.FlutterBeaconFenceBackgroundWorker
import com.flutter.beacon_fence.generated.FlutterBeaconFenceBackgroundApi

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
        context.startForegroundService(Intent(context, FlutterBeaconFenceForegroundService::class.java))
        Log.d(TAG, "Promoted background service to foreground service.")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun demoteToBackground() {
        val intent = Intent(context, FlutterBeaconFenceForegroundService::class.java)
        intent.action = Constants.ACTION_SHUTDOWN
        context.startForegroundService(intent)
        Log.d(TAG, "Demoted foreground service back to background service.")
    }
}
