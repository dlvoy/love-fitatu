# 08 Settings and model picker: status

**State:** done (shipped in 0.0.1; sectioned layout, provider and catering choice in 0.1.0;
accessibility state button, version check buttons and advanced AAPS package in 0.3.0; version
state buttons in 0.4.0)

## Done

- All settings fields, accessibility state, About section.
- Model list with cache, preferred section, search and manual entry.
- Settings grouped into cards with icons; provider and catering chosen from a menu with logos.

- UX pass (unreleased): cog icon instead of the **Ustawienia** label, back arrow instead of
  **Gotowe**, model picker as a full screen with a back arrow instead of **Anuluj**. Not yet
  checked by hand on a device.

- **Uprawnienia** card (1.1.0): the accessibility button moved there from **Fitatu i AAPS**,
  joined by the file access state button and a note on where the history is saved. Built; not yet
  checked by hand on a device.

## Known gaps

- The preferred model ids are hard-coded per provider and need updating when a provider renames or
  retires them.
- The sectioned layout and the reworked **Fitatu i AAPS** card were built and unit-tested but not
  yet checked by hand on a device.
