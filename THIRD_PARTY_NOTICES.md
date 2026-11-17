# Third-Party Notices

FleetPulse is distributed under the [MIT License](LICENSE). It uses open-source libraries and assets whose licenses remain with their respective owners. This file is a practical notice, not a replacement for the upstream license texts.

## Android application dependencies

The following are used by the application and are licensed under the **Apache License, Version 2.0** unless noted otherwise:

- AndroidX Core KTX, Core Splashscreen, and Activity Compose
- AndroidX Lifecycle runtime, runtime-compose, and viewmodel-compose
- AndroidX Navigation Compose
- Jetpack Compose UI, UI Graphics, UI Tooling Preview, Material 3, and Material Icons Extended
- AndroidX Room runtime, room-ktx, and Room compiler (KSP)
- AndroidX DataStore Preferences
- AndroidX WorkManager runtime-ktx
- Kotlin standard library and Kotlin coroutines
- Kotlin Coroutines Android and coroutines-test

Project links:

- AndroidX: <https://developer.android.com/jetpack/androidx>
- Jetpack Compose: <https://developer.android.com/jetpack/compose>
- Room: <https://developer.android.com/training/data-storage/room>
- WorkManager: <https://developer.android.com/topic/libraries/architecture/workmanager>
- Kotlin: <https://kotlinlang.org/>
- Kotlin Coroutines: <https://github.com/Kotlin/kotlinx.coroutines>

The Android Gradle Plugin, Gradle wrapper, and KSP are build-time tools under their own Apache-2.0 licenses:

- Gradle: <https://gradle.org/>
- Android Gradle Plugin: <https://developer.android.com/build>
- KSP: <https://github.com/google/ksp>

## Test-only dependencies

- **JUnit 4** — Eclipse Public License 1.0. <https://junit.org/junit4/>
- **org.json** — JSON License (test-only). <https://github.com/douglascrockford/JSON-java>
- **AndroidX Test Ext JUnit, Espresso, Compose UI Test** — Apache License 2.0. These are test dependencies and are not part of the production APK's runtime logic.

## Legacy web prototype

`reference/FleetPulse-v4.html` is a historical prototype. It is not built into the Android app. When opened directly in a browser, it loads:

- **Tailwind CSS 3.4.17** — MIT License. <https://tailwindcss.com/>
- **Chart.js 4.4.1** — MIT License. <https://www.chartjs.org/>
- **Inter** — SIL Open Font License 1.1. <https://rsms.me/inter/>
- **JetBrains Mono** — SIL Open Font License 1.1. <https://www.jetbrains.com/lp/mono/>

Those assets are fetched from Google Fonts, the Tailwind CDN, and jsDelivr only when the HTML file is opened in a browser. The Android app does not load the file and has no network permission.

## Android SDK

The project compiles against the Android SDK. The SDK itself is not redistributed in this repository. Use of the SDK is governed by the Android SDK license accepted by your local installation and CI environment.

## Images and branding

The FleetPulse launcher icon is a simple vector bolt/gauge mark created for this project. The logo under `docs/assets/fleetpulse-logo.svg` mirrors that launcher mark. No third-party logo or screenshot is included.

## Reporting a missing notice

If you believe a dependency or asset is missing from this notice, open an issue with the package name, version, and license information. Do not include tokens or private data.
