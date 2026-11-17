# 06 Label history: status

**State:** done (shipped in 0.0.1)

## Done

- TOML plus photo per entry, failed reads kept with their error.
- Grid and list views by week, details dialog, reopen for editing, multi-select delete.

- UX pass (unreleased): week and day headers with a filled background, endless scroll with paged
  reading, details as a full screen with a back arrow and a pencil action. Built and unit-tested;
  not yet checked by hand on a device.

- Shared storage (1.1.0): history in `Documents/LabelBite` with all-files access, the app's
  own folder without it, and a move of existing entries once access is granted. Built and
  unit-tested (`moveEntries`); the permission screen and the move were not yet checked by hand on
  a device.

## Known gaps

- If all-files access is taken away again, the entries in `Documents/LabelBite` stay there but
  are no longer shown; the app goes back to its own folder until access returns.
- The folder is on the phone's built-in shared storage; a removable SD card is not used.
- Label photos in `Documents/LabelBite` can show up in gallery apps that list every image folder.

- History is never pruned; it grows until the user deletes entries.
- A reload still lists every day folder and re-reads all entries shown so far, so it slows down
  once the user has scrolled far back in a large history.
