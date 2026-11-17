# 12 Usage and cost: status

**State:** done (shipped in 0.2.0)

## Done

- Tokens read from OpenAI-style and Anthropic replies; cost taken from OpenRouter, estimated from
  the built-in price table for Anthropic, OpenAI and Together AI.
- Usage merged into the history `[meta]`, also for a billed reply the app could not use.
- Details dialog shows input tokens, output tokens and the cost, with estimates marked.

## Known gaps

- Covered by unit tests on sample replies only; no provider was called with a real key, so the
  numbers still need a check on a device against the provider's own usage page.
- The price table is a snapshot from 2026-11-13 and goes stale when providers change prices or
  release models; it is updated by hand in `LlmUsage.kt`.
- Together AI is priced for `Qwen/Qwen3.5-9B` only; other Together models, and any Anthropic or
  OpenAI model outside the table, show tokens without a cost.
- Estimates use the base list price: the higher rate some models charge for very long prompts and
  any cache discount are ignored, and Anthropic cache tokens are priced as plain input.
- A structured request rejected with HTTP 4xx and retried plain records the retry only (a rejected
  request is normally not billed).
