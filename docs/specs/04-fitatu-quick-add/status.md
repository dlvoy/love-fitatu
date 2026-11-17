# 04 Fitatu quick add: status

**State:** done (shipped in 0.0.1)

## Done

- Day selection, meal lookup with safe scrolling, meal check, typed and verified fields, optional
  auto-save.
- Meal lookup survives a meal with many products filling the screen (no section header visible)
  and starts from the top of the list, so meals above the last scroll position are found.
  Checked on the device with Fitatu 2026-11-17.

## Known gaps

- Typing needs Android 13, but `minSdk` is 31 and the README says Android 12. On Android 12 the job
  stops with a clear message.
- Tied to Fitatu's current view ids; a Fitatu update that renames them breaks the job.
- Days outside Fitatu's current week bar cannot be reached.
