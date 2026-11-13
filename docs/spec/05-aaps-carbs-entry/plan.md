# 05 AAPS carbs entry: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/automation/Jobs.kt`: `AutomationJob.Aaps` and
  `AapsGuard` (calculator and carbs labels in Polish and English, forbidden label parts,
  `MAX_CARBS = 120`, `validCarbs`).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/automation/BridgeAccessibilityService.kt`: `runAaps`
  and `clickInAaps`, the only click path allowed inside AAPS. The Fitatu `click` helper asserts the
  Fitatu package, so it cannot be used in AAPS by mistake.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ReviewScreen.kt`: `carbsForAaps` rounds half up and
  applies the range.
- Manifest `<queries>` for `info.nightscout.androidaps`.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `aapsGuardRefusesConfirmControls`, `aapsCarbsRange`
