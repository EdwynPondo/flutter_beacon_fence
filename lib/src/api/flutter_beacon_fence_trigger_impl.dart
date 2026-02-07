import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';
import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';
import 'package:flutter_beacon_fence/src/model/model_mapper.dart';

class FlutterBeaconFenceTriggerImpl implements FlutterBeaconFenceTriggerApi {
  /// Cached instance of [FlutterBeaconFenceTriggerImpl]
  static FlutterBeaconFenceTriggerImpl? _instance;

  static void ensureInitialized() {
    _instance ??= FlutterBeaconFenceTriggerImpl._();
  }

  FlutterBeaconFenceTriggerImpl._() {
    FlutterBeaconFenceTriggerApi.setUp(this);
  }

  @override
  Future<void> beaconTriggered(BeaconCallbackParamsWire params) async {
    final Function? callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(params.callbackHandle));
    if (callback == null) {
      throw BeaconFenceException(code: BeaconFenceErrorCode.callbackNotFound);
    }
    if (callback is! BeaconCallback) {
      throw BeaconFenceException(
          code: BeaconFenceErrorCode.callbackInvalid,
          message: 'Invalid callback type: ${callback.runtimeType.toString()}',
          details: 'Expected: BeaconCallback');
    }
    await callback(params.fromWire());
    debugPrint('Beacon trigger callback completed.');
  }
}
