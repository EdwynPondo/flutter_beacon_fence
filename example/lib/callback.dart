import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';
import 'package:flutter_beacon_fence_example/notifications_repository.dart';

@pragma('vm:entry-point')
Future<void> beaconFenceTriggered(BeaconCallbackParams params) async {
  debugPrint('beaconFenceTriggered params: $params');
  final SendPort? send =
      IsolateNameServer.lookupPortByName('flutter_beacon_fence_send_port');
  send?.send(params.event.name);

  final notificationsRepository = NotificationsRepository();
  // TODO: Test to see what happens if we do not initialize the Notifications
  // plugin during callbacks.
  await notificationsRepository.init();
  final title =
      'Beacon ${capitalize(params.event.name)}: ${params.beacons.map((e) => e.id).join(', ')}';
  final message = 'Beacons:\n'
      '${params.beacons.map((e) => '• ID: ${e.id}, '
          'Major=${e.major}, '
          'Minor=${e.minor}, '
          'RSSI=${e.rssi}, '
          'Triggers=${e.triggers.map((e) => e.name).join(',')}').join('\n')}\n'
      'Event: ${params.event.name}\n';
  await notificationsRepository.showBeaconFenceTriggerNotification(
      title, message);

  await Future.delayed(const Duration(seconds: 1));
}

String capitalize(String text) {
  if (text.isEmpty) return text;
  return text[0].toUpperCase() + text.substring(1);
}
