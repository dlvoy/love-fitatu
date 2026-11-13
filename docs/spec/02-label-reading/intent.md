# 02 Label reading

## Goal

Read the values needed for Fitatu and AAPS off the label photo with an OpenRouter vision model,
without guessing anything the model cannot read.

## Behaviour

The model returns, for the whole meal:

| Field | Meaning |
| --- | --- |
| `day` | Weekday printed on the label, in Polish (e.g. `Czwartek`). Printed dates (production, best-before) are ignored. |
| `code` | Dish code `number/letter/letter`, e.g. `10284/K/M`. The middle letter identifies the meal. |
| `dish_name` | Dish title in Polish, as printed. |
| `kcal`, `fat_g`, `carbs_g`, `protein_g` | Main nutrition values as printed. Not sugars, saturated fat or fibre. |

- Any field the model cannot read with confidence is `null`; the user fills it in on review.
- If the macros do not add up to the stated energy (4/4/9 kcal per gram) within 15 %, the review
  screen shows a warning.
- On failure the user sees the error message and the start of the raw model reply, and the error is
  recorded in the history entry.

## Constraints

- The image sent to the model is scaled to a 1600 px long edge, JPEG quality 85, as a data URL.
- Temperature 0. A strict JSON schema is requested; models that reject it (any HTTP 4xx except
  401/402) are asked once more without the schema.
- The reply parser tolerates code fences or prose around the JSON object.
- The model used is the one chosen in Settings (default `google/gemini-2.5-flash`).

## Out of scope

- On-device OCR, or reading more than one label per photo.
