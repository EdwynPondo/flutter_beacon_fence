# Beacon Fence

A Flutter plugin that watches iBeacon regions natively on Android and iOS, and reports region entry and exit to Dart.

## Language

### Errors

**Call error**:
A failure that happens while a call started from Dart is running on the native side. It is returned as the result of that call.
_Avoid_: Platform error, native error

**Background error**:
A failure that happens outside any Dart call, such as while scanning, in a background worker, or in an OS location callback. Dart is not told about it today.

**Error code**:
A `BeaconFenceErrorCode` value. It names a known business-rule or validation failure, such as an unknown beacon or invalid arguments. Any unexpected failure is reported as `unknown`.
_Avoid_: Error type, error kind
