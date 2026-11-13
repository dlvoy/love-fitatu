# 06 Label history

## Goal

Keep every photographed label and what was read and sent from it, so the user can look back,
reuse an entry or find out what went wrong.

## Behaviour

- Each capture creates an entry: the photo plus a TOML file with three sections:
  - `[meta]`: when it was taken, the photo file, the model used, and `error` if reading failed;
  - `[detected]`: what the model read;
  - `[saved]`: what the user sent on after edits (name, values, meal, date, `saved_at`, `sent_to`,
    `aaps_carbs_g`).
- The main screen lists entries newest first, in Monday-based weeks with a week header.
- Two views, toggled from the top bar and remembered: a photo grid, or a list with date, meal and
  dish name.
- Tapping an entry shows its details (all three sections, numbers with a decimal comma) and the
  photo, which can be zoomed. **Otwórz do edycji** opens it in review (see
  [03](../03-review-and-correction/intent.md)).
- Long press starts selection; tapping then toggles entries. The top bar shows the count, a delete
  action and **Anuluj**. Delete asks for confirmation and removes the photos and files.
- Deleting the entry behind the open review detaches it, so a later send does not recreate the file.

## Constraints

- Stored under the app's external files folder (`.../files/labels/YYYY-MM-DD/HH-mm-ss.jpg` and
  `.toml`), readable over USB or adb; internal storage if external is unavailable.
- The entry's time comes from the folder and file names, so a damaged TOML still sorts correctly.
- Two captures in the same second get a `-2`, `-3`, ... suffix.

## Out of scope

- Sync or backup to a server, search, export.
