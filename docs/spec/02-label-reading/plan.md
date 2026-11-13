# 02 Label reading: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LabelReader.kt`: `readLabel(dataUrl)` posts the
  catering prompt and the image to the active provider, structured first, plain on fallback.
  Errors become `LlmException` carrying the raw reply and the HTTP code. Wire formats: see
  [10](../10-llm-providers/plan.md). Prompts: see [11](../11-diet-caterings/plan.md).
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LabelResult.kt`: the serialisable result, `parse` (finds the outermost `{...}`),
  `energyMismatch`, and `toToml` / `fromToml` for the history `[detected]` section.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/ImagePrep.kt`: `scaled` to `UPLOAD_EDGE` (1600) and `toDataUrl`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt` `process`: stores the photo, calls the reader, writes `[detected]` or
  `meta.error`, then opens review.

## Tests (`app/src/test/.../BridgeTest.kt`)

- `parsesModelReplyWithCodeFence`
- `nullsStayNullAndMismatchIsFlagged`
