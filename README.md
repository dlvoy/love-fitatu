# Love → Fitatu

An Android app that reads a Love Catering meal label from a photo and enters its nutrition values
into Fitatu and the carbs into the AAPS bolus calculator.

## How it works

1. Take a photo of the label, pick one from the gallery, or share an image to the app.
2. An OpenRouter vision model reads the dish code, the date and the nutrition values.
3. You check and correct the values on the review screen.
4. The app's accessibility service fills Fitatu's quick-add form for the right day and meal,
   and the carbs field of the AAPS bolus calculator. It never confirms a bolus.

Every photo and what was read from it is kept in the in-app history.

## Requirements

- Android 12 (API 31) or newer
- An [OpenRouter](https://openrouter.ai/settings/keys) API key
- Fitatu and, optionally, AAPS installed
- The app's accessibility service turned on (Settings → Accessibility)

## Setup

Open **Settings** in the app and set:

- **OpenRouter API key** and the vision **model** to use.
- **Code letter = meal**: maps the letter in the dish code (e.g. `10284/K/M`) to a Fitatu meal,
  by default `Ś=breakfast, O=dinner, P=snack, K=supper`.
- **AAPS package**, if yours differs from the default.

The configuration and API key can be locked behind biometrics.

## Building

Needs JDK 17 or newer (Android Studio's bundled JDK works).

```sh
./gradlew assembleDebug      # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest
```

A release APK is signed only when the signing environment variables are set; see
[DEPLOYMENT.md](DEPLOYMENT.md).

## Versioning and releases

The version lives in `app/build.gradle.kts` (`val appVersion`); `versionCode` is derived from it.
Pushing a `vX.Y.Z` tag that matches it makes GitHub Actions build, sign and publish the APK as a
GitHub release. Changes are recorded in [CHANGELOG.md](CHANGELOG.md).

## Copyright

© Dominik Dzienia 2026
