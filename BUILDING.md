# Building and running on a device

`scripts/run-debug.sh` (Git Bash, Linux, macOS) and `scripts/run-debug.ps1` (PowerShell) do the
same thing:

1. read signing settings from `scripts/.env`,
2. build the **debug** APK (`./gradlew assembleDebug`), signed with your keystore instead of the
   default Android debug key,
3. install it on the connected device (`adb install -r`),
4. start the app.

Signing the debug build with the release keystore means it installs over a release build from
GitHub (and the other way round) without uninstalling and losing the app's data.

## Requirements

- JDK 17 or newer. The scripts use `JAVA_HOME` from `scripts/.env`, else from the environment, and
  fall back to Android Studio's bundled JDK when that one is missing or older than 17.
- Android SDK platform-tools (`adb`). Found on `PATH`, through `ADB`, `ANDROID_HOME` /
  `ANDROID_SDK_ROOT`, or `sdk.dir` in `local.properties`.
- A device with USB (or wireless) debugging on, authorised for this computer: `adb devices` must
  list it as `device`.
- A keystore. Use the release keystore (see [DEPLOYMENT.md](DEPLOYMENT.md#signing-key)), or create
  a separate one for local builds with `keytool -genkeypair`.

## Setup

```sh
cp scripts/.env.example scripts/.env
```

Edit `scripts/.env`:

| Key | Value |
| --- | --- |
| `KEYSTORE_PATH` | Path to the `.jks`; absolute, or relative to `scripts/` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password (same as the keystore password for PKCS12) |
| `JAVA_HOME` | Optional: JDK 17+ to build with |
| `ANDROID_SERIAL` | Optional: device serial when several devices are connected |

Values are read literally: no variable expansion, so `$` and `#` in passwords are fine. Quotes
around a value are removed.

`scripts/.env` is git-ignored. Never commit it, and keep the keystore outside the repository.

## Usage

```sh
scripts/run-debug.sh              # build, install, start
scripts/run-debug.sh --no-launch  # build and install only
```

```powershell
.\scripts\run-debug.ps1
.\scripts\run-debug.ps1 -NoLaunch
```

If PowerShell refuses to run scripts, use
`powershell -ExecutionPolicy Bypass -File scripts\run-debug.ps1`.

## How it works

The scripts pass the settings to Gradle as `DEBUG_SIGNING_KEYSTORE_PATH`,
`DEBUG_SIGNING_KEYSTORE_PASSWORD`, `DEBUG_SIGNING_KEY_ALIAS` and `DEBUG_SIGNING_KEY_PASSWORD`.
`app/build.gradle.kts` uses them for the debug build type when they are set; without them (plain
`./gradlew assembleDebug`, Android Studio, CI) the debug APK is signed with the default debug key
as usual. Release signing is separate and described in [DEPLOYMENT.md](DEPLOYMENT.md).

## Troubleshooting

- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**: the app on the device was signed with a different key
  (for example a build from Android Studio). Uninstall it first, which deletes its history and
  settings: `adb uninstall pl.cukrzycowy.labelbite`.
- **No device / several devices**: check `adb devices`; set `ANDROID_SERIAL` in `scripts/.env` to
  pick one.
- **`Gradle requires JVM 17 or later`**: set `JAVA_HOME` in `scripts/.env` to a JDK 17+.
