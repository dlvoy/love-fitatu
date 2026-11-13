# 06 Label history: status

**State:** done (shipped in 0.0.1)

## Done

- TOML plus photo per entry, failed reads kept with their error.
- Grid and list views by week, details dialog, reopen for editing, multi-select delete.

## Known gaps

- History is never pruned; it grows until the user deletes entries.
- `History.list` reads every TOML on each reload, which will slow down with a large history.
