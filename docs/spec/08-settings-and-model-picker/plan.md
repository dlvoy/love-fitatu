# 08 Settings and model picker: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/SettingsScreen.kt`: `Section` (titled card),
  `ChoiceField` (read-only field with icon plus a `DropdownMenu`), and one composable per card:
  `LlmSettings` (state keyed by provider, the model list `LaunchedEffect` with 1 s debounce and
  refresh counter), `CateringSettings`, `AutomationSettings`, `About`. `AdvancedHeader` is the
  shared **Zaawansowane** toggle. `StateButton` is a full-width button coloured by a `ButtonState`
  (`OK` green, `NEEDED` orange, `BAD` red, `TO_CHECK` yellow); `AccessibilityButton` (also used by
  `MainActivity`) and `VersionCheckRow` are built on it.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/ModelPickerDialog.kt`: `preferredModels`,
  `filterModels`, `canUseTypedId`, the full-screen dialog with a top bar (back arrow, title),
  search and manual id entry.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ocr/ModelCatalog.kt`: `fetchModels` and `parse` per
  provider, `encode` / `decode` for the cache (`id<TAB>name` per line).
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Settings.kt`: per-provider and per-catering
  values (see [10](../10-llm-providers/plan.md), [11](../11-diet-caterings/plan.md)), `autoSave`,
  `aapsPackage`, model list caches (plain preferences, not secret).
- `app/src/main/res/drawable/`: `logo_openrouter`, `logo_anthropic`, `logo_openai`,
  `logo_together` (single-colour vectors from the lobe-icons set, tinted with the text colour),
  section, control and state (`ic_check_circle`, `ic_warning`) icons from Material Icons; `drawable-nodpi/logo_love_catering.png` is the
  Love Catering site icon, drawn untinted.
- `app/build.gradle.kts`: `BuildConfig.GIT_COMMIT` and `BUILD_TIME` for the About section.

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `modelCatalogKeepsOnlyVisionModels`, `modelCatalogPerProvider`, `pickerPreferredAndSearch`,
  `mealLetters`
