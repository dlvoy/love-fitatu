# 02 Label reading: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/OpenRouterClient.kt`: `readLabel(dataUrl)` posts to
  `/api/v1/chat/completions` with `PROMPT` and the image, structured first, plain on fallback.
  Errors become `OpenRouterException` carrying the raw reply.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LabelResult.kt`: the serialisable result, `parse` (finds the outermost `{...}`),
  `energyMismatch`, and `toToml` / `fromToml` for the history `[detected]` section.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/ImagePrep.kt`: `scaled` to `UPLOAD_EDGE` (1600) and `toDataUrl`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt` `process`: stores the photo, calls the client, writes `[detected]` or
  `meta.error`, then opens review.

## Tests (`app/src/test/.../BridgeTest.kt`)

- `parsesModelReplyWithCodeFence`
- `nullsStayNullAndMismatchIsFlagged`
