package com.flutter.beacon_fence.services

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.flutter.beacon_fence.Constants
import com.flutter.beacon_fence.generated.AndroidScanStrategy
import com.flutter.beacon_fence.util.NativeBeaconPersistence
import java.util.concurrent.TimeUnit

class BeaconWatchdogService(private val context: Context) {
    companion object {
        private const val TAG = "BeaconWatchdogWorker"
    }

    fun updateWatchdogState() {
        val settings = NativeBeaconPersistence.getScannerSettings(context)
        val usesForegroundService = settings?.scanStrategy == AndroidScanStrategy.FOREGROUND_SERVICE
        val hasBeacons = NativeBeaconPersistence.getAllBeaconIds(context).isNotEmpty()

        if (usesForegroundService && hasBeacons) {
            enqueueWatchdog()
        } else {
            cancelWatchdog()
        }
    }

    private fun enqueueWatchdog() {
        Log.d(TAG, "Enqueuing periodic watchdog work request")
        val watchdogRequest = PeriodicWorkRequestBuilder<BeaconWatchdogWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.BEACON_WATCHDOG_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            watchdogRequest
        )
    }

    private fun cancelWatchdog() {
        Log.d(TAG, "Cancelling periodic watchdog work request")
        WorkManager.getInstance(context).cancelUniqueWork(Constants.BEACON_WATCHDOG_WORK_NAME)
    }
}
