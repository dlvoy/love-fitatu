# 03 Review and correction: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/ReviewScreen.kt`
  - `ReviewData`: editable state built from a `LabelResult`, the meal mapping, today, the photo
    and the history record; overlays `[saved]` values when reopening. `savedToml`,
    `carbsForAaps` and `fitatuJob` derive the outputs.
  - `ReviewScreen`: day-choice dialog, date picker, meal chips, number fields, send buttons.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Meal.kt`: `Meal` (Fitatu keys and Polish labels),
  `DishCode.parse`, `MealMapping.parse` / `mealFor` (diacritic-tolerant), `LabelDate.inCurrentWeek`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/ZoomableImage.kt`: full-screen zoom and pan, with
  `clampPan` keeping the image on screen.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt`: `showReview`, `reopen`, and `run`,
  which starts the jobs and writes `[saved]`.

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `dishCode`, `mealLetters`, `labelWeekdayMapsIntoCurrentWeek`, `aapsCarbsRange`,
  `zoomPanIsClampedToImageEdges`
