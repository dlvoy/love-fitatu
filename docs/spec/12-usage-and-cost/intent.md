# 12 Usage and cost

## Goal

Show what each label read cost, so the user can compare models and providers and keep an eye on
spending.

## Behaviour

- After every label read the history entry records, in `[meta]`:
  - `input_tokens` and `output_tokens` as the provider reported them for the call;
  - `cost_usd`, the cost of the call in US dollars;
  - `cost_estimated`: `false` when the provider reported the cost itself, `true` when the app
    worked it out from a price list.
- OpenRouter reports the cost with the reply and that number is stored as is. Anthropic, OpenAI
  and Together AI report only tokens; the cost is tokens times the model's list price from a table
  built into the app.
- A model missing from the table gets tokens but no cost. A reply without usage data gets neither.
- A read that the provider answered but the app could not use (unparsable reply, refusal, empty
  reply) was still billed, so its tokens and cost are recorded next to the error.
- The details dialog shows, under **Informacje**: **Tokeny wejściowe**, **Tokeny wyjściowe** and
  **Koszt odczytu**. A reported cost reads `0,000345 USD`; an estimate reads
  `≈ 0,000345 USD (szacunek)`.
- Entries from before this feature simply have no such rows.

## Constraints

- The numbers come from the reply to the label request; no extra request is made, and nothing is
  sent to a provider other than the active one.
- The price table holds list prices in USD per million tokens, input and output, taken from the
  providers' public price lists on the date noted in the code. Volume tiers, batch and cache
  discounts are ignored: a label read is one small uncached request.
- An estimate is always marked as one, in the file and on screen.
- Recording usage never fails a read: missing or malformed usage data is skipped.

## Out of scope

- Totals per week or month, budgets or warnings, currency conversion, refreshing the price table
  over the network.
