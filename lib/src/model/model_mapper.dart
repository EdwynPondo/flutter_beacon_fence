import 'package:flutter/services.dart';
import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';
import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';

extension BeaconFenceExceptionMapper on BeaconFenceException {
  static BeaconFenceException fromPlatformException(PlatformException ex) {
    return BeaconFenceException(
      code: ex.code == 'channel-error'
          ? BeaconFenceErrorCode.channelError
          : BeaconFenceErrorCode.values.firstWhere(
              (e) => e.index == (int.tryParse(ex.code) ?? 0),
              orElse: () => BeaconFenceErrorCode.unknown,
            ),
      message: ex.message,
      details: ex.details,
      stacktrace: ex.stacktrace,
    );
  }

  static BeaconFenceException fromException(Exception ex,
      [StackTrace? stacktrace]) {
    return BeaconFenceException(
      code: BeaconFenceErrorCode.unknown,
      message: ex.toString(),
      stacktrace: stacktrace?.toString() ?? StackTrace.current.toString(),
    );
  }

  static BeaconFenceException fromError(dynamic error,
      [StackTrace? stacktrace]) {
    if (error is BeaconFenceException) {
      return error;
    }
    if (error is PlatformException) {
      return fromPlatformException(error);
    }
    if (error is Exception) {
      return fromException(error, stacktrace);
    }
    return BeaconFenceException(
      code: BeaconFenceErrorCode.unknown,
      message: error.toString(),
      stacktrace: stacktrace?.toString() ?? StackTrace.current.toString(),
    );
  }

  static T catchError<T>(dynamic error, StackTrace stacktrace) {
    throw fromError(error, stacktrace);
  }
}

extension IosBeaconSettingsMapper on IosBeaconSettings {
  IosBeaconSettingsWire toWire() {
    return IosBeaconSettingsWire(
      initialTrigger: initialTrigger,
      notifyEntryStateOnDisplay: notifyEntryStateOnDisplay,
    );
  }
}

extension IosBeaconSettingsWireMapper on IosBeaconSettingsWire {
  IosBeaconSettings fromWire() {
    return IosBeaconSettings(
      initialTrigger: initialTrigger,
      notifyEntryStateOnDisplay: notifyEntryStateOnDisplay,
    );
  }
}

extension AndroidBeaconSettingsMapper on AndroidBeaconSettings {
  AndroidBeaconSettingsWire toWire() {
    return AndroidBeaconSettingsWire(
      initialTriggers: initialTriggers.toList(),
    );
  }
}

extension AndroidBeaconSettingsWireMapper on AndroidBeaconSettingsWire {
  AndroidBeaconSettings fromWire() {
    return AndroidBeaconSettings(
      initialTriggers: initialTriggers.toSet(),
    );
  }
}

extension AndroidScannerSettingsMapper on AndroidScannerSettings {
  AndroidScannerSettingsWire toWire() {
    return AndroidScannerSettingsWire(
      foregroundScanPeriodMillis: foregroundScanPeriod.inMilliseconds,
      foregroundBetweenScanPeriodMillis:
          foregroundBetweenScanPeriod.inMilliseconds,
      backgroundScanPeriodMillis: backgroundScanPeriod.inMilliseconds,
      backgroundBetweenScanPeriodMillis:
          backgroundBetweenScanPeriod.inMilliseconds,
    );
  }
}

extension BeaconMapper on Beacon {
  BeaconWire toWire(int callbackHandle) {
    return BeaconWire(
      id: id,
      uuid: uuid,
      major: major,
      minor: minor,
      triggers: triggers.toList(),
      iosSettings: iosSettings.toWire(),
      androidSettings: androidSettings.toWire(),
      callbackHandle: callbackHandle,
    );
  }
}

extension BeaconWireMapper on BeaconWire {
  Beacon fromWire() {
    return Beacon(
      id: id,
      uuid: uuid,
      major: major,
      minor: minor,
      triggers: triggers.toSet(),
      iosSettings: iosSettings.fromWire(),
      androidSettings: androidSettings.fromWire(),
    );
  }
}

extension ActiveBeaconMapper on ActiveBeacon {
  ActiveBeaconWire toWire() {
    return ActiveBeaconWire(
      id: id,
      uuid: uuid,
      major: major,
      minor: minor,
      rssi: rssi,
      triggers: triggers.toList(),
      androidSettings: androidSettings?.toWire(),
    );
  }
}

extension ActiveBeaconWireMapper on ActiveBeaconWire {
  ActiveBeacon fromWire() {
    return ActiveBeacon(
      id: id,
      uuid: uuid,
      major: major,
      minor: minor,
      rssi: rssi,
      triggers: triggers.toSet(),
      androidSettings: androidSettings?.fromWire(),
    );
  }
}

extension BeaconCallbackParamsWireMapper on BeaconCallbackParamsWire {
  BeaconCallbackParams fromWire() {
    return BeaconCallbackParams(
      beacons: beacons.map((e) => e.fromWire()).toList(),
      event: event,
    );
  }
}
