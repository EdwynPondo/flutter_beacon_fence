import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';

import 'notifications_repository.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  MyAppState createState() => MyAppState();
}

@pragma('vm:entry-point')
Future<void> beaconEnterTriggered(BeaconCallbackParams params) async {
  debugPrint('Beacon enter triggered: ${params.event}');
  final SendPort? sendPort =
      IsolateNameServer.lookupPortByName('flutter_beacon_fence_send_port');
  sendPort
      ?.send('Entered beacon: ${params.beacons.map((e) => e.id).join(', ')}');
}

class MyAppState extends State<MyApp> {
  String beaconfenceState = 'N/A';
  ReceivePort port = ReceivePort();

  @override
  void initState() {
    super.initState();
    unawaited(NotificationsRepository().init());
    IsolateNameServer.registerPortWithName(
      port.sendPort,
      'flutter_beacon_fence_send_port',
    );
    port.listen((dynamic data) {
      debugPrint('Event: $data');
      setState(() {
        beaconfenceState = data;
      });
    });
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    debugPrint('Initializing...');
    await FlutterBeaconFenceManager.instance.initialize();
    await FlutterBeaconFenceManager.instance.configureAndroidMonitor(
      const AndroidScannerSettings(
        notificationSettings:
            AndroidNotificationSettings(title: 'title', content: 'content'),
      ),
    );
    await FlutterBeaconFenceManager.instance.removeAllBeacons();
    await FlutterBeaconFenceManager.instance.createBeacon(
      Beacon(
        id: 'pondo_beacon_session',
        uuid: '39ED98FF-2900-441A-802F-9C398FC199D2',
        triggers: {BeaconEvent.enter},
        iosSettings: IosBeaconSettings(initialTrigger: true),
        androidSettings:
            AndroidBeaconSettings(initialTriggers: {BeaconEvent.enter}),
      ),
      beaconEnterTriggered,
    );
    debugPrint('Initialization done');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Beacons Fence'),
        ),
        body: Container(
          padding: const EdgeInsets.all(20.0),
          child: SingleChildScrollView(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text('Current state: $beaconfenceState'),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
