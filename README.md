# Bottle Checker

A small Android and iOS app that scans a drink container's barcode and tells you whether
a **BCRS Malta** Reverse Vending Machine will accept it.

> **Not affiliated with BCRS Malta.** The app uses the same barcode lookup as the search
> box on [bcrsmalta.mt](https://bcrsmalta.mt/#barcode-deposit). It isn't an official API:
> BCRS can change or block it at any time, and the app will then show an error instead of
> an answer. One request is sent per scan or check, and nothing is stored.

## Features

- Scan EAN-13, EAN-8, UPC-A and UPC-E barcodes with the camera.
- Type the barcode in by hand when scanning doesn't work. The check digit is verified
  locally, so typos are caught before anything is sent.
- Clear result: **Eligible**, **Not eligible**, or **Couldn't check** with a Retry button.
  Network or server problems are never reported as "not eligible".
- **Find a return point** button that opens the official map on bcrsmalta.mt.
- Phone and tablet/iPad layouts, light and dark theme.

## Status

- **Android:** tested on a real phone running Android 17.
- **iOS:** not tested on a device yet. The iOS app builds in CI (GitHub Actions on macOS),
  and the shared tests pass on the iOS simulator.

## How it works

The app sends one form POST per check:

```
POST https://bcrsmalta.mt/wp-admin/admin-ajax.php
action=barcode_data_fetch&keyword=<barcode>
```

and reads `{"status":"success"|"fail","message":"…"}` from the reply.

## Tech stack

Kotlin Multiplatform with Compose Multiplatform (Material 3) for a shared UI, Ktor client,
kotlinx.serialization, and the multiplatform `lifecycle-viewmodel`.

| | Android | iOS |
|---|---|---|
| Scanner | [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner) (a module inside Google Play services; no camera permission needed) | VisionKit `DataScannerViewController` |
| HTTP engine | OkHttp | Darwin (URLSession) |
| Minimum | Android 7.0 (API 24) | iOS 16, A12 chip or newer for scanning |

```
shared/        Kotlin Multiplatform module: almost all the code
  src/commonMain/   domain/, data/, ui/, scanner/BarcodeScanner.kt (interface)
  src/androidMain/  AndroidBarcodeScanner (Google Code Scanner)
  src/iosMain/      MainViewController + bridge interface for the Swift scanner
  src/commonTest/   validator, repository (Ktor MockEngine) and ViewModel tests
androidApp/    Android entry point (MainActivity, manifest, launcher icon)
iosApp/        Xcode project: SwiftUI host + VisionKitScanner.swift
branding/      Source app icon (SVG)
```

The barcode scanner is the only platform-specific feature. Shared code depends on the
`BarcodeScanner` interface, and each platform passes in its own implementation at start-up.

## Building

You need JDK 17 or newer **with a compiler** (`javac`). The JDK bundled with Android
Studio works: `export JAVA_HOME=/path/to/android-studio/jbr`.

### Tests

```sh
./gradlew allTests          # shared tests (Android host; iOS simulator tests run on macOS only)
```

### Android

```sh
./gradlew :androidApp:installDebug   # with a phone connected (USB debugging on) or an emulator running
```

Or open the project in Android Studio and run the `androidApp` configuration.

**Release APK** (shrunk with R8, about 2 MB):

```sh
./gradlew :androidApp:assembleRelease   # → androidApp/build/outputs/apk/release/
```

It's signed only if these Gradle properties are set, for example in `~/.gradle/gradle.properties`
(never in the repo): `releaseStoreFile`, `releaseStorePassword`, `releaseKeyAlias`,
`releaseKeyPassword`. Otherwise an unsigned APK is produced. CI reads the same values from
repository secrets, see `.github/workflows/build.yml`.

A release APK can't install over a debug one (different signing key): uninstall the debug
build first.

On the first scan, Google Play services may still be downloading the scanner module. The
app then says so; wait a moment and try again.

### iOS (on a Mac)

1. Install Xcode, and add your Apple ID under Xcode → Settings → Accounts.
2. Put your team ID in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID=`).
3. Open `iosApp/iosApp.xcodeproj`, pick your iPhone or iPad, and press Run.
   The Xcode build calls Gradle to compile the shared Kotlin framework.

With a free Apple account, apps installed on your device expire after 7 days.

## Rebranding

Branding is kept in a few places so a branded build is mostly a configuration change:

| What | Where |
|---|---|
| Android app name, application ID, version | `gradle.properties` (`app.*`) |
| iOS app name, bundle ID | `iosApp/Configuration/Config.xcconfig` |
| Title shown inside the app, all UI text | `shared/src/commonMain/composeResources/values/strings.xml` |
| Colours, typography | `shared/src/commonMain/kotlin/.../ui/theme/` |
| Icon | `branding/icon.svg` → `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` and `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` |
| Endpoint and request format | `shared/src/commonMain/kotlin/.../data/BcrsApi.kt` |
| Links to the BCRS website | `shared/src/commonMain/kotlin/.../data/BcrsLinks.kt` |

## Suggestions for BCRS

Ideas for improving the official website on phones: [docs/suggestions-for-bcrs.md](docs/suggestions-for-bcrs.md).

## License

[Apache License 2.0](LICENSE). See [NOTICE](NOTICE).
