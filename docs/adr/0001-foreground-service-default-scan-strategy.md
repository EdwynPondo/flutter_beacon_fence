# Foreground service is the default Android scan strategy, with continuous scanning

On Android, the plugin uses the foreground-service scan strategy by default, and its scan periods stay at `1100ms` scan / `0ms` between in both app states. The main consumer (Pondo) must catch short beacon flares (4–8s at 1Hz) as soon as they happen, even when the app is in the background or killed. AltBeacon's own measurements say only a foreground service does this reliably: about 1s to detect a beacon and about 30s to detect it leaving. JobScheduler and intent scanning take 450–1500s to notice that a beacon has gone, so a region stays "inside" and repeat flares fire no new enter.

The defaults are continuous even though that costs battery, for this reason: AltBeacon always runs its main scan cycle in `SCAN_MODE_LOW_LATENCY`. It only uses a low-power, hardware-filtered scan *between* cycles, and only when `betweenScanPeriod > 6000ms`, because Android N allows at most 5 scan starts in 30s. Any gap of 6s or more lets the low-power scan miss part of a 4–8s flare. So the only real choices are continuous full-power scanning or missed flares. We chose continuous scanning.

## Consequences

- An app that doesn't choose a strategy gets a persistent notification. To opt out, it passes `scanStrategy: AndroidScanStrategy.jobScheduler` (or `.intent`).
- If the host app lacks the foreground-service permissions, the call fails with `missingForegroundServicePermission` (including `createBeacon` when the default is applied implicitly). It does not silently fall back to another strategy.
- Lowering battery use further needs beacon-side changes, such as advertising faster during a flare. Changing these phone-side defaults trades battery for missed flares.
