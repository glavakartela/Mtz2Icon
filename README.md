# mtz2icon

Create launcher shortcuts that use icons from a Xiaomi theme / icon pack (`.mtz`, `.zip`), in a shape you choose.
Built for Xiaomi, Redmi and POCO phones running HyperOS. No root needed.

Kotlin, Jetpack Compose, Material 3 (Material You). Package: `com.glavalartela.mtz2icon`.

## Features

- **Import a pack** — pick a `.mtz` theme or a `.zip` icon pack. The nested `icons` archive of an `.mtz` is read automatically.
- **Matching** — pack icons are matched to your installed apps by file name (`<package>.png`, `<package>$<activity>.png`) or through an `appfilter.xml` if the zip contains one.
- **Apply only some icons** — search the list, tick the apps you want, create shortcuts only for those.
- **Shapes** — launcher/original (keeps the pack icon as it is), circle, squircle, diamond, hexagon, octagon, flower, star, clover, square. **Draw your own shape** with a finger (it is smoothed and fitted to the icon square) or paste a shape code (`icrec-…`, compatible with IconRecolor). Custom shapes are saved, can be copied as a code and deleted. A zoom slider crops the pack icon's own rounded corners.
- **Languages** — English, Deutsch, Русский, Українська, Polski, Español, Français. Follows the system language; can be changed per app in Settings.
- **Settings** — a sheet over the main screen: theme (system / light / dark), dynamic colors on or off, remember shape and zoom, select all matches after import, show package names, delay between shortcut requests, icon quality, reset.
- **Local only** — everything happens on the device. The app has no internet permission.

## Requirements

- Android 13 or newer (API 33).
- A Xiaomi / Redmi / POCO device on HyperOS. Other devices are not guaranteed to work.
- [Shizuku](https://shizuku.rikka.app/) running — recommended. It enables the Xiaomi “Home screen shortcuts” permission with one tap. You can also enable that permission manually.

## Usage

1. Start Shizuku, then tap **Connect** and **Allow** in the shortcut permission card (or enable “Home screen shortcuts” manually via the settings button).
2. Tap **Choose icon pack** and select a `.mtz` / `.zip`.
3. Pick a shape and zoom, or tap **Draw** to make your own. The previews update on your own icons.
4. Tick the apps you want. **Select all** / **Clear** work on the visible (filtered) list.
5. Tap **Create shortcuts**. The launcher asks you to confirm each shortcut; the delay between requests can be changed in Settings.

Shortcuts are added next to the original icons; nothing on the system is changed. Creating a shortcut again for the same app updates the existing one.

## Supported packs

| Pack | Supported |
| --- | --- |
| `.mtz` theme (Xiaomi Themes) | yes, icons are read from the nested `icons` archive |
| `.zip` with PNG / WebP icons named after packages | yes |
| `.zip` with `appfilter.xml` (ADW/Nova style) | yes |
| Installed icon-pack APKs | not yet |

Large non-icon files in a pack (wallpapers, fonts, lock screens, previews) are skipped.

## Troubleshooting

- **“No icons in this pack match your installed apps”** — the pack uses names this app can't map to your apps. Open an issue with the pack's file list.
- **No shortcut dialog appears** — the “Home screen shortcuts” permission is missing. Use Connect / Allow, or enable it manually.
- **Corners look cut off or the icon has a frame** — change the zoom or pick another shape; use “original” to keep the pack icon untouched.
- **Shizuku is not running** — open the Shizuku app and start it (wireless debugging or adb).
- **Shortcut icon is blurry** — set a higher icon quality in Settings.

## Building

Needs JDK 17 or newer (JDK 25 works) and the Android SDK.

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. `gradle.properties` points `aapt2` to the Termux binary for on-device builds; remove that line on a regular machine or in CI.

Toolchain: Android Gradle Plugin 9.2.0, Gradle 9.4.1, compileSdk 37, minSdk 33.

## Translations

Add a `res/values-<lang>/strings.xml` copied from `res/values/strings.xml`, and add the language to `res/xml/locales_config.xml` and to the list in `LanguageRow` (`MainScreen.kt`).

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE).
