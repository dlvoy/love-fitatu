# 13 App version check: status

**State:** done (shipped in 0.3.0; state buttons and the check on opening Settings in 0.4.0;
check time on the button instead of a line under it in 0.5.0)

## Done

- Version read from the package manager, minimum 4.17.0 for Fitatu and 4.0.0 for AAPS, result
  kept per app with its time.
- State buttons (green, red, yellow) in Settings, refreshed when Settings opens.
- Check before every run, recorded anew on the first run and after an app update; a failed check
  stops the run.

## Known gaps

- Built and unit-tested, not yet checked by hand on a device with Fitatu and AAPS installed.
- The result is stored with the configuration, so it is lost when the configuration is reset.
