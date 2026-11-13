# Deployment

Releases are built by GitHub Actions (`.github/workflows/release.yml`) whenever a tag matching
`vX.Y.Z` is pushed. The workflow:

1. checks that the tag equals `appVersion` in `app/build.gradle.kts`,
2. runs the unit tests and builds a signed release APK,
3. publishes a GitHub release named after the tag, with the matching `CHANGELOG.md` section as
   its notes and `labelbite-vX.Y.Z.apk` attached.

## Cutting a release

Use `/commit-tag-push` (or `/commit as X.Y.Z` followed by a push). It bumps `appVersion`, cuts the
changelog section, tags the commit and pushes. By hand:

1. Set `val appVersion = "X.Y.Z"` in `app/build.gradle.kts`.
2. Move the `[Unreleased]` entries in `CHANGELOG.md` under `## [X.Y.Z] - YYYY-MM-DD`.
3. Commit, `git tag vX.Y.Z`, `git push`, `git push origin vX.Y.Z`.

## Signing key

Keep one keystore for the life of the app: Android refuses to update an installed app with an
APK signed by a different key. Back it up somewhere safe outside the repository, and never
commit it.

Create it once:

```sh
keytool -genkeypair -v -keystore love-fitatu-release.jks -alias love-fitatu \
  -keyalg RSA -keysize 4096 -validity 10000
```

## Environment variables

The release build type reads these. When `SIGNING_KEYSTORE_PATH` is unset the release APK is
built unsigned.

| Variable | Value |
| --- | --- |
| `SIGNING_KEYSTORE_PATH` | Path to the `.jks` file |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias, e.g. `love-fitatu` |
| `SIGNING_KEY_PASSWORD` | Key password (same as the keystore password for a PKCS12 keystore) |

## GitHub secrets

In the repository go to **Settings → Secrets and variables → Actions → New repository secret**
and add:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | The keystore, base64-encoded (see below) |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

The workflow decodes `SIGNING_KEYSTORE_BASE64` to a temporary file and sets
`SIGNING_KEYSTORE_PATH` itself.

Encode the keystore without line breaks:

```sh
# Linux
base64 -w0 love-fitatu-release.jks > keystore.b64
# macOS
base64 -i love-fitatu-release.jks | tr -d '\n' > keystore.b64
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("love-fitatu-release.jks")) | Set-Content -NoNewline keystore.b64
```

Paste the contents of `keystore.b64` as the secret value, then delete `keystore.b64`.

With the GitHub CLI:

```sh
gh secret set SIGNING_KEYSTORE_BASE64 < keystore.b64
gh secret set SIGNING_KEYSTORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS
gh secret set SIGNING_KEY_PASSWORD
```

## Building a signed APK locally

```sh
export SIGNING_KEYSTORE_PATH=/path/to/love-fitatu-release.jks
export SIGNING_KEYSTORE_PASSWORD=...
export SIGNING_KEY_ALIAS=love-fitatu
export SIGNING_KEY_PASSWORD=...
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

```powershell
$env:SIGNING_KEYSTORE_PATH = "C:\path\to\love-fitatu-release.jks"
$env:SIGNING_KEYSTORE_PASSWORD = "..."
$env:SIGNING_KEY_ALIAS = "love-fitatu"
$env:SIGNING_KEY_PASSWORD = "..."
.\gradlew.bat assembleRelease
```
