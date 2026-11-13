# 07 Secure configuration: status

**State:** done (shipped in 0.0.1)

## Done

- Encrypted store, automatic switch to biometric mode with an API key, once-per-start unlock.
- Wipe and warning when the biometric key is invalidated.

## Known gaps

- No automated tests (Keystore-bound).
- On a device without a strong biometric the key is encrypted but opens without a prompt.
