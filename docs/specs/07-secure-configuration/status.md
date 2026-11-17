# 07 Secure configuration: status

**State:** done (shipped in 0.0.1)

## Done

- Encrypted store, automatic switch to biometric mode with an API key, once-per-start unlock.
- Wipe and warning when the biometric key is invalidated.
- Lock screen with the app icon, a fingerprint icon on the button and the note at the bottom
  (1.0.0). In landscape the logo is smaller and the screen scrolls.

## Known gaps

- No automated tests (Keystore-bound).
- On a device without a strong biometric the key is encrypted but opens without a prompt.
