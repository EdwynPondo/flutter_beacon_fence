import 'dart:async';

import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';
import 'package:flutter_beacon_fence/src/model/model_mapper.dart';

class FlutterBeaconBackgroundManager {
  static FlutterBeaconBackgroundManager? _instance;

  /// The singleton instance of [FlutterBeaconBackgroundManager].
  ///
  /// WARNING: Can only be accessed within Beacon callbacks. Trying to access
  /// this anywhere else will throw an [AssertionError].
  static FlutterBeaconBackgroundManager get instance {
    assert(
        _instance != null,
        'FlutterBeaconBackgroundManager has not been initialized yet; '
        'Are you running within a Beacon callback?');
    return _instance!;
  }

  final FlutterBeaconFenceBackgroundApi _api;

  FlutterBeaconBackgroundManager._(this._api);

  /// Promote the beacon callback to an Android foreground service.
  ///
  /// Android only, has no effect on iOS (but is safe to call).
  ///
  /// Throws [BeaconFenceExceptionMapper].
  Future<void> promoteToForeground() async => _api
      .promoteToForeground()
      .catchError(BeaconFenceExceptionMapper.catchError<void>);

  /// Demote the beacon service from an Android foreground service to a
  /// background service.
  ///
  /// Android only, has no effect on iOS (but is safe to call).
  ///
  /// Throws [BeaconFenceExceptionMapper].
  Future<void> demoteToBackground() async => _api
      .demoteToBackground()
      .catchError(BeaconFenceExceptionMapper.catchError<void>);
}

/// Private method internal to plugin, do not use.
Future<void> createFlutterBeaconBackgroundManagerInstance() async {
  final api = FlutterBeaconFenceBackgroundApi();
  FlutterBeaconBackgroundManager._instance =
      FlutterBeaconBackgroundManager._(api);
  await api.triggerApiInitialized();
}
