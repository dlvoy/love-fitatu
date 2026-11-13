# 10 LLM providers: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LlmProvider.kt`: the `LlmProvider` enum (id,
  label, icon, key page, preferred models, wire `Api`, whether the model list is filtered to vision
  models) and `LlmException`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/LabelReader.kt` (replaces `OpenRouterClient.kt`):
  `readLabel(dataUrl)` with the structured-then-plain fallback. Two wire formats built by pure
  functions so they can be unit-tested: `openAiBody` / `openAiReply` (OpenRouter, OpenAI, Together:
  `/chat/completions`, bearer key, `response_format`) and `anthropicBody` / `anthropicReply`
  (`/v1/messages`, `x-api-key`, `anthropic-version`, base64 image block, `output_config.format`,
  `stop_reason` check).
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/ModelCatalog.kt`: `fetchModels(provider, key)` and
  `parse(provider, body)`: OpenRouter keeps image-input models, Anthropic lists all, OpenAI drops
  non-chat families by id, Together keeps `type == "chat"`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/Settings.kt`: `provider`, and per-provider
  `apiKey`, `model`, `lastPickedModel`, `cachedModels`. OpenRouter keeps the old storage keys
  (`api_key`, `model`, ...); other providers add a `_<id>` suffix. Biometric mode is on while any
  provider has a key.
- `app/src/main/kotlin/pl/lovefitatu/bridge/data/History.kt`: `meta.provider`.
- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt` `process`: builds the `LabelReader`
  from the active provider.
- Settings UI: see [08](../08-settings-and-model-picker/plan.md).

## Tests (`app/src/test/kotlin/pl/lovefitatu/bridge/BridgeTest.kt`)

- `providerDefaultsAndLookup`, `openAiStyleRequestAndReply`, `anthropicRequestAndReply`,
  `modelCatalogPerProvider`
