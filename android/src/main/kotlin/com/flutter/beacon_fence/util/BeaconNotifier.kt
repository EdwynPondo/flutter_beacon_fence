package com.flutter.beacon_fence.util

import android.content.Context
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
import kotlinx.serialization.json.Json
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region

class BeaconNotifier(private val context: Context): MonitorNotifier, RangeNotifier  {
    companion object {
        @JvmStatic
        private val TAG = "BeaconNotifier"
    }

    private val beaconManager = BeaconManager.getInstanceForApplication(context)
    // --- MonitorNotifier Implementation ---

    override fun didEnterRegion(region: Region) {
        Log.d(TAG, "didEnterRegion: ${region.uniqueId}")
        // Match iOS: Start ranging to get RSSI, do not broadcast Enter yet.
        beaconManager.startRangingBeacons(region)
    }

    override fun didExitRegion(region: Region) {
        Log.d(TAG, "didExitRegion: ${region.uniqueId}")
        // Match iOS: Stop ranging and broadcast Exit.
        beaconManager.stopRangingBeacons(region)
        triggerBeaconBroadcast(region, MonitorNotifier.OUTSIDE)
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        Log.d(TAG, "didDetermineStateForRegion: $state for ${region.uniqueId}")
    }

    // --- RangeNotifier Implementation ---

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        if (beacons != null && region != null && beacons.isNotEmpty()) {
            val beacon = beacons.first()
            Log.d(TAG, "didRangeBeaconsInRegion: ${region.uniqueId}, rssi=${beacon.rssi}")
            // Match iOS: Send Enter event with RSSI, then stop ranging immediately.
            triggerBeaconBroadcast(region, MonitorNotifier.INSIDE, beacon.rssi)
            beaconManager.stopRangingBeacons(region)
        }
    }

    private fun triggerBeaconBroadcast(region: Region, state: Int, rssi: Int? = null) {
        val params = getBeaconCallbackParams(region, state, rssi) ?: return
        handleBeaconEvent(params)
    }

    private fun handleBeaconEvent(params: BeaconCallbackParamsWire) {
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

    private fun getBeaconCallbackParams(region: Region, state: Int, rssi: Int? = null): BeaconCallbackParamsWire? {
        val event = when (state) {
            MonitorNotifier.INSIDE -> BeaconEvent.ENTER
            MonitorNotifier.OUTSIDE -> BeaconEvent.EXIT
            else -> {
                Log.w(TAG, "Unknown beacon state: $state")
                return null
            }
        }

        val beaconWire = NativeBeaconPersistence.getAllBeacons(context).find { it.id == region.uniqueId } ?: return null
        if (!beaconWire.triggers.contains(event)) return null

        return BeaconCallbackParamsWire(
            listOf(ActiveBeaconWire(
                beaconWire.id, beaconWire.uuid, beaconWire.major, beaconWire.minor,
                rssi?.toLong(),
                beaconWire.triggers, beaconWire.androidSettings
            )),
            event,
            beaconWire.callbackHandle
        )
    }
}