# 06 Label history: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/History.kt`: `LabelRecord` (photo, `taken` from
  the path, `dishName`, `mealLabel`) and `History` (`create`, `update` with a stable section order,
  `list`, `delete` removing empty day folders, `weekStart`, `byWeek`).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Toml.kt`: writer and parser for the small TOML
  subset used (sections, strings, integers, floats, booleans).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/Gallery.kt`: `Gallery` (grid or list by week,
  selection marks, async thumbnails), `RecordDialog`, `detailRows` (usage and cost rows: see [12](../12-usage-and-cost/plan.md)).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ZoomableImage.kt`: `ZoomablePhotoDialog`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/MainActivity.kt`: `selected`, `toggleSelected`,
  `deleteSelected`, `reloadHistory`, the top bar actions; `Settings.galleryGrid` holds the view.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `tomlRoundTrip`, `historyWeeksStartOnMondayNewestFirst`, `detailRowsLabelAndFormat`
