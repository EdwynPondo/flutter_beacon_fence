import 'package:flutter/material.dart';

import 'package:flutter_beacon_fence/src/api/flutter_beacon_fence_trigger_impl.dart';
import 'package:flutter_beacon_fence/src/flutter_beacon_fence_background_manager.dart';

@pragma('vm:entry-point')
void callbackDispatcher() {
  debugPrint('Callback dispatcher called.');
  // Setup connection between platform and Flutter.
  WidgetsFlutterBinding.ensureInitialized();
  // Create the NativeBeaconTriggerApi.
  FlutterBeaconFenceTriggerImpl.ensureInitialized();
  // Create the NativeBeaconBackgroundApi.
  createFlutterBeaconBackgroundManagerInstance();
}
