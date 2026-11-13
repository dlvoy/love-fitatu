# 08 Settings and model picker

## Goal

One screen for everything the app needs to work, with an easy way to choose a vision model.

## Behaviour

Settings holds:

- **Klucz API OpenRouter**, with a button that opens OpenRouter's key page.
- **Model**: opens the model picker (below), with a refresh button for the model list.
- **Automatycznie naciśnij Zapisz w Fitatu** (auto-save, on by default; see
  [04](../04-fitatu-quick-add/intent.md)).
- **Litera kodu = posiłek**: the code letter to meal mapping, with the parsed result shown under
  the field.
- **Pakiet AAPS**: the AAPS package name.
- Accessibility service state and a button to the system accessibility settings. The main screen
  also shows that button while the service is off.
- **O aplikacji**: copyright, version name and code, build time and git commit.

Model picker:

- Lists OpenRouter models that accept image input, loaded once an API key is present (after a short
  typing pause) and on every refresh tap. The list is cached between runs.
- A preferred section on top: `anthropic/claude-haiku-5.5`, `google/gemini-2.5-flash` and the last
  model the user picked.
- Search by id or name. A typed id containing `/` that is not in the list can be used as is.
- Shows a notice while loading, without a key, or when the list could not be fetched.

## Out of scope

- Per-model settings (temperature, prompt), or showing model prices.
