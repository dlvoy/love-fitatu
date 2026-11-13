# 05 AAPS carbs entry

## Goal

Put the meal's carbs into the AAPS bolus calculator so the user only has to check and confirm.
The app never confirms or delivers a bolus.

## Behaviour

1. Launch AAPS (package from Settings, default `info.nightscout.androidaps`).
2. On the main screen only, click the **Kalkulator bolusa** button. No other navigation in AAPS.
3. Recognise the calculator screen: its title, a carbs label and exactly one text field.
4. Set the carbs field to the whole-gram value and read it back.
5. Stop and tell the user to check and confirm themselves.

**Fitatu, potem AAPS** runs the Fitatu job first and AAPS only if Fitatu succeeded. When the AAPS
job fails, the message says how many grams to type by hand.

## Safety rules

- The only actions inside AAPS are one click on the calculator button and one set-text on the carbs
  field.
- Never click a node whose subtree has a confirm-like label (`OK`, `Potwierdź`, `Zatwierdź`,
  `Confirm`, `Podaj`, `Deliver`, `Wyślij`, `Send`), nor any node not labelled as the calculator.
- Carbs must be 1–120 g; anything else is refused before AAPS is opened.

## Out of scope

- Entering insulin, changing calculator options, or anything past typing the carbs.
