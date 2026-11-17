# 07 Secure configuration

## Goal

Keep the provider API keys and the rest of the configuration encrypted, and behind a biometric once
a key is set.

## Behaviour

- All settings except the public model list caches live in one encrypted store.
- While no API key is set, the store is encrypted but opens without a prompt.
- Setting an API key for any provider switches the store to biometric protection (when a strong
  biometric is enrolled); clearing the last key switches it back.
- In biometric mode the app shows a lock screen and asks for a fingerprint once per app start. The
  store stays open while the process lives, so changing settings needs no further prompt.
- The lock screen shows the app icon, large and centred, with the app name and the **Odblokuj**
  button (with a fingerprint icon) under it. At the bottom, below a thin line, a small centred grey
  note with an info icon says that the configuration is protected by biometrics.
- If the biometric key can no longer be used (for example a new fingerprint was enrolled), the
  store is wiped and the user is told to enter the configuration again.
- The app opts out of Android backup.

## Constraints

- Values are one AES-GCM blob; its data key is wrapped by an Android Keystore key: an AES key in
  plain mode, an RSA key whose private half needs a strong biometric for every use in biometric
  mode.
- Encrypting needs no authentication: the RSA public key is re-created outside the Keystore.

## Out of scope

- Device credential (PIN) fallback, or a configurable lock timeout.
