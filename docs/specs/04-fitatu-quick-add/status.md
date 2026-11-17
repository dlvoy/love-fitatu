# 04 Fitatu quick add: status

**State:** done (shipped in 0.0.1)

## Done

- Day selection, meal lookup with safe scrolling, meal check, typed and verified fields, optional
  auto-save.

## Known gaps

- Typing needs Android 13, but `minSdk` is 31 and the README says Android 12. On Android 12 the job
  stops with a clear message.
- Tied to Fitatu's current view ids; a Fitatu update that renames them breaks the job.
- Days outside Fitatu's current week bar cannot be reached.
