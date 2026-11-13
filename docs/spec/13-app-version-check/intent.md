# 13 App version check

## Goal

Know which versions of Fitatu and AAPS are installed before the app steers them, and refuse to
steer a version older than the one the automation was written for.

## Behaviour

- The check reads the installed version of an app from the system package manager. It opens
  neither app and performs no action inside them.
- **Fitatu** passes when it is installed in version **4.17.0 or newer** (the automation was
  written against 4.17.x).
- **AAPS** (the package from Settings) passes when it is installed in version **4.0.0 or newer**.
- Only the leading numbers of a version count, so `4.0.0-dev` is 4.0.0.
- Settings, card **Fitatu i AAPS**, has one state button per app. It is green with a check mark
  and the version when the check passed, red with a warning when the app is missing or too old,
  and yellow with **Sprawdź wersję …** while the app still has to be checked. A line under it
  gives the time of the check and, when it failed, the reason. Tapping the button checks again.
- Opening Settings looks at both apps the same way a run does (next point), so the buttons show
  what is installed now. The yellow state is left for an AAPS package that was just edited.
- The last result of each app is kept: package, version, the time the system last installed or
  updated the app, passed or not, and the time of the check.
- Before every run the installed version of each app the run steers is looked at. The result on
  record is replaced, with a new check time, when it no longer describes what is installed: the
  app was never checked, the package is a different one, the app was updated since, or the verdict
  changed (for example after an update of this app raised a minimum). Otherwise the record and
  its time stay as they are.
- A failed check stops the whole run before anything is opened and the reason is shown.

## Rules

- The check never navigates inside Fitatu or AAPS. The AAPS rule of
  [05](../05-aaps-carbs-entry/intent.md) (one click, one set-text, nothing else) stays as it is,
  which is why the version is not read from the apps' own "about" screens.

## Out of scope

- An upper limit: a Fitatu newer than 4.17.x is steered, and the step checks of
  [04](../04-fitatu-quick-add/intent.md) stop the job if its screens changed.
