# 09 Versioning and releases

## Goal

Ship signed APKs from tags with no manual build steps, and always be able to update an installed
app.

## Behaviour

- The version is set in one place, `val appVersion` in `app/build.gradle.kts`; `versionCode` is
  derived from it.
- Pushing a `vX.Y.Z` tag that matches `appVersion` makes GitHub Actions run the unit tests, build a
  signed release APK and publish a GitHub release named after the tag, with the matching
  `CHANGELOG.md` section as notes and `labelbite-vX.Y.Z.apk` attached.
- A tag that does not match `appVersion`, or a missing signing secret, fails the workflow.
- Changes are recorded in `CHANGELOG.md` (Keep a Changelog, Semantic Versioning).
- The app shows its version, build time and commit in Settings (see
  [08](../08-settings-and-model-picker/intent.md)).

## Constraints

- One signing keystore for the life of the app; it is never committed.
- A release build without the signing environment variables is built unsigned.

## Out of scope

- Play Store publishing, in-app update checks.
