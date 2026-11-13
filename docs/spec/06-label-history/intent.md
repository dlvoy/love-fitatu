# 06 Label history

## Goal

Keep every photographed label and what was read and sent from it, so the user can look back,
reuse an entry or find out what went wrong.

## Behaviour

- Each capture creates an entry: the photo plus a TOML file with three sections:
  - `[meta]`: when it was taken, the photo file, the model, provider and catering used, and `error` if
    reading failed, plus the tokens and cost of the read (see
    [12](../12-usage-and-cost/intent.md));
  - `[detected]`: what the model read;
  - `[saved]`: what the user sent on after edits (name, values, meal, date, `saved_at`, `sent_to`,
    `aaps_carbs_g`).
- The main screen lists entries newest first under two levels of headers, both with a filled
  background: the Monday-based week (**Tydzień 46**, no date range) and, under it, the day (the
  weekday name, **Poniedziałek**, with the date in a smaller font on the right).
- The list scrolls endlessly: the newest entries are read first and older ones are read a page at
  a time as the user nears the end.
- Two views, toggled from the top bar and remembered: a photo grid, or a list with time, meal and
  dish name.
- Tapping an entry opens its details as a full screen (all three sections, numbers with a decimal
  comma) with the photo, which can be zoomed. The top bar has a back arrow on the left of the title
  **Szczegóły etykiety** and a pencil icon on the right that opens the entry in review (see
  [03](../03-review-and-correction/intent.md)).
- Child screens (details, review, settings, the photo viewer, the model picker) are left with a
  back arrow on the left of their title, not with a **Zamknij** or **Gotowe** button.
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
