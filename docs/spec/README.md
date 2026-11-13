# Specs

Each intent (a feature or a change to the app) has its own folder here, named `NN-intent-name`:
a two-digit number followed by a short kebab-case name. Numbers start at `01` and go up by one; a
new intent takes the next free number. Numbers are never reused, even when an intent is dropped.

Every intent folder holds three files:

| File | What it holds |
| --- | --- |
| `intent.md` | Why the intent exists and what it must do: goal, user-visible behaviour, rules and constraints, what is out of scope. Describes the outcome, not the code. |
| `plan.md` | How it is (or will be) built: the files and components involved, the approach, the steps, and the tests that cover it. |
| `status.md` | Where it stands: state, the release it shipped in, what is done, known gaps and open questions. |

`status.md` starts with a state line, one of `planned`, `in progress`, `done`, `dropped`.

## Intents

| # | Intent | State |
| --- | --- | --- |
| 01 | [Label capture](01-label-capture/intent.md) | done |
| 02 | [Label reading](02-label-reading/intent.md) | done |
| 03 | [Review and correction](03-review-and-correction/intent.md) | done |
| 04 | [Fitatu quick add](04-fitatu-quick-add/intent.md) | done |
| 05 | [AAPS carbs entry](05-aaps-carbs-entry/intent.md) | done |
| 06 | [Label history](06-label-history/intent.md) | done |
| 07 | [Secure configuration](07-secure-configuration/intent.md) | done |
| 08 | [Settings and model picker](08-settings-and-model-picker/intent.md) | done |
| 09 | [Versioning and releases](09-versioning-and-releases/intent.md) | done |
