# 01 Label capture: plan

## Components

- `app/src/main/kotlin/pl/lovefitatu/bridge/MainActivity.kt`
  - `takePicture` (`ActivityResultContracts.TakePicture`) writes to `cacheDir/photos/label.jpg`
    exposed through the `FileProvider` (`${applicationId}.files`, `res/xml/file_paths.xml`).
  - `pickPhoto` (`PickVisualMedia`, images only).
  - `handleIntent` reads `EXTRA_STREAM` for `ACTION_SEND` and the debug-only `debug_label` extra.
    `pendingIntent` holds the intent until `onUnlocked`.
  - `process(uri)` checks the API key, sets `busy`, then loads, stores and sends the image
    (continues in [02](../02-label-reading/plan.md)).
- `app/src/main/kotlin/pl/lovefitatu/bridge/ocr/ImagePrep.kt`: `load` decodes with power-of-two subsampling, applies EXIF rotation and
  scales to `STORED_EDGE` (2560).
- `AndroidManifest.xml`: `SEND` intent filter on `MainActivity` (`singleTask`, so a share reaches
  `onNewIntent`), `<queries>` for `IMAGE_CAPTURE`.

## Tests

None specific; the flow depends on Android framework classes. Verified by hand on a device.
