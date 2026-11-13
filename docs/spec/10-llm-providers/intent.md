# 10 LLM providers

## Goal

Let the user read labels through the vision-model provider they already pay for, instead of only
through OpenRouter.

## Behaviour

Settings offers four providers. One is active at a time; each keeps its own API key, chosen model
and model list, so switching back and forth loses nothing.

| Provider | Preferred models (default in bold) | Key page |
| --- | --- | --- |
| OpenRouter | `anthropic/claude-haiku-5.5`, **`google/gemini-2.5-flash`** | openrouter.ai/settings/keys |
| Anthropic | **`claude-haiku-5-5`**, `claude-sonnet-5-5` | platform.claude.com/settings/keys |
| OpenAI | **`gpt-6-luna`** | platform.openai.com/api-keys |
| Together AI | **`Qwen/Qwen3.5-9B`** | api.together.ai/settings/api-keys |

- The label is read with the active provider's key and model. Without a key for the active provider
  the app sends the user to Settings.
- The history entry records the provider next to the model.
- An OpenRouter key and model saved by an earlier version keep working without re-entry.

## Constraints

- Every provider gets the same prompt, image and JSON schema, and returns the same
  [label fields](../02-label-reading/intent.md).
- A strict JSON schema is requested first; on any HTTP 4xx except 401/402/403/429 the request is sent
  once more without the schema and without a temperature.
- Temperature 0 is sent where the provider accepts it (not to Anthropic, whose current models
  reject sampling parameters).
- A key is only ever sent to its own provider.
- A model's refusal or an empty reply is reported as an error, never parsed as a label.

## Out of scope

- Custom base URLs or self-hosted models, using several providers for one photo, showing prices.
