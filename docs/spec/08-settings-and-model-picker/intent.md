# 08 Settings and model picker

## Goal

One screen for everything the app needs to work, grouped so each part is easy to find, with an easy
way to choose a vision model.

## Behaviour

Settings is a column of titled cards, each with an icon:

- **Rozpoznawanie etykiety**
  - **Dostawca modelu**: a field showing the active provider's logo and name; tapping it opens a
    menu of all providers, each with its logo (see [10](../10-llm-providers/intent.md)).
  - **Klucz API** of that provider, with a button that opens the provider's key page.
  - **Model**: opens the model picker (below), with a refresh button for the model list.
  - Changing the provider swaps the key, model and list to that provider's own.
- **Catering dietetyczny** (see [11](../11-diet-caterings/intent.md))
  - **Catering**: the same kind of field with logo; Love Catering also gets a button to its site.
  - **Opis etykiety dla modelu**: only for Własny. Hidden for Love Catering.
  - **Zaawansowane**, collapsed by default: **Litera kodu = posiłek**, the code letter to meal
    mapping of the chosen catering, with the parsed result shown under the field.
- **Fitatu i AAPS**
  - **Automatycznie naciśnij Zapisz w Fitatu** (auto-save, on by default; see
    [04](../04-fitatu-quick-add/intent.md)).
  - The accessibility service button: one button that shows the state and opens the system
    accessibility settings. Orange with a warning icon and **Włącz usługę ułatwień dostępu** while
    the service is off, green with a check mark and **Usługa ułatwień dostępu włączona** while it
    is on. The main screen shows the orange button while the service is off.
  - A version state button for Fitatu and one for AAPS, in the same style: green, red or yellow
    (see [13](../13-app-version-check/intent.md)).
  - **Zaawansowane**, collapsed by default: **Pakiet AAPS**, the AAPS package name.
- **O aplikacji**: copyright, version name and code, build time and git commit.

Model picker:

- Lists the active provider's models, loaded once a key is present (after a short typing pause) and
  on every refresh tap. The list is cached per provider between runs.
- OpenRouter's list holds only models that accept image input. The other providers do not say which
  models read images, so their lists are wider and the picker says so.
- A preferred section on top: the provider's preferred models and the last model the user picked.
- Search by id or name. A typed id without spaces that is not in the list can be used as is.
- Shows a notice while loading, without a key, or when the list could not be fetched.

## Out of scope

- Per-model settings (temperature), or showing model prices.
