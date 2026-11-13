# 06 Label history: status

**State:** done (shipped in 0.0.1)

## Done

- TOML plus photo per entry, failed reads kept with their error.
- Grid and list views by week, details dialog, reopen for editing, multi-select delete.

- UX pass (unreleased): week and day headers with a filled background, endless scroll with paged
  reading, details as a full screen with a back arrow and a pencil action. Built and unit-tested;
  not yet checked by hand on a device.

## Known gaps

- History is never pruned; it grows until the user deletes entries.
- A reload still lists every day folder and re-reads all entries shown so far, so it slows down
  once the user has scrolled far back in a large history.
