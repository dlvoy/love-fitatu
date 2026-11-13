# 01 Label capture

## Goal

Get a photo of a Love Catering meal label into the app with as few taps as possible.

## Behaviour

- The main screen offers **Zrób zdjęcie** (take a photo with the system camera) and **Z galerii**
  (pick an image with the system photo picker).
- Sharing an image to the app from any other app (`ACTION_SEND`, `image/*`) starts the same flow.
- A share that arrives while the configuration is still locked is held and processed right after
  unlocking.
- Without an OpenRouter API key the app does not process the photo; it shows an error and opens
  Settings.
- While a label is being read, the capture buttons are disabled and a progress line is shown.
- The photo is saved to history before the model is asked, so a failed read is still kept
  (see [06](../06-label-history/intent.md)).

## Constraints

- No camera permission: the camera app writes to a `FileProvider` URI the app hands it.
- The stored copy is rotated upright from EXIF and scaled to a 2560 px long edge, enough to zoom
  into small print.
- Debug builds only: an intent extra `debug_label` (model-style JSON) opens the review screen with
  preset values, to test automation without calling the API.

## Out of scope

- An in-app camera preview, cropping, or batch capture of several labels.
