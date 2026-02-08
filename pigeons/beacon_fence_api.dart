import 'package:pigeon/pigeon.dart';

// After modifying this file run:
// dart run pigeon --input pigeons/beacon_fence_api.dart && dart format .

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/src/generated/platform_bindings.g.dart',
  dartPackageName: 'flutter_beacon_fence',
  swiftOut: 'ios/Classes/Generated/FlutterBindings.g.swift',
  kotlinOut:
      'android/src/main/kotlin/com/flutter/beacon_fence/generated/FlutterBindings.g.kt',
  kotlinOptions: KotlinOptions(package: 'com.flutter.beacon_fence.generated'),
))

/// Bluetooth beacon events.
enum BeaconEvent {
  enter(),
  exit();
}

enum BeaconFenceErrorCode {
  unknown,

  /// A plugin internal error. Please report these as bugs on GitHub.
  pluginInternal,

  /// The arguments passed to the method are invalid.
  invalidArguments,

  /// An error occurred while communicating with the native platform.
  channelError,

  /// The required location permission was not granted.
  ///
  /// On Android we need: `ACCESS_FINE_LOCATION`
  /// On iOS we need: `NSLocationWhenInUseUsageDescription`
  ///
  /// Please use an external permission manager such as "permission_handler" to
  /// request the permission from the user.
  missingLocationPermission,

  /// The required background location permission was not granted.
  ///
  /// On Android we need: `ACCESS_BACKGROUND_LOCATION` (for API level 29+)
  /// On iOS we need: `NSLocationAlwaysAndWhenInUseUsageDescription`
  ///
  /// Please use an external permission manager such as "permission_handler" to
  /// request the permission from the user.
  missingBackgroundLocationPermission,

  /// The specified beacon callback was not found.
  /// This can happen for old beacon callback functions that were
  /// moved/renamed. Please re-create those beacon.
  callbackNotFound,

  /// The specified beacon callback function signature is invalid.
  /// This can happen if the callback function signature has changed or due to
  /// plugin contract changes.
  callbackInvalid,

  /// The required Bluetooth permission was not granted.
  ///
  /// On Android we need: `BLUETOOTH_SCAN` (for API level 31+)
  /// On iOS we need: `NSBluetoothAlwaysUsageDescription`
  ///
  /// Please use an external permission manager such as "permission_handler" to
  /// request the permission from the user.
  missingBluetoothPermission,

  /// Bluetooth is not enabled on the device.
  /// The user needs to enable Bluetooth in device settings.
  bluetoothNotEnabled,

  /// The beacon deletion failed because the beacon was not found.
  /// This is safe to ignore.
  beaconNotFound,
}

class IosBeaconSettingsWire {
  final bool initialTrigger;
  final bool notifyEntryStateOnDisplay;

  const IosBeaconSettingsWire({
    required this.initialTrigger,
    required this.notifyEntryStateOnDisplay,
  });
}

class AndroidBeaconSettingsWire {
  final List<BeaconEvent> initialTriggers;

  const AndroidBeaconSettingsWire({
    required this.initialTriggers,
  });
}

class AndroidScannerSettingsWire {
  final int foregroundScanPeriodMillis;
  final int foregroundBetweenScanPeriodMillis;
  final int backgroundScanPeriodMillis;
  final int backgroundBetweenScanPeriodMillis;
  final bool useForegroundService;

  const AndroidScannerSettingsWire({
    required this.foregroundScanPeriodMillis,
    required this.foregroundBetweenScanPeriodMillis,
    required this.backgroundScanPeriodMillis,
    required this.backgroundBetweenScanPeriodMillis,
    required this.useForegroundService,
  });
}

class BeaconWire {
  final String id;
  final String uuid;
  final int? major;
  final int? minor;
  final List<BeaconEvent> triggers;
  final IosBeaconSettingsWire iosSettings;
  final AndroidBeaconSettingsWire androidSettings;
  final int callbackHandle;

  const BeaconWire({
    required this.id,
    required this.uuid,
    this.major,
    this.minor,
    required this.triggers,
    required this.iosSettings,
    required this.androidSettings,
    required this.callbackHandle,
  });
}

class ActiveBeaconWire {
  final String id;
  final String uuid;
  final int? major;
  final int? minor;
  final int? rssi;
  final List<BeaconEvent> triggers;
  final AndroidBeaconSettingsWire? androidSettings;

  const ActiveBeaconWire({
    required this.id,
    required this.uuid,
    this.major,
    this.minor,
    this.rssi,
    required this.triggers,
    required this.androidSettings,
  });
}

class BeaconCallbackParamsWire {
  final List<ActiveBeaconWire> beacons;
  final BeaconEvent event;
  final int callbackHandle;

  const BeaconCallbackParamsWire({
    required this.beacons,
    required this.event,
    required this.callbackHandle,
  });
}

@HostApi()
abstract class FlutterBeaconFenceApi {
  void initialize({required int callbackDispatcherHandle});

  @async
  void createBeacon({required BeaconWire beacon});

  void reCreateAfterReboot();

  List<String> getBeaconIds();

  List<ActiveBeaconWire> getBeacons();

  @async
  void removeBeaconById({required String id});

  @async
  void removeAllBeacons();

  @async
  void configureAndroidMonitor({required AndroidScannerSettingsWire settings});
}

@HostApi()
abstract class FlutterBeaconFenceBackgroundApi {
  void triggerApiInitialized();

  void promoteToForeground();

  void demoteToBackground();
}

@FlutterApi()
abstract class FlutterBeaconFenceTriggerApi {
  @async
  void beaconTriggered(BeaconCallbackParamsWire params);
}
