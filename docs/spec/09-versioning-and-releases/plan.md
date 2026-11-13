# 09 Versioning and releases: plan

## Components

- `app/build.gradle.kts`: `appVersion`, `appVersionCode`, `GIT_COMMIT` / `BUILD_TIME` build config
  fields, release signing from `SIGNING_KEYSTORE_PATH`, `SIGNING_KEYSTORE_PASSWORD`,
  `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
- `.github/workflows/release.yml`: on `v*.*.*` tags; checks the tag against `appVersion`, decodes
  `SIGNING_KEYSTORE_BASE64`, runs `testDebugUnitTest assembleRelease`, extracts the changelog
  section with `awk`, publishes with `softprops/action-gh-release`, removes the keystore.
- `CHANGELOG.md`, `DEPLOYMENT.md` (cutting a release, keystore, secrets, local signed builds).

## Tests

The workflow runs the debug unit tests before building the release APK.
