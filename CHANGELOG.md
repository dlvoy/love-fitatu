# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Versions are tagged `vX.Y.Z` and match the app's `versionName`.

## [Unreleased]

## [0.0.1] - 2026-11-13

### Added

- Read a Love Catering meal label from a photo or the gallery with an OpenRouter vision model.
- Review and correct the read values before sending them on.
- Fill Fitatu's quick-add form and the carbs field of the AAPS bolus calculator through an accessibility service; the bolus is never confirmed automatically.
- Keep a history of photographed labels, shown as a grid or a list, with multi-select delete.
- Protect the configuration and API key with biometrics.
- Show the app version, build time and commit in an About section at the bottom of Settings.
