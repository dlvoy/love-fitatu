# 06 Label history: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/History.kt`: `LabelRecord` (photo, `taken` from
  the path, `dishName`, `mealLabel`) and `History` (`create`, `update` with a stable section order,
  `files` listing every entry's TOML newest first without reading it, `load` reading a slice of
  them, `takenOf`, `delete` removing empty day folders, `weekStart`, `byWeek`, `byDay`).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Toml.kt`: writer and parser for the small TOML
  subset used (sections, strings, integers, floats, booleans).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/Gallery.kt`: `Gallery` (grid or list by week and
  day with `WeekHeader` and `DayHeader`, `weekTitle`, `dayTitle`, selection marks, async
  thumbnails; a trailing `LoadMore` item calls `onLoadMore` when it scrolls into view),
  `RecordDetails` (the body of the details screen), `detailRows` (usage and cost rows: see
  [12](../12-usage-and-cost/plan.md)).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ZoomableImage.kt`: `ZoomablePhotoDialog`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/MainActivity.kt`: `selected`, `toggleSelected`,
  `deleteSelected`, the top bar (back arrow on every child screen, the pencil on `Screen.DETAILS`);
  `Settings.galleryGrid` holds the view. Paging: `historyFiles` is the full file list, `records`
  the part read so far; `reloadHistory` re-reads as many entries as were shown (at least
  `HISTORY_PAGE`, 60) and `loadMoreHistory` appends the next page, both under `historyLock`.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `tomlRoundTrip`, `historyWeeksStartOnMondayNewestFirst`, `historyDaysWithinWeekNewestFirst`,
  `detailRowsLabelAndFormat`
