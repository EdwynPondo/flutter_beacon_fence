package com.flutter.beacon_fence

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.flutter.beacon_fence.api.BeaconFenceBackgroundApiImpl
import com.flutter.beacon_fence.generated.FlutterBeaconFenceBackgroundApi
import com.flutter.beacon_fence.generated.NativeBeaconBackgroundApi
import com.flutter.beacon_fence.generated.NativeBeaconTriggerApi
import com.flutter.beacon_fence.model.BeaconCallbackParamsStorage
import com.flutter.beacon_fence.util.Notifications
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor.DartCallback
import io.flutter.view.FlutterCallbackInformation
import kotlinx.serialization.json.Json

class FlutterBeaconFenceBackgroundWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) :
    ListenableWorker(context, workerParams) {
    companion object {
        const val TAG = "FlutterBeaconFenceBackgroundWorker"
        // TODO: Consider using random ID.
        private const val NOTIFICATION_ID = 493620
        private val flutterLoader = FlutterInjector.instance().flutterLoader()
    }

    private var flutterEngine: FlutterEngine? = null

    private var startTime: Long = 0

    private var completer: CallbackToFutureAdapter.Completer<Result>? = null

    private var resolvableFuture =
        CallbackToFutureAdapter.getFuture { completer ->
            this.completer = completer
            null
        }

    private var backgroundApiImpl: BeaconFenceBackgroundApiImpl? = null

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // This method does not need to be implemented for Android 31 (S) and above.
            return super.getForegroundInfoAsync()
        }
        val notification = Notifications.createBackgroundWorkerNotification(context)
        return Futures.immediateFuture(ForegroundInfo(NOTIFICATION_ID, notification))
    }

    override fun startWork(): ListenableFuture<Result> {
        startTime = System.currentTimeMillis()

        flutterEngine = FlutterEngine(applicationContext)

        if (!flutterLoader.initialized()) {
            flutterLoader.startInitialization(applicationContext)
        }

        flutterLoader.ensureInitializationCompleteAsync(
            applicationContext,
            null,
            Handler(Looper.getMainLooper()),
        ) {
            val sharedPrefs = context.getSharedPreferences(
                Constants.SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )
            val callbackHandle = sharedPrefs.getLong(Constants.BEACON_CALLBACK_DISPATCHER_HANDLE_KEY, 0)
            
            if (callbackHandle == 0L) {
                Log.e(TAG, "No callback dispatcher registered.")
                stopEngine(Result.failure())
                return@ensureInitializationCompleteAsync
            }

            flutterEngine?.let { engine ->
                val callbackInfo =
                    FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
                if (callbackInfo == null) {
                    Log.e(TAG, "Failed to find callback dispatcher.")
                    stopEngine(Result.failure())
                    return@ensureInitializationCompleteAsync
                }

                backgroundApiImpl = BeaconFenceBackgroundApiImpl(context, this)
                FlutterBeaconFenceBackgroundApi.setUp(
                    engine.dartExecutor.binaryMessenger,
                    backgroundApiImpl
                )

                engine.dartExecutor.executeDartCallback(
                    DartCallback(
                        context.assets,
                        flutterLoader.findAppBundlePath(),
                        callbackInfo
                    )
                )
            }
        }

        return resolvableFuture
    }

    override fun onStopped() {
        stopEngine(null)
    }

    fun triggerApiReady() {
        val lEngine = flutterEngine
        if (lEngine == null) {
            Log.e(TAG, "FlutterEngine was null.")
            stopEngine(Result.failure())
            return
        }

        val jsonData = workerParams.inputData.getString(Constants.WORKER_PAYLOAD_KEY)
        if (jsonData == null) {
            Log.e(TAG, "Worker payload was missing.")
            stopEngine(Result.failure())
            return
        }

        try {
            val beaconParams = Json.decodeFromString<BeaconCallbackParamsStorage>(jsonData).toWire()
            val nativeBeaconTriggerApi = NativeBeaconTriggerApi(lEngine.dartExecutor.binaryMessenger)
            Log.d(TAG, "Triggering beacon callback.")
            nativeBeaconTriggerApi.beaconTriggered(beaconParams) {
                stopEngine(Result.success())
            }
            return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse worker payload of beacon. Data=$jsonData Error=$e")
            stopEngine(Result.failure())
        }
    }

    private fun stopEngine(result: Result?) {
        val fetchDuration = System.currentTimeMillis() - startTime

        // No result indicates we were signalled to stop by WorkManager. The result is already
        // STOPPED, so no need to resolve another one.
        if (result != null) {
            this.completer?.set(result)
        }

        // If stopEngine is called from `onStopped`, it may not be from the main thread.
        Handler(Looper.getMainLooper()).post {
            flutterEngine?.destroy()
            flutterEngine = null
        }

        Log.d(TAG, "Work took ${fetchDuration}ms.")
    }
}
