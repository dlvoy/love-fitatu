# 12 Usage and cost: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LlmUsage.kt`: `LlmUsage` (input and output tokens,
  `costUsd`, `costEstimated`, `toToml`) and `ModelPrices`, the built-in price table with
  `estimate(provider, model, inputTokens, outputTokens)`. A model id matches a table entry when,
  lower-cased, it equals the entry or continues it after a `-` (a date or variant suffix); the
  longest entry wins.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LabelReader.kt`:
  - `LlmWire.usage(provider, model, root)` reads `usage` from the reply: `prompt_tokens` /
    `completion_tokens` (OpenAI style; `cost` is taken from OpenRouter only), or `input_tokens`
    plus the two cache token counts / `output_tokens` (Anthropic). Without a reported cost it asks
    `ModelPrices`.
  - `readLabel` returns `LabelReading(result, usage)`.
  - `LlmException.usage` carries the usage of a reply that was billed but unusable.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt` `process`: merges the usage into
  `[meta]` on success and, with the error, on failure.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ui/Gallery.kt` `detailRows`: labels for the new keys,
  `cost_usd` formatted by `formatCost` (six decimals, trailing zeros dropped, `≈` and
  `(szacunek)` for an estimate), `cost_estimated` hidden.

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `usageFromProviderReplies`, `costEstimatesFromPriceTable`, `detailRowsShowUsageAndCost`
