# CLAUDE.md

Love → Fitatu: an Android app (Kotlin, Jetpack Compose) that reads a Love Catering meal label with
an OpenRouter vision model and fills Fitatu and the AAPS bolus calculator through an accessibility
service. See [README.md](README.md) for the overview and build commands, and
[DEPLOYMENT.md](DEPLOYMENT.md) for releases.

## Specs

Feature specs live in [docs/spec](docs/spec/README.md). Use them when planning:

- Before planning a change, read the intents it touches (`intent.md`, `plan.md`, `status.md`).
  Their rules, especially the Fitatu and AAPS safety rules, stay in force unless the user changes
  them.
- A new feature gets a new intent folder `NN-intent-name`, taking the next free two-digit number
  (`01`, `02`, ...), with `intent.md` (what and why), `plan.md` (how) and `status.md` (state, done,
  known gaps). Write `intent.md` and `plan.md` while planning, before writing code.
- A change to an existing feature updates that intent's files instead.
- Keep `status.md` current as work progresses, and add new intents to the table in
  `docs/spec/README.md`.
