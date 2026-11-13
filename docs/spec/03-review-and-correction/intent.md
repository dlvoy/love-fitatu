# 03 Review and correction

## Goal

Let the user check and fix what the model read, and settle the meal and the day, before anything
is sent to Fitatu or AAPS.

## Behaviour

- The review screen shows the photo (tap to zoom and pan), the entry name, kcal, fat, carbs and
  protein as editable fields with a decimal comma, the meal and the date.
- The name defaults to the dish code followed by the dish name.
- **Meal** is preset from the code's middle letter through the *Code letter = meal* mapping from
  Settings (default `Ś=breakfast, O=dinner, P=snack, K=supper`). A letter read without its
  diacritic (`S` for `Ś`) still matches. When no meal is recognised the user is asked to pick one.
- **Date** defaults to today. If the label's weekday falls on a different day of the current
  (Monday-based) week, a dialog asks whether the meal is for today or for the label's day. The date
  can also be stepped a day at a time or picked from a calendar.
- An energy mismatch warning is shown when the macros do not add up to the kcal (see
  [02](../02-label-reading/intent.md)).
- The date row reads `<  day  >`: arrows step a day back or forward, and the day itself, marked
  with a calendar icon, opens the calendar.
- Send actions: **Dodaj do Fitatu** / **Wypełnij w Fitatu** (depending on auto-save) and
  **Kalkulator bolusa AAPS (N g)**, each with the single-colour logo of its app. Each is enabled
  only when its inputs are valid. There is no combined Fitatu-then-AAPS action.
- Under the send actions, below a thin line, a small centred grey note with an info icon says that
  in AAPS the app only types the carbs and the user confirms the bolus themselves.
- The screen is a child screen: its top bar has the title **Edycja etykiety** and a back arrow on
  the left that returns to the main screen.
- Pressing a send action records the values on screen in the history entry's `[saved]` section,
  with where they were sent, even if the automation could not start.
- **Wróć do ostatniej etykiety** on the main screen, a full-width button with a small icon, returns
  to the open review.
- A history entry opened for editing starts from its saved values when it has any.

## Constraints

- Fitatu needs a non-empty name, all four numbers (non-negative) and a meal.
- AAPS needs carbs that round half up to a whole number between 1 and 120 g.

## Out of scope

- Splitting a meal into portions, or logging more than one label in one send.
