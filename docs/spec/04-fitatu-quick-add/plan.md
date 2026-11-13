# 04 Fitatu quick add: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/automation/Jobs.kt`: `AutomationJob.Fitatu`,
  `AutomationStatus` (last message as a `StateFlow`), `Format.fitatuNumber` / `sameNumber`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/automation/BridgeAccessibilityService.kt`
  - `start(jobs)` runs jobs in order on the main dispatcher, one run at a time, 45 s per job,
    reporting through a toast and `AutomationStatus`.
  - `runFitatu` implements the steps against Fitatu's Flutter view ids
    (`planner_page_planner_*`, `search_catalog_page_*`, `quick_food_form_page_*`).
  - `setField` taps the inner `EditText`, waits for the input connection, clears with
    `deleteSurroundingText`, types with `commitText` and verifies with `getSurroundingText`.
  - `mealList` picks the scrollable ancestor of a meal section, so the week bar and the day pager
    are never scrolled. `click` refuses nodes outside the Fitatu package.
- `app/src/main/res/xml/accessibility_service_config.xml`, the manifest `<service>` entry and
  `<queries>` for `com.fitatu.tracker`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/MainActivity.kt` `run`: shows an error when the service
  is off or a run is already active.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `fitatuNumbers`. The UI driving itself is checked by hand against the installed Fitatu.
