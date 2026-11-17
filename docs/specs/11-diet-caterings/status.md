# 11 Diet caterings: status

**State:** done (shipped in 0.1.0)

## Done

- Love Catering and Własny, each with its own code-letter mapping under Zaawansowane.
- Custom label description with fixed output rules appended; Love Catering's prompt unchanged.
- Catering recorded in history.

## Known gaps

- The custom prompt was not tried on a real label of another catering.
- The dish code is still parsed as `number/letter/letter`; a catering with another code format gets
  no automatic meal and the user picks it on review.
- History entries are listed with the active catering's letter mapping, also those captured under
  the other catering (only until the entry is saved, which stores the meal).
