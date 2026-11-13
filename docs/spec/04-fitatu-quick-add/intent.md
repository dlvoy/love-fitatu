# 04 Fitatu quick add

## Goal

Enter the reviewed meal into Fitatu's **Szybkie dodawanie** (quick add) form, on the right day and
in the right meal, without the user touching Fitatu.

## Behaviour

1. Launch Fitatu and get to the day planner, backing out of the search or quick-add screen if
   Fitatu was left there.
2. Select the chosen day in the week bar. Leave it alone if it is already selected: tapping the
   selected day is not a no-op in Fitatu.
3. Find the meal's add button, scrolling only the meal list.
4. Check that the search screen opened for the expected meal.
5. Open quick add and type the name, kcal, fat, carbs and protein, reading each field back.
6. With **auto-save** on (the default), tap **Zapisz** and wait for the form to close. With it off,
   stop and let the user check and save.

The result, or the reason for stopping, is shown as a toast and on the bridge app's screen.

## Safety rules

- Stop at the first step that does not match expectations; never guess.
- Abort if the planner ends up on a different day after scrolling.
- Abort if the opened meal is not the requested one.
- The whole job times out after 45 s.
- Fitatu must be version 4.17.0 or newer; an older one is refused before it is opened (see
  [13](../13-app-version-check/intent.md)).

## Constraints

- Only days visible in Fitatu's current week bar can be selected.
- Fitatu is a Flutter app: `ACTION_SET_TEXT` changes what accessibility reports but not the form,
  so text is typed through the service's input connection, which needs Android 13 (API 33).
- Numbers are written with a decimal comma, rounded to one decimal; whole numbers have no decimals.

## Out of scope

- Searching Fitatu's product database, editing or deleting existing entries.
