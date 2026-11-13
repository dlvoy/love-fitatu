# 06 Label history: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/data/History.kt`: `LabelRecord` (photo, `taken` from
  the path, `dishName`, `mealLabel`) and `History` (`create`, `update` with a stable section order,
  `list`, `delete` removing empty day folders, `weekStart`, `byWeek`).
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Toml.kt`: writer and parser for the small TOML
  subset used (sections, strings, integers, floats, booleans).
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/Gallery.kt`: `Gallery` (grid or list by week,
  selection marks, async thumbnails), `RecordDialog`, `detailRows`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/ZoomableImage.kt`: `ZoomablePhotoDialog`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt`: `selected`, `toggleSelected`,
  `deleteSelected`, `reloadHistory`, the top bar actions; `Settings.galleryGrid` holds the view.

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `tomlRoundTrip`, `historyWeeksStartOnMondayNewestFirst`, `detailRowsLabelAndFormat`
