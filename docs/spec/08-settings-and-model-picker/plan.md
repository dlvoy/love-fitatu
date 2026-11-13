# 08 Settings and model picker: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/SettingsScreen.kt`: the fields, the model list
  `LaunchedEffect` (1 s debounce, refresh counter), `About`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/ModelPickerDialog.kt`: `preferredModels`,
  `filterModels`, the dialog with search and manual id entry.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/ModelCatalog.kt`: `fetchVisionModels` from
  `/api/v1/models`, `parse` keeping models with `image` in `input_modalities`, `encode` / `decode`
  for the cache (`id<TAB>name` per line), `PREFERRED`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Settings.kt`: `model` (default
  `google/gemini-2.5-flash`), `lastPickedModel`, `autoSave`, `mealMapping`, `aapsPackage`,
  `cachedModels` (plain preferences, not secret).
- `app/build.gradle.kts`: `BuildConfig.GIT_COMMIT` and `BUILD_TIME` for the About section.

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `modelCatalogKeepsOnlyVisionModels`, `pickerPreferredAndSearch`, `mealLetters`
