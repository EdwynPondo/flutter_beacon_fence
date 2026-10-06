// ignore_for_file: deprecated_member_use_from_same_package

import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';
import 'package:flutter_beacon_fence/src/model/model_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AndroidScannerSettings', () {
    test('defaults to the foreground service scan strategy', () {
      const settings = AndroidScannerSettings();
      expect(settings.scanStrategy, AndroidScanStrategy.foregroundService);
      expect(settings.regionExitPeriod, const Duration(seconds: 10));
    });

    test('maps deprecated useForegroundService when no strategy is given', () {
      expect(
        const AndroidScannerSettings(useForegroundService: true).scanStrategy,
        AndroidScanStrategy.foregroundService,
      );
      expect(
        const AndroidScannerSettings(useForegroundService: false).scanStrategy,
        AndroidScanStrategy.jobScheduler,
      );
    });

    test('scanStrategy wins over deprecated useForegroundService', () {
      const settings = AndroidScannerSettings(
        useForegroundService: true,
        scanStrategy: AndroidScanStrategy.intent,
      );
      expect(settings.scanStrategy, AndroidScanStrategy.intent);
    });

    test('toWire sends the strategy and region exit period', () {
      final wire = const AndroidScannerSettings(
        scanStrategy: AndroidScanStrategy.jobScheduler,
        regionExitPeriod: Duration(seconds: 30),
        backgroundBetweenScanPeriod: Duration(seconds: 10),
        notificationSettings: AndroidNotificationSettings(
          title: 'title',
          content: 'content',
        ),
      ).toWire();
      expect(wire.scanStrategy, AndroidScanStrategy.jobScheduler);
      expect(wire.regionExitPeriodMillis, 30000);
      expect(wire.foregroundScanPeriodMillis, 1100);
      expect(wire.backgroundBetweenScanPeriodMillis, 10000);
      expect(wire.notificationsSettings?.title, 'title');
    });
  });
}
