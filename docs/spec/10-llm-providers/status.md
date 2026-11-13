# 10 LLM providers: status

**State:** done (shipped in 0.1.0)

## Done

- Four providers with separate keys, models, model lists and preferred models.
- OpenAI-style and Anthropic wire formats, structured request with plain fallback, refusal handling.
- Provider recorded in history; an existing OpenRouter configuration carries over.

## Known gaps

- Request and reply shapes are unit-tested, but no provider was called with a real key during
  development: Anthropic, OpenAI and Together AI still need a check on a device.
- `gpt-6-luna` and `Qwen/Qwen3.5-9B` are used as given; whether they accept image input was not
  verified.
- Anthropic, OpenAI and Together AI do not mark vision models in their lists, so the picker can
  offer a model that cannot read the photo; the read then fails with the provider's error.
