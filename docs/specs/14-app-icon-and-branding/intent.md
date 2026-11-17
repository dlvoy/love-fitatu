# 14 App icon and branding

## Goal

Give LabelBite its own face instead of the stock Android camera icon and the Material purple, and
show it wherever the app introduces itself.

## Behaviour

- The icon is a friendly one-eyed monster with big teeth that has bitten the top right corner off a
  label. It is flat, in the style of Material icons, with few colours: salmon, a light green
  monster, white teeth and eye, and a dark red-brown mouth and pupil.
- The brand colour is a salmon like Fitatu's, a little more vivid (`#F08C4A`).
- The launcher icon is the monster with a white label on the salmon background. The artwork is
  large: the monster fills the icon and the label runs off its bottom edge, so it does not look
  like a small drawing set inside a circle. The system shows the same icon in the biometric prompt.
- The system splash screen shows the whole artwork, at its smaller drawn size, on salmon while the
  app starts.
- Inside the app the logo has no background and no circle around it: the monster with a salmon
  label, drawn straight on the screen.
  - The lock screen shows it large and centred, above the app name (see
    [07](../07-secure-configuration/intent.md)).
  - On the main screen it sits left of the **LabelBite** title in the top bar. Other screens and
    the selection mode keep a plain title.
- The whole app uses a light colour scheme built on the salmon: salmon buttons and accents, warm
  off-white backgrounds, pale salmon highlights. The green, orange, red and yellow state buttons
  keep their own colours.
- The camera icon stays only on **Zrób zdjęcie**, where it means the camera.

## Constraints

- The icon is drawn as SVG, kept in the repository as the source. The app ships it only as
  scalable Android vectors, with no bitmaps.
- In the launcher icon the monster stays inside the round mask, the smallest one, so no launcher
  cuts into it; only the label is cropped.
- The bite and the lines on the label are real holes, so the logo works on any background.

## Out of scope

- A themed (monochrome) launcher icon for Android 13 and later.
- An animated splash screen.
- A dark colour scheme.
