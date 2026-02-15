import 'dart:async';
import 'dart:ui';

import 'package:flutter_beacon_fence/src/callback_dispatcher.dart';
import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';
import 'package:flutter_beacon_fence/src/model/beacon_fence_exception.dart';
import 'package:flutter_beacon_fence/src/model/beacon_models.dart';
import 'package:flutter_beacon_fence/src/model/model_mapper.dart';
import 'package:flutter_beacon_fence/src/typedefs.dart';

class FlutterBeaconFenceManager {
  /// Cached instance of [FlutterBeaconFenceManager]
  static FlutterBeaconFenceManager? _instance;

  /// The singleton instance of [FlutterBeaconFenceManager].
  ///
  /// Throws [BeaconFenceException].
  static FlutterBeaconFenceManager get instance {
    try {
      _instance ??= FlutterBeaconFenceManager._();
    } catch (e, stackTrace) {
      throw BeaconFenceExceptionMapper.fromError(e, stackTrace);
    }
    return _instance!;
  }

  final FlutterBeaconFenceApi _api;

  FlutterBeaconFenceManager._() : _api = FlutterBeaconFenceApi();

  /// Initialize the plugin.
  ///
  /// Must be called before any other method.
  ///
  /// Throws [BeaconFenceException].
  Future<void> initialize() async {
    final CallbackHandle? callback;
    try {
      callback = PluginUtilities.getCallbackHandle(callbackDispatcher);
    } catch (e, stackTrace) {
      throw BeaconFenceExceptionMapper.fromError(e, stackTrace);
    }
    if (callback == null) {
      throw BeaconFenceException.internal(
          message: 'Callback dispatcher is invalid.');
    }
    return _api
        .initialize(callbackDispatcherHandle: callback.toRawHandle())
        .catchError(BeaconFenceExceptionMapper.catchError<void>);
  }

  /// Register for beacon events for a [Beacon].
  ///
  /// [beacon] is the beacon region to register with the system.
  /// [callback] is the method to be called when a beacon event associated
  /// with [beacon] occurs.
  ///
  /// Throws [BeaconFenceException].
  Future<void> createBeacon(Beacon beacon, BeaconCallback callback) async {
    if (beacon.id.isEmpty) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon ID cannot be empty.');
    }
    if (beacon.triggers.isEmpty) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon triggers cannot be empty.');
    }
    if (beacon.uuid.isEmpty) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon UUID cannot be empty.');
    }
    // Validate UUID format (basic check)
    final uuidRegex = RegExp(
        r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
    if (!uuidRegex.hasMatch(beacon.uuid)) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon UUID format is invalid. Expected format: '
              'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX');
    }
    if (beacon.major != null && (beacon.major! < 0 || beacon.major! > 65535)) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon major value must be between 0 and 65535.');
    }
    if (beacon.minor != null && (beacon.minor! < 0 || beacon.minor! > 65535)) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon minor value must be between 0 and 65535.');
    }
    if (beacon.minor != null && beacon.major == null) {
      throw BeaconFenceException.invalidArgument(
          message: 'Beacon minor value requires major value to be set.');
    }
    final CallbackHandle? callbackHandle;
    try {
      callbackHandle = PluginUtilities.getCallbackHandle(callback);
    } catch (e, stackTrace) {
      throw BeaconFenceExceptionMapper.fromError(e, stackTrace);
    }
    if (callbackHandle == null) {
      throw BeaconFenceException.invalidArgument(
          message: 'Callback is invalid.');
    }
    return _api
        .createBeacon(beacon: beacon.toWire(callbackHandle.toRawHandle()))
        .catchError(BeaconFenceExceptionMapper.catchError<void>);
  }

  /// Re-register beacons after reboot.
  ///
  /// Optional: This function can be called when the autostart feature is not
  /// working as it should (e.g. for some Android OEMs). This way you can ensure
  /// all Beacons are re-created at app launch.
  ///
  /// Throws [BeaconFenceException].
  Future<void> reCreateAfterReboot() async => _api
      .reCreateAfterReboot()
      .catchError(BeaconFenceExceptionMapper.catchError<void>);

  /// Get all registered [Beacon] IDs.
  ///
  /// If there are no beacons registered it returns an empty list.
  ///
  /// Throws [BeaconFenceException].
  Future<List<String>> getRegisteredBeaconIds() async => _api
      .getBeaconIds()
      .catchError(BeaconFenceExceptionMapper.catchError<List<String>>);

  /// Get all [Beacon] regions and their properties.
  ///
  /// If there are no beacons registered it returns an empty list.
  ///
  /// Throws [BeaconFenceException].
  Future<List<ActiveBeacon>> getRegisteredBeacons() async => _api
      .getBeacons()
      .then((value) => value.map((e) => e.fromWire()).toList())
      .catchError(BeaconFenceExceptionMapper.catchError<List<ActiveBeacon>>);

  /// Stop receiving beacon events for a given [Beacon].
  ///
  /// If the [Beacon] is not registered, this method does nothing.
  ///
  /// Throws [BeaconFenceException]. Might throw
  /// [BeaconFenceErrorCode.beaconNotFound] on Android.
  Future<void> removeBeacon(Beacon beacon) async => removeBeaconById(beacon.id);

  /// Stop receiving beacon events for an identifier associated with a
  /// beacon region.
  ///
  /// If a [Beacon] with the given ID is not registered, this method does
  /// nothing.
  ///
  /// Throws [BeaconFenceException]. Might throw
  /// [BeaconFenceErrorCode.beaconNotFound] on Android.
  Future<void> removeBeaconById(String id) async => _api
      .removeBeaconById(id: id)
      .catchError(BeaconFenceExceptionMapper.catchError<void>);

  /// Stop receiving beacon events for all registered beacons.
  ///
  /// If there are no beacons registered, this method does nothing.
  ///
  /// Throws [BeaconFenceException].
  Future<void> removeAllBeacons() async => _api
      .removeAllBeacons()
      .catchError(BeaconFenceExceptionMapper.catchError<void>);

  /// Configure Android beacon scanner settings.
  ///
  /// This method is only available on Android. On iOS it does nothing.
  ///
  /// Throws [BeaconFenceException].
  Future<void> configureAndroidMonitor(
    AndroidScannerSettings settings,
  ) async =>
      _api
          .configureAndroidMonitor(settings: settings.toWire())
          .catchError(BeaconFenceExceptionMapper.catchError<void>);
}
