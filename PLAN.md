# BCRS Bottle Checker — Project Plan

A small app for Android and iOS. It scans a beverage container's barcode and checks
whether the container can be returned to a BCRS Malta Reverse Vending Machine.

Status: **in progress** (2026-11-08). Project skeleton, shared code, Android app and iOS sources are
written; the Android debug build and the shared tests pass. Next: test on a real Android phone, then
build and test on the Mac.

---

## 1. Feasibility (checked on 2026-11-05)

BCRS has no public API. Its barcode search box (https://bcrsmalta.mt/#barcode-deposit)
sends a simple request to the site's server, and the server answers in JSON. There's no
nonce, captcha or login on that request.

```
POST https://bcrsmalta.mt/wp-admin/admin-ajax.php
Content-Type: application/x-www-form-urlencoded

action=barcode_data_fetch&keyword=<barcode>
```

Responses seen:

```json
{"status":"success","message":"Product Eligible for Collection by BCRS"}
{"status":"fail","message":"This Product is Not Eligible for Collection by BCRS."}
```

Test barcodes:

- `5449000000996` (Coca-Cola 330 ml): success
- `1234567890123` (made-up barcode): fail

Found in the site's script `wp-content/themes/bcrs-child/assets/js/child-script.min.js`,
function `ajaxWrapperFetch`.

### Caveats

- This isn't an official endpoint, so BCRS can change or block it at any time. The app
  must handle failures cleanly.
- The reply is only yes or no. It gives no product name, deposit amount or container
  type.
- One request per user action, and no bulk queries.
- Use a neutral name, and no BCRS logo or colours, until BCRS approves a branded version.
  Add the disclaimer "Not affiliated with BCRS Malta".

---

## 2. Goals and scope

**In scope (v1):**

- Scan the barcode with the camera (EAN-13, EAN-8, UPC-A, UPC-E).
- Type the barcode in by hand when scanning doesn't work.
- Show the result: Eligible, Not eligible, or Error with a retry button.
- Android phone, iPhone and iPad.

**Out of scope for now:**

- Saving previous scans. Later, possibly with SQLDelight or Room, both of which work on
  Android and iOS.
- Publishing to Google Play or the App Store (notes are kept in §8 for later).

**Design goal:** easy to rebrand, so BCRS can take it over as their own branded app.

---

## 3. Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| Cross-platform | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform (Material 3), shared by Android and iOS |
| Networking | Ktor client (OkHttp engine on Android, Darwin engine on iOS) |
| JSON | kotlinx.serialization |
| State | Multiplatform `lifecycle-viewmodel` with `StateFlow` |
| Scanner (Android) | Google Code Scanner (`play-services-code-scanner`), no camera permission needed. It's a module inside Google Play services (not a Play Store app); Play services downloads it at install time or on first use |
| Scanner (iOS) | VisionKit `DataScannerViewController` (iOS 16+, A12 chip or newer) |
| Build | Gradle Kotlin DSL with a version catalog (`libs.versions.toml`) |
| Tests | `kotlin.test` and Ktor `MockEngine` in shared code |

### Platforms

- Android: minSdk 24, targetSdk set to the latest stable release.
- iOS: deployment target 16.0. That's the minimum for VisionKit's scanner, and it
  covers recent iPhones and iPads.

---

## 4. Architecture

```
mobileapp-recycle/
├── shared/                      # shared KMP module (com.android.kotlin.multiplatform.library)
│   └── src/
│       ├── commonMain/          # ~90% of the code
│       │   ├── data/BcrsApi.kt          # Ktor call + DTO
│       │   ├── data/BcrsRepository.kt   # check(barcode): Eligibility
│       │   ├── domain/Eligibility.kt    # Eligible | NotEligible | Error
│       │   ├── domain/BarcodeValidator.kt  # digits/length/check digit
│       │   ├── ui/CheckerViewModel.kt   # Idle | Loading | Result | Error
│       │   ├── ui/CheckerScreen.kt      # Compose UI
│       │   ├── ui/theme/                # colours, typography (brandable)
│       │   └── scanner/BarcodeScanner.kt   # expect fun / interface
│       ├── androidMain/         # actual scanner = Google Code Scanner
│       ├── iosMain/             # actual scanner = VisionKit bridge
│       └── commonTest/          # parsing + repository tests
├── androidApp/                 # Android entry point (AGP 9 needs a separate app module)
├── iosApp/                      # Xcode project, SwiftUI host for Compose
├── gradle/libs.versions.toml
├── PLAN.md
├── README.md
└── LICENSE
```

The scanner is the only platform-specific code. `commonMain` declares the scanner
interface, and each platform supplies its own version.

```kotlin
interface BarcodeScanner {
    suspend fun scan(): ScanResult   // Success(code) | Cancelled | Failure(msg)
}
```

The Android and iOS versions are passed in at app start, so shared code never touches
platform APIs directly.

---

## 5. Implementation steps

### Step 0: Environment

**On Linux (this machine):**

- [x] Install Android Studio (official tarball or `flatpak install flathub com.google.AndroidStudio`).
      Note: the installed IDE is *Android Studio for Platform* (`asfp`), which targets AOSP work.
      It was used for the SDK; the regular Android Studio or IntelliJ IDEA + KMP plugin is nicer
      for day-to-day KMP editing. Builds use its bundled JDK: `JAVA_HOME=/opt/android-studio-for-platform/jbr`
      (the system Java is a JRE without `javac`).
- [x] Run the first-time wizard: Android SDK, platform tools, emulator.
- [ ] Install the **Kotlin Multiplatform** plugin in Android Studio.
- [ ] Enable USB debugging on the Android phone and check that `adb devices` lists it.

**On the Mac (for iOS):**

- [ ] Install Xcode from the App Store, then run `xcode-select --install`.
- [ ] Install Android Studio with the KMP plugin (or IntelliJ IDEA). Not strictly
      required, but it can launch the iOS app directly.
- [ ] Open Xcode → Settings → Accounts and add your Apple ID. A free account can install
      on your own iPhone/iPad, with apps expiring after 7 days.
- [ ] On the iPhone/iPad, enable Developer Mode.

### Step 1: Project skeleton

- [x] Generate the project with the KMP wizard (https://kmp.jetbrains.com or the IDE
      wizard), selecting Android and iOS with shared UI. Done by hand from JetBrains'
      `Kotlin/KMP-App-Template` (AGP 9 layout: `shared` + `androidApp` + `iosApp`).
- [x] Add dependencies: Ktor, kotlinx.serialization, coroutines, lifecycle-viewmodel and
      the code scanner.
- [x] Android manifest: the `INTERNET` permission.
- [x] iOS `Info.plist`: `NSCameraUsageDescription`.
- [x] Initialise git, add `.gitignore`, `README.md` and `LICENSE` (see §7).
- [x] Make a first commit and push to GitHub.

### Step 2: Domain and data (commonMain)

- [x] `Eligibility` sealed type.
- [x] `BarcodeValidator`: digits only, length 8/12/13, and an optional GTIN check-digit
      test to catch misreads before calling the server.
- [x] `BcrsApi`: send the form POST, decode `{status, message}`, 10 s timeout, normal
      User-Agent.
- [x] `BcrsRepository`: turn the reply into a result.
  - `status == "success"` → `Eligible`
  - `status == "fail"` → `NotEligible`
  - Anything else (HTML, timeout, unexpected JSON) → `Error`. Never treat an error as
    "not eligible".
- [x] Unit tests with Ktor `MockEngine`: success, fail, malformed reply, HTTP 500,
      timeout.

### Step 3: UI (commonMain)

- [x] `CheckerViewModel` with a `StateFlow` of `Idle / Loading / Result / Error`.
- [x] `CheckerScreen`:
  - Large **Scan barcode** button.
  - Field to type the barcode, with a numeric keyboard and a **Check** button.
  - Result card: green tick for "Eligible", red cross for "Not eligible", grey for
    errors with a **Retry** button.
  - The scanned or typed barcode shown on the card.
- [x] A layout that adapts to iPad and tablets: centred content with a maximum width,
      not a stretched phone layout.
- [x] Light and dark theme.
- [x] Footer: "Not affiliated with BCRS Malta. Data from bcrsmalta.mt."

### Step 4: Android scanner (androidMain)

- [x] Google Code Scanner, limited to EAN-13, EAN-8, UPC-A and UPC-E.
- [x] Turn its success, cancel and failure callbacks into `ScanResult`.
- [x] Handle the first-use case where Play Services is still downloading the scanner
      module.

### Step 5: iOS scanner (iosMain + iosApp)

Code written (`iosMain/.../IosBarcodeScanner.kt`, `iosApp/iosApp/VisionKitScanner.swift`); the
Kotlin side compiles on Linux, but the Swift side has not been built yet. Tick these after the
first build on the Mac.


- [ ] Bridge to `DataScannerViewController` with the barcode symbologies EAN-13,
      EAN-8 and UPC-E (UPC-A is read as EAN-13).
- [ ] Check `DataScannerViewController.isSupported` and `isAvailable`. If the scanner
      isn't available, fall back to typing the barcode in by hand.
- [ ] Show the scanner as a full-screen modal on both iPhone and iPad.

### Step 6: Testing

- [x] Shared unit tests pass with `./gradlew allTests`.
- [ ] Android phone: scan real bottles and cans, try manual entry, try airplane mode
      (error path).
- [ ] iPhone and iPad: the same checks, plus rotating the iPad and Split View.

### Step 7: Build and run

- **Android:** `./gradlew :androidApp:installDebug`, or Run from Android Studio.
- **iOS:** open `iosApp/iosApp.xcodeproj` in Xcode, choose the device and Run.

---

## 6. Making it rebrandable (for a possible BCRS hand-off)

Keep all branding in one place so a branded version is mostly a config change:

- App name, `applicationId` / bundle ID: Gradle properties and the iOS `Config.xcconfig`.
- Colours and typography: `ui/theme/` only.
- Strings: Compose Multiplatform resources (`composeResources/values/strings.xml`), ready
  for adding Maltese later.
- Icon: one source SVG, exported per platform.
- Endpoint URL and request format: isolated in `BcrsApi`, so it's easy to switch if BCRS
  provides an official API.

---

## 7. Licence and repository

- **Licence: Apache-2.0** (decided). It's permissive, lets BCRS rebrand the code and
  ship it closed-source, and includes a patent grant and a clear NOTICE convention.
  The full text is in `LICENSE`, and `NOTICE` holds "Copyright 2026 adalseno".
  Add the short Apache header (same copyright holder) to source files when the
  project is created.
- Public GitHub repository. The README covers what the app does, the unofficial-endpoint
  disclaimer, build instructions for Android and iOS, and the licence.
- Optional: a GitHub Actions workflow that runs shared tests and builds the Android debug
  APK. iOS builds need a macOS runner.

---

## 8. Later: publishing (not planned now)

**Google Play:**

- Play Console account ($25 once) and identity verification.
- Personal accounts need a closed test (≥12 testers for 14 days) before production.
- A signed AAB, Play App Signing and a recent targetSdk.
- Privacy policy URL, Data safety form, content rating, and store graphics (512×512 icon,
  1024×500 feature graphic, screenshots).
- Neutral name and branding unless BCRS gives permission (impersonation policy).

**App Store:**

- Apple Developer Program ($99/year), App Store Connect listing, privacy "nutrition
  label", screenshots for iPhone and iPad sizes, and App Review.

**Ideally:** BCRS publishes the branded version under their own developer accounts.

---

## 9. Future ideas

- Saved scan history (SQLDelight or Room, multiplatform).
- Show the deposit value (€0.10 per eligible container), if confirmed.
- Maltese translation.
- Map of nearby Reverse Vending Machines, if BCRS publishes the locations.
