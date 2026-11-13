# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Versions are tagged `vX.Y.Z` and match the app's `versionName`.

## [Unreleased]

## [1.0.0] - 2026-11-13

### Added

- LabelBite has its own icon: a friendly monster biting the corner off a label. It is the launcher icon, and it shows on the splash screen, on the lock screen and next to the title on the main screen.

### Changed

- The app is salmon instead of purple: buttons, accents, highlights and backgrounds.
- Day headers in the history are light grey, so they read as separators under the coloured week header.
- The lock screen shows the icon large in the centre, the Odblokuj button has a fingerprint icon, and the note about biometric protection moved to the bottom as a small grey line.

## [0.7.0] - 2026-11-13

### Added

- The history has day headers under each week, with the weekday name and its date.
- The history scrolls endlessly: older labels are read as you scroll, so a long history opens faster.
- The take photo and gallery buttons have large icons, and the Fitatu and AAPS buttons carry the logo of their app.

### Changed

- Settings, label details, label editing, the photo viewer and the model picker are left with a back arrow next to the title instead of a Gotowe, Zamknij or Anuluj button.
- Label details open as a full screen; a pencil icon in the top bar opens the label for editing.
- The model picker is a full screen.
- Settings open from a cog icon in the top bar.
- Week headers show only the week number and have a filled background; list rows show the time instead of the full date.
- The return to the last label action is a full-width button with an icon.
- The day in label editing has a calendar icon, and the note about AAPS is smaller and grey.

### Removed

- The combined "Fitatu, potem AAPS" button. Send to Fitatu and to the AAPS bolus calculator separately.

## [0.6.0] - 2026-11-13

### Changed

- The app is now called LabelBite, and its Android package is `pl.cukrzycowy.labelbite` (was `pl.lovefitatu.bridge`). Android treats it as a new app: it installs next to the old one, starts with empty settings and history, and its accessibility service has to be turned on again.
- The release APK is named `labelbite-vX.Y.Z.apk`.

## [0.5.0] - 2026-11-13

### Changed

- The Fitatu and AAPS version buttons in Settings no longer have a status line under them: the green button carries the time of the check, the red one the required version.

## [0.4.0] - 2026-11-13

### Changed

- The Fitatu and AAPS version checks in Settings are state buttons: green with the version when it is fine, red when the app is missing or too old, yellow while it still has to be checked.
- Opening Settings checks the installed Fitatu and AAPS versions.

## [0.3.0] - 2026-11-13

### Added

- Settings can check the installed versions of Fitatu and AAPS and shows the last result with its time. The check also runs before the app steers Fitatu or AAPS, and is recorded again after either is updated.

### Changed

- Fitatu older than 4.17.0 and AAPS older than 4.0.0 are refused: the run stops before the app is opened.
- The accessibility service state and the button to its settings are now one button: orange while the service is off, green with a check mark while it is on.
- The AAPS package setting moved under an advanced section.

## [0.2.0] - 2026-11-13

### Added

- Each history entry records the tokens used by the label read and its cost in USD, shown in the details dialog. OpenRouter reports the cost; for Anthropic, OpenAI and Together AI it is estimated from list prices and marked as an estimate.

## [0.1.0] - 2026-11-13

### Added

- Choose the model provider in Settings: OpenRouter, Anthropic, OpenAI or Together AI. Each keeps its own API key, model and model list.
- Choose the catering in Settings: Love Catering, or a custom one whose label you describe for the model in your own words.
- Each history entry records the provider and catering used.

### Changed

- Settings is grouped into cards with icons; the provider and the catering are picked from a menu with logos.
- The code letter to meal mapping moved under an advanced section and is kept per catering.
- The model picker accepts any typed model id without spaces, not only ids containing a slash.
- A model refusal or an empty reply is reported as an error instead of being read as a label.

## [0.0.1] - 2026-11-13

### Added

- Read a Love Catering meal label from a photo or the gallery with an OpenRouter vision model.
- Review and correct the read values before sending them on.
- Fill Fitatu's quick-add form and the carbs field of the AAPS bolus calculator through an accessibility service; the bolus is never confirmed automatically.
- Keep a history of photographed labels, shown as a grid or a list, with multi-select delete.
- Protect the configuration and API key with biometrics.
- Show the app version, build time and commit in an About section at the bottom of Settings.
