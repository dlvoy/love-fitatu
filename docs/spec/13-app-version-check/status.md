# 13 App version check: status

**State:** done (shipped in 0.3.0)

## Done

- Version read from the package manager, minimum 4.17.0 for Fitatu and 4.0.0 for AAPS, result
  kept per app with its time.
- Check buttons with results in Settings.
- Check before every run, recorded anew on the first run and after an app update; a failed check
  stops the run.

## Known gaps

- Built and unit-tested, not yet checked by hand on a device with Fitatu and AAPS installed.
- The result is stored with the configuration, so it is lost when the configuration is reset.
- The result shown in Settings is the stored one; after an app update it is refreshed by the next
  run or a tap on the button, not when Settings opens.
