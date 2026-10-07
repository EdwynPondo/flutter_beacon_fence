# Beacon Fence

A Flutter plugin that watches iBeacon regions natively on Android and iOS, and reports region entry and exit to Dart.

## Language

### Scanning (Android)

**App state**:
Whether the host app's UI is visible to the user, either foreground or background. It decides which set of scan periods applies.
_Avoid_: Mode, foreground mode, background mode

**Scan strategy**:
How Android keeps scanning for beacons running: a foreground service with a persistent notification, OS-scheduled jobs, or OS-delivered scan intents. It is independent of the app state.
_Avoid_: Mode, scanning mode

### Errors

**Call error**:
A failure that happens while a call started from Dart is running on the native side. It is returned as the result of that call.
_Avoid_: Platform error, native error

**Background error**:
A failure that happens outside any Dart call, such as while scanning, in a background worker, or in an OS location callback. Dart is not told about it today.

**Error code**:
A `BeaconFenceErrorCode` value. It names a known business-rule or validation failure, such as an unknown beacon or invalid arguments. Any unexpected failure is reported as `unknown`.
_Avoid_: Error type, error kind
