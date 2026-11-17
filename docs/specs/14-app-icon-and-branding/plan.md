# 14 App icon and branding: plan

## Components

- `docs/brand/labelbite-icon.svg`: the launcher artwork on the 108 x 108 adaptive icon grid. The
  salmon square is the background; the `foreground` group holds the white label, the mouth, the
  monster, its teeth and its eye, all as `<path>` elements, enlarged 1.25 times by the group's
  `transform`. The label is one even-odd path: its
  outline follows the tooth marks of the bite, and the text lines are holes in it.
- `docs/brand/labelbite-logo.svg`: the same shapes with a salmon label and no background, cropped
  to a 60 x 60 square around the artwork.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_logo.xml`: the two SVGs as vector
  drawables, path for path, each with the SVG group's transform on a `<group>`. `ic_splash.xml` is
  the launcher foreground without the enlargement. Changing an SVG means copying the path data
  over.
- `app/src/main/res/values/colors.xml`: `ic_launcher_background` (the salmon, `#F08C4A`) and
  `app_background`.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: the adaptive icon, the colour under the
  foreground. The manifest uses it for `android:icon` and `android:roundIcon`.
- `app/src/main/res/values/themes.xml`: `Theme.LabelBite`, the previous
  `Theme.Material.Light.NoActionBar` plus the window background, the accent colour,
  `windowSplashScreenBackground` and `windowSplashScreenAnimatedIcon` (`ic_splash`, whose
  artwork fits the circle the splash screen shows). `minSdk` is 31, so the platform
  splash attributes need no library.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/Theme.kt`: `Brand` and `LabelBiteTheme`, a
  `lightColorScheme` with every role set, so nothing falls back to the purple baseline.
  `MainActivity` wraps its content in it.
- `app/src/main/kotlin/pl/cukrzycowy/labelbite/ui/AppLogo.kt`: `AppLogo(size)` draws `ic_logo`.
  Used in the top bar title on the main screen and on `LockScreen`.

## Tests

None; it is artwork and resources. The SVGs were rendered under a circle and a rounded square mask
and on the app background, at full and at small size, to check the shapes. Checked by hand on a
device.
