package com.flutter.beacon_fence.util

import com.flutter.beacon_fence.generated.BeaconFenceErrorCode
import com.flutter.beacon_fence.generated.FlutterError

/**
 * Builds a [FlutterError] whose code is decoded on the Dart side into a
 * [BeaconFenceErrorCode]. Use only for business logic or validation failures;
 * unexpected exceptions should be forwarded as-is and surface as `unknown`.
 */
fun BeaconFenceErrorCode.toFlutterError(message: String?): FlutterError =
    FlutterError(raw.toString(), message, null)
