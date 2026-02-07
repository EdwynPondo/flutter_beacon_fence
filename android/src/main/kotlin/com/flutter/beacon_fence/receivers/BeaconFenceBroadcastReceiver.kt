package com.flutter.beacon_fence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.FlutterBeaconFenceBackgroundWorker
import com.flutter.beacon_fence.generated.ActiveBeaconWire
import com.flutter.beacon_fence.generated.BeaconCallbackParamsWire
import com.flutter.beacon_fence.generated.BeaconEvent
import com.flutter.beacon_fence.model.BeaconCallbackParamsStorage
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import kotlinx.serialization.json.Json
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region

class BeaconFenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BeaconFenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 2. Check if it's a Beacon event
        // Using string literals for keys to avoid issues with missing Constants
        // "state" and "org.altbeacon.beacon.Region" are the standard extras for AltBeacon Monitoring
        if (intent.hasExtra("state") && intent.hasExtra("org.altbeacon.beacon.Region")) {
            handleBeaconEvent(context, intent)
            return
        }

        Log.w(TAG, "Broadcast received but beacon data found. Extras: ${intent.extras?.keySet()}")
    }

    private fun handleBeaconEvent(context: Context, intent: Intent) {
        val params = getBeaconCallbackParams(context, intent) ?: return
        val jsonData = Json.encodeToString(BeaconCallbackParamsStorage.fromWire(params))

        val workRequest = OneTimeWorkRequestBuilder<FlutterBeaconFenceBackgroundWorker>()
            .setInputData(Data.Builder().putString(Constants.WORKER_PAYLOAD_KEY, jsonData).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).beginUniqueWork(
            Constants.BEACON_CALLBACK_WORK_GROUP,
            ExistingWorkPolicy.APPEND,
            workRequest
        ).enqueue()
    }

    private fun getBeaconCallbackParams(context: Context, intent: Intent): BeaconCallbackParamsWire? {
        val state = intent.getIntExtra("state", -1)
        val region = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("org.altbeacon.beacon.Region", Region::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("org.altbeacon.beacon.Region") as? Region
        }
        
        if (region == null || state == -1) {
            Log.e(TAG, "Beacon event region or state missing. Region: $region, State: $state")
            return null
        }

        val event = when (state) {
            MonitorNotifier.INSIDE -> BeaconEvent.ENTER
            MonitorNotifier.OUTSIDE -> BeaconEvent.EXIT
            else -> {
                Log.w(TAG, "Unknown beacon state: $state")
                return null
            }
        }

        val rssi = intent.getIntExtra("rssi", 0).takeIf { it != 0 }?.toLong()

        val beaconWire = NativeBeaconPersistence.getAllBeacons(context).find { it.id == region.uniqueId } ?: return null
        if (!beaconWire.triggers.contains(event)) return null

        return BeaconCallbackParamsWire(
            listOf(ActiveBeaconWire(
                beaconWire.id, beaconWire.uuid, beaconWire.major, beaconWire.minor,
                rssi,
                beaconWire.triggers, beaconWire.androidSettings
            )),
            event,
            beaconWire.callbackHandle
        )
    }
}
