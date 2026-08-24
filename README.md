# Feldman Flashlight

A touch-first Android flashlight for the camera flash and the whole screen, with safety signals, a Quick Settings tile, expressive personalization, and cutout-safe landscape controls.

## Download

[**Download the latest APK from Releases**](https://github.com/feldmandev/feldman-flashlight/releases/latest)

> [!IMPORTANT]
> Android may ask you to allow installation from your browser or file manager. Only install APKs downloaded from this repository.

Feldman Flashlight requires Android 14 or newer.

## Features

- Control the camera flash at multiple intensity levels
- Turn the full display into an even screen light with preset or custom colors
- Run Blink, Morse, and SOS safety signals
- Use Flash, Screen, or Both light sources together
- See ambient luminance when the device provides a light sensor
- Control the flashlight from a configurable Quick Settings tile
- Configure automatic shutdown, volume-button controls, and launch behavior
- Personalize theme, colors, motion, expressive shapes, and orientation
- Use a dedicated landscape layout that accounts for left, right, or absent camera cutouts
- Keep settings entirely on-device without accounts, ads, analytics, or tracking

## Screenshots

### Phone

<p align="center">
  <img src="screenshots/phone/01_flashlight_controls.png" alt="Flashlight controls" width="32%">
  <img src="screenshots/phone/02_screen_light.png" alt="Screen light" width="32%">
  <img src="screenshots/phone/03_light_sources.png" alt="Flash and screen light sources" width="32%">
</p>
<p align="center">
  <img src="screenshots/phone/04_safety_signals.png" alt="Safety signals" width="32%">
  <img src="screenshots/phone/05_settings.png" alt="Flashlight settings" width="32%">
  <img src="screenshots/phone/06_personalization.png" alt="Appearance personalization" width="32%">
</p>
<p align="center">
  <img src="screenshots/phone/07_quick_settings_tile.png" alt="Quick Settings tile" width="32%">
  <img src="screenshots/phone/08_landscape.png" alt="Landscape flashlight controls" width="32%">
  <img src="screenshots/phone/09_landscape_settings.png" alt="Landscape settings" width="32%">
</p>

## Source and builds

Requirements:

- JDK 17
- Android SDK 37

Build a debug APK:

```bash
./gradlew assembleDebug
```

Debug builds use Android's default debug key. To use your own signing key, copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties` and fill in the local values. Signing credentials and keystores are ignored by Git.

## Project policies

- [Privacy policy](https://feldmandev.github.io/feldman-flashlight/privacy-policy.html)
- [License page](https://feldmandev.github.io/feldman-flashlight/license.html)
- [Issue tracker](https://github.com/feldmandev/feldman-flashlight/issues)

## License

The source code is licensed under the [Apache License 2.0](https://feldmandev.github.io/feldman-flashlight/license.html). The canonical license text is also available in [`LICENSE`](LICENSE).

The Feldman name, logos, app icon, and promotional artwork are not licensed for reuse under the Apache License.
