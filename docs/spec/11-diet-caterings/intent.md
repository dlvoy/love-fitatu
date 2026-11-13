# 11 Diet caterings

## Goal

Make the label reading catering-specific, so the app is not tied to Love Catering labels.

## Behaviour

Settings offers two caterings; one is active.

- **Love Catering**: the built-in prompt written for its labels. The prompt is not shown. An
  advanced section holds **Litera kodu = posiłek**, the mapping from the middle letter of the dish
  code to a Fitatu meal.
- **Własny (Custom)**: the user describes their label in their own words (which caption precedes
  which value, what to ignore). That description is editable; the app appends the fixed output
  rules itself. Custom has its own code-letter mapping in its advanced section.

The active catering decides the prompt sent with the photo and the letter mapping used on the
review screen and in the history list. The history entry records the catering.

## Constraints

- Whatever the catering, the model must return the same
  [label fields](../02-label-reading/intent.md); the output rules (keys, nulls, decimal point,
  never guess) are fixed and cannot be edited away.
- Love Catering's prompt stays as it shipped in 0.0.1.
- The Fitatu and AAPS safety rules do not depend on the catering.

## Out of scope

- A catalogue of other caterings, per-catering dish-code formats (the code is still parsed as
  `number/letter/letter`; otherwise the user picks the meal on review), sharing custom prompts.
