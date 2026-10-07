## Unreleased

* iOS: `createBeacon` with an invalid UUID now throws `BeaconFenceErrorCode.invalidArguments` instead of `unknown`.
* iOS: `removeBeaconById` now throws `BeaconFenceErrorCode.beaconNotFound` for an unregistered id, matching Android.
* Android: invalid UUID/major/minor values now throw `BeaconFenceErrorCode.invalidArguments`.
* Android: unexpected native exceptions are now reported as `BeaconFenceErrorCode.unknown` (with the native message and stacktrace) instead of `pluginInternal`.
* **Breaking (Android):** the default scan strategy is now `AndroidScanStrategy.foregroundService`, which shows a persistent notification. It is applied on the first `createBeacon` even if `configureAndroidMonitor` is never called. Opt out with `AndroidScannerSettings(scanStrategy: AndroidScanStrategy.jobScheduler)` (or `.intent`). See `docs/adr/0001-foreground-service-default-scan-strategy.md`.
* Android: the host app must declare `android.permission.FOREGROUND_SERVICE` for the foreground service strategy, otherwise calls fail with the new `BeaconFenceErrorCode.missingForegroundServicePermission`. On Android 14+ a granted location permission is also required (`missingLocationPermission`).
* Android: new `AndroidScannerSettings.scanStrategy` (`foregroundService`, `jobScheduler`, `intent`) and `regionExitPeriod` (default 10s).
* Android: `AndroidScannerSettings.useForegroundService` is deprecated; `true` maps to `foregroundService`, `false` to `jobScheduler`.
* Android: changing the scan strategy (`configureAndroidMonitor`, `promoteToForeground`, `demoteToBackground`) now works while beacons are monitored.
* Android: only the iBeacon parser is registered, narrowing hardware scan filters.
* Android: the watchdog only re-arms beacons that are no longer monitored and only reposts the foreground notification when it is missing.
* Android: the default foreground service notification text is now generic ("Beacon scanning active" / "Detecting nearby beacons").
