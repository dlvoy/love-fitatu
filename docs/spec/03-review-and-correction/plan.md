# 03 Review and correction: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ReviewScreen.kt`
  - `ReviewData`: editable state built from a `LabelResult`, the meal mapping, today, the photo
    and the history record; overlays `[saved]` values when reopening. `savedToml`,
    `carbsForAaps` and `fitatuJob` derive the outputs.
  - `ReviewScreen`: day-choice dialog, date picker, meal chips, number fields, the date row
    (`ic_chevron_left`, `ic_calendar`, `ic_chevron_right`), the two send buttons with
    `logo_fitatu` and `logo_aaps`, and the muted AAPS note.
- `app/src/main/res/drawable/logo_fitatu.xml`, `logo_aaps.xml`: single-colour vectors taken from
  the apps themselves (the monochrome layer of the Fitatu launcher icon, cropped to the glyph, and
  the AAPS notification icon), tinted with the button's content colour like the provider logos.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Meal.kt`: `Meal` (Fitatu keys and Polish labels),
  `DishCode.parse`, `MealMapping.parse` / `mealFor` (diacritic-tolerant), `LabelDate.inCurrentWeek`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ZoomableImage.kt`: full-screen zoom and pan, with
  `clampPan` keeping the image on screen; a back arrow and the title **Zdjęcie etykiety** on top.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/MainActivity.kt`: `showReview`, `reopen`, and `run`,
  which starts the jobs and writes `[saved]`.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `dishCode`, `mealLetters`, `labelWeekdayMapsIntoCurrentWeek`, `aapsCarbsRange`,
  `zoomPanIsClampedToImageEdges`
