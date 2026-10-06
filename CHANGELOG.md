## Unreleased

* iOS: `createBeacon` with an invalid UUID now throws `BeaconFenceErrorCode.invalidArguments` instead of `unknown`.
* iOS: `removeBeaconById` now throws `BeaconFenceErrorCode.beaconNotFound` for an unregistered id, matching Android.
* Android: invalid UUID/major/minor values now throw `BeaconFenceErrorCode.invalidArguments`.
* Android: unexpected native exceptions are now reported as `BeaconFenceErrorCode.unknown` (with the native message and stacktrace) instead of `pluginInternal`.
