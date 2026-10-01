package com.flutter.beacon_fence.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.flutter.beacon_fence.api.BeaconFenceApiImpl
import org.altbeacon.beacon.BeaconManager

class BeaconWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
            val apiImpl = BeaconFenceApiImpl(applicationContext, beaconManager)
            BeaconScannerService(
                applicationContext,
                beaconManager,
                apiImpl
            ).reactivateScanning()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}