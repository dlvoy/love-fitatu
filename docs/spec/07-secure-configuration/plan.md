# 07 Secure configuration: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/data/SecureStore.kt`: process-wide store.
  `unlockPlain`, `unlockCipher` / `finishUnlock` for the biometric path, `reset`, `get` / `put`,
  `setBiometric`, and the private `rewrap`, `saveBlob`, `loadBlob`. OAEP uses SHA-256 with MGF1
  SHA-1 on both sides, as the Keystore requires.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Settings.kt`: typed accessors over the store;
  `setApiKey` turns biometric mode on while any provider has a key. Deletes the plain `settings` preferences left by the
  first build.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt`: `LockScreen`, `promptUnlock`
  (`BiometricPrompt` with a `CryptoObject`, `BIOMETRIC_STRONG`), `onUnlocked`, the reset-and-warn
  path when the key is invalidated.
- Manifest: `USE_BIOMETRIC`, `android:allowBackup="false"`.

## Tests

None; the store depends on the Android Keystore. Checked by hand on a device.
