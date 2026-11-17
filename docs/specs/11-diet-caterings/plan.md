# 11 Diet caterings: plan

## Components

- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Catering.kt`: the `Catering` enum (id, label,
  icon, site, default letter mapping) and `LabelPrompt`: `LOVE` (the 0.0.1 prompt, verbatim),
  `CUSTOM_DEFAULT` (starting description for a custom label), `OUTPUT_RULES`, and
  `forCatering(catering, customDescription)`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/Settings.kt`: `catering`, `customPrompt`,
  per-catering `mealMapping` (Love keeps the old `meal_mapping` key), and `labelPrompt`.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ocr/LabelReader.kt`: takes the prompt as a parameter.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/data/History.kt`: `meta.catering`.
- Settings UI: see [08](../08-settings-and-model-picker/plan.md).

## Tests (`app/src/test/kotlin/pl/cukrzycowy/labelbite/BridgeTest.kt`)

- `cateringPrompts`
