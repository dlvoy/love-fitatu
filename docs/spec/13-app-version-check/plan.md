# 13 App version check: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/automation/AppVersions.kt`
  - `SteeredApp` (`FITATU`, `AAPS`): label, storage key, minimum version (`4.17.0`, `4.0.0`).
  - `VersionCheck(packageName, version, updatedAt, ok, at)`: `describe`, `sameFinding` (equal
    apart from the check time), `encode` / `decode` for storage.
  - `AppVersions.atLeast` compares the leading numeric parts of two versions; `evaluate` builds
    the result from a version (pure); `check` reads `versionName` and `lastUpdateTime` from
    `PackageManager.getPackageInfo`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Settings.kt`: `versionCheck(app)` /
  `setVersionCheck(app, check)`, kept in `SecureStore` like the other settings.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt` `run`: checks each job's app, stores
  the result unless the stored one is the `sameFinding`, and stops the run on a failure.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/SettingsScreen.kt` `VersionCheckRow`: the button
  and the stored result (see [08](../08-settings-and-model-picker/plan.md)).
- Both packages are already visible through the manifest `<queries>`; no new permission.

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `appVersionComparison`, `appVersionCheckResult`
