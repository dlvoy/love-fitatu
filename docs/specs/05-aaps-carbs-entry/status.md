# 05 AAPS carbs entry: status

**State:** done (shipped in 0.0.1)

## Done

- Calculator opened from the main screen, carbs typed and verified, guard against confirm controls.
- Chained Fitatu → AAPS run.

## Known gaps

- If AAPS is not on its main screen the job gives up rather than navigating there.
- A custom AAPS package that is not in the manifest `<queries>` may be invisible to the app
  (package visibility, Android 11+).
