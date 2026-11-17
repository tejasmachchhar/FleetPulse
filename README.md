<div align="center">
  <img src="docs/assets/fleetpulse-logo.svg" width="104" height="104" alt="FleetPulse logo">
  <h1>FleetPulse</h1>
  <p><strong>Offline-first Android tracking for vehicle usage, mileage, fuel and charging expenses, maintenance, and running costs.</strong></p>
  <p>
    <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
    <img alt="Kotlin 2.0.21" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
    <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
    <img alt="Room 2.6.1" src="https://img.shields.io/badge/Room-2.6.1-3DDC84">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg">
  </p>
  <p>
    <a href="https://github.com/tejasmachchhar/FleetPulse/actions/workflows/android-ci.yml"><img alt="Android CI" src="https://github.com/tejasmachchhar/FleetPulse/actions/workflows/android-ci.yml/badge.svg?branch=main"></a>
    <a href="https://github.com/tejasmachchhar/FleetPulse/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/tejasmachchhar/FleetPulse?include_prereleases&label=release"></a>
  </p>
</div>

FleetPulse is a native Kotlin/Jetpack Compose Android application for a small personal fleet of electric and petrol two-wheelers. It records full-to-full mileage, wall energy or fuel consumption, historical tariffs, battery-wear cost, service reminders, and per-vehicle running costs.

The app is deliberately local-first:

- **No `INTERNET` permission**
- **No analytics, ads, tracking, or cloud account**
- **No WebView or embedded web app**
- **Android auto-backup disabled**
- Data stays in a local Room database and DataStore preferences
- Backups and CSV files leave the app only when the user chooses a destination through Android's system file picker

> **Verification status:** version 4.1.1 is source-, unit-test-, lint-, and debug-build-verified. The build environment used for this publication has no Android emulator or connected device, so the UI has not been launched or captured here. See [Screenshots](#screenshots) for the honest status.

## Contents

- [Screenshots](#screenshots)
- [Features](#features)
- [How the calculations work](#how-the-calculations-work)
- [Tech stack](#tech-stack)
- [Install](#install)
- [Build and test](#build-and-test)
- [Project structure](#project-structure)
- [Data, privacy, and backup](#data-privacy-and-backup)
- [Verification](#verification)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Screenshots

**No runtime screenshots are committed yet.** This repository was prepared in a phone-hosted Linux environment with no emulator, no connected Android device, and no display. Publishing mockups, re-creations, or images from the legacy web prototype would misrepresent the Android app, so none are included.

To capture real screens after installing the app:

```bash
# With a device connected over adb
./scripts/capture-screenshots.sh docs/screenshots
```

The script captures the Dashboard, Vehicles, Usage Log, Analysis, Maintenance, and Settings screens. The screenshots can then be committed under `docs/screenshots/` and linked from this section. See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#device-testing-and-screenshots) for the expected device checks.

## Features

### Dashboard

- Fleet KPI cards for distance, energy/fuel spend, battery-wear allowance, running cost per km, maintenance, and EV-vs-petrol savings.
- Vehicle and period filters: all data, current month, last 30/90 days, current/previous financial year, or a custom date range.
- Fuel/energy chart, efficiency trend, per-vehicle spotlight, data-quality indicator, and a clear **Sample Mode** banner.
- Month-to-month and per-vehicle comparisons without hiding unavailable values behind fake zeroes.

### Vehicles

- Add, edit, archive, restore, and delete vehicles with cascade protection.
- Electric or petrol energy source with source-specific fields.
- Battery capacity, replacement rate, expected life, charge cycles, rated km/charge, and planned daily km for EV wear modelling.
- Tank capacity, colour, registration, purchase date, notes, and initial odometer.
- Archiving preserves history; deleting a vehicle removes its records only after confirmation and while at least one vehicle remains.

### Quick entry

- One FAB with EV charge, petrol refill, odometer/ride-only, and maintenance shortcuts.
- Forms adapt to the selected vehicle's energy source.
- EV charging supports metered wall kWh or voltage-derived energy from the per-vehicle calibration curve.
- Petrol entry accepts any two of quantity, ₹/L, and refilling amount and derives the third value.
- Previous odometer, duplicate-reading detection, live preview, ERROR blocking, and CHECK warnings.
- **Include this entry for mileage estimation** is checked by default. Excluded entries stay in history and expenses but never contribute energy/fuel to a mileage denominator and cannot start or end a full-to-full interval.
- Keyboard-safe forms and dialogs use `WindowInsets.safeDrawing` and bring-into-view scrolling on the IME inset.

### Usage log

- Card-based history with date, vehicle, fill type, odometer, distance, energy/fuel, cost, basis, status, and eligibility.
- Status filters for OK, CHECK, ERROR, and all entries.
- Data-quality explanations that say exactly why an entry needs attention.
- Edit and confirmed delete actions.

### Analysis

- Mileage, cost, and energy tabs.
- Verified-vs-provisional detail with the basis shown instead of silently mixing measured and estimated data.
- Battery-wear breakdown, running-cost split, EV-vs-petrol savings, and historical tariff history.
- Separate EV km/kWh and petrol km/L trends; the app never combines the units into a misleading single series.

### Maintenance

- Service records with date, odometer, category, description, amount, next-due date, next-due km, and remarks.
- Automatic retirement of older same-category reminders as **Completed**.
- Overdue, Due Soon, OK, and Logged states.
- Daily local WorkManager reminder check with an Android notification; no network call is involved.
- Km reminders use the latest odometer actually logged by the user.

### Calibration

- Per-EV voltage-to-battery-percentage curve with linear interpolation.
- Live voltage checker showing the resulting percentage.
- Duplicate, inverted, out-of-range, and missing-curve checks.
- Clear warning that the sample curves are typical examples, not measurements of the user's pack.

### Settings

- Charger efficiency, plausible-efficiency bands, maximum plausible distance, reminder thresholds, chart start month, maintenance categories, theme, notifications, and historical tariffs.
- Historical tariffs are date-effective: a new price never rewrites an older entry's cost.
- Material 3 light/dark/system theme.

### Data management

- JSON backup export/import through Android's Storage Access Framework.
- Backup format v2 plus legacy FleetPulse v4 web-state import.
- Import is parsed and previewed before any write; restore replaces the dataset in one Room transaction.
- Evaluated CSV exports for usage and maintenance.
- Sample-data load/removal with `isSample` separation.

### Verification

- In-app **Calculation Verification** screen runs the same reference suite as the JVM tests.
- 30 executable cases cover workbook parity, full-to-full boundaries, partial fills, voltage interpolation, historical tariffs, battery wear, maintenance retirement, and edge cases.

## How the calculations work

The full rules are in [docs/CALCULATION_LOGIC.md](docs/CALCULATION_LOGIC.md). The short version:

### Full-to-full mileage

A verified stretch starts at an eligible full charge or full tank and ends at the next eligible full charge or full tank.

```text
verified distance = end odometer - start odometer
verified energy   = eligible energy/fuel added during the stretch
efficiency        = verified distance / verified energy
```

Partial charges and refills accumulate into the next full-to-full stretch. They never produce an independent mileage result. An entry with **Include this entry for mileage estimation** off contributes no energy/fuel to the denominator and cannot be a boundary. Its distance, cost, and history are still retained.

### EV energy

- **Meter mode:** wall kWh is used directly and labelled `Measured`.
- **Voltage mode:** battery percentages come from the calibration curve, then:

```text
wall energy = (percent_after - percent_before) / 100
              * usable battery capacity / charger efficiency
```

The result is labelled `Estimated`; a full charge uses 100% as the after value.

### Battery wear

```text
battery price       = usable capacity * replacement rate per kWh
cycles capacity     = expected cycles * km per charge
age capacity        = expected life years * 365 * planned km per day
distance considered = min(cycles capacity, age capacity)
wear cost per km    = battery price / distance considered
```

Km per charge is the all-time average of verified EV stretches, falling back to the rated km/charge when no verified stretch exists. Wear is an allowance, not cash spent, and is shown separately from energy cost. The dashboard states whether actual or rated km/charge was used.

### Running cost

```text
running cost per km = verified energy cost per km + battery wear per km
total running cost  = all energy/fuel spend in the period + wear for distance in the period
```

The two figures answer different questions: the per-km rate uses verified stretch costs, while the period total includes every saved entry's energy or fuel cost (including a baseline or partial fill). That is why the reference total is ₹477.75 while the per-km rate is ₹1.79.

Maintenance is reported separately, with an optional all-in per-km view when both a running-cost basis and a maintenance cost basis exist.

### Petrol entry calculator

Enter any two of quantity (L), unit price (₹/L), or amount (₹):

```text
amount     = quantity * unit price
unit price = amount / quantity
quantity   = amount / unit price
```

Zero, blank, non-finite, and division-by-zero inputs are rejected. Quantity is shown to three decimals; unit price and amount use two.

### Data quality

- **ERROR** — unusable in calculations: missing mandatory fields, missing calibration, impossible values, or an odometer rollback. Retained and listed; new entries with an ERROR preview are blocked.
- **CHECK** — calculated but unusual: implausible efficiency, duplicate reading, out-of-range voltage, or a first reading far above the initial odometer. Included and flagged.
- **OK** — complete and within configured plausibility bounds.

### Reference parity

The demo dataset and verification suite reproduce the reference workbook values:

| Check | Value |
|---|---:|
| Demo EV 1 verified distance | 254.0 km |
| Demo EV 1 verified wall energy | 5.43 kWh |
| Demo EV 1 verified efficiency | 46.7 km/kWh |
| Demo EV 1 average km per full charge | 163.5 km |
| Demo EV 1 cycle capacity | 408,742 km |
| Demo EV 1 age capacity (limiting distance) | 38,325 km |
| Demo EV 1 battery wear | ₹1.63/km |
| Demo EV 1 verified energy cost | ₹0.16/km |
| Demo EV 1 running cost | ₹1.79/km |
| Demo EV 1 total running cost | ₹477.75 |
| Demo EV 2 fallback wear | ₹0.70/km |
| Petrol partial-refill stretch | 62.0 km/L |
| Calibration at 60.0 V | 55.0% |

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Single-activity Compose shell, `AndroidViewModel`, `StateFlow` |
| Persistence | Room 2.6.1 with KSP; DataStore Preferences |
| Background work | WorkManager 2.10.0 |
| Build | Gradle 8.9, Android Gradle Plugin 8.7.3, JDK 17 |
| SDK | `minSdk 26`, `compileSdk 35`, `targetSdk 35` |
| Tests | JUnit 4, `kotlinx-coroutines-test`, `org.json` |

## Install

### Requirements

- Android 8.0 (API 26) or newer
- No internet connection is required
- No account, subscription, or server is used

### From a release

Download the APK from [Releases](https://github.com/tejasmachchhar/FleetPulse/releases) and open it on the phone. Android may ask you to allow installation from the browser or file manager you used.

Release assets are labelled by build type. A **debug/development** APK is debug-signed and is not a production-signed Play Store build. Never install an APK from an untrusted mirror; compare its SHA-256 with the `SHA256SUMS` asset.

### From source

```bash
git clone https://github.com/tejasmachchhar/FleetPulse.git
cd FleetPulse
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk   # platform 35 + build-tools 35.0.0
./scripts/verify.sh
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Build and test

Tested toolchain:

| Component | Version |
|---|---|
| JDK | 17 |
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 (KSP) |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |

Useful commands:

```bash
# Full local verification: unit tests, lint, and debug APK
./scripts/verify.sh

# Individual tasks
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug

# Install on a connected device (requires adb)
./gradlew :app:installDebug
```

`app/schemas/` contains the Room schema exports and is tracked source. Do not add it to `.gitignore`.

### Build notes for arm64 Linux hosts

The x86_64 AAPT2 binary shipped with AGP cannot run natively on arm64. On an arm64 host (including a phone-hosted Linux environment), use a local Gradle property outside the repository:

```properties
# ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/path/to/aapt2-wrapper
```

The wrapper must execute an amd64 AAPT2 through an amd64 sysroot. This is machine-specific and deliberately not committed. On a normal x86_64 desktop no override is needed.

The project's `gradle.properties` limits Gradle to `-Xmx1536m`, one worker, and two worker threads so it can run on a memory-constrained host. Raise these values if your machine has more memory.

## Project structure

```text
.
├── app/
│   ├── schemas/                         Room schema exports (versioned source)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/fleetpulse/app/
│       │   │   ├── MainActivity.kt      Compose shell, navigation, FAB sheet
│       │   │   ├── FleetPulseApp.kt     Room, DataStore, WorkManager bootstrap
│       │   │   ├── data/
│       │   │   │   ├── db/              Entities, DAOs, database, migration
│       │   │   │   ├── repo/            Flows, demo data, transactional restore
│       │   │   │   ├── backup/          JSON v2, legacy import, CSV
│       │   │   │   └── prefs/           DataStore preferences
│       │   │   ├── domain/
│       │   │   │   ├── CalcEngine.kt    Pure Kotlin calculation engine
│       │   │   │   ├── NumberText.kt    Editable-number parsing/formatting
│       │   │   │   ├── PetrolPricing.kt Two-of-three petrol formulas
│       │   │   │   ├── VerificationSuite.kt
│       │   │   │   └── SampleData.kt    Synthetic demo dataset
│       │   │   ├── notifications/       Daily reminder worker
│       │   │   └── ui/                  ViewModel, components, theme, screens
│       │   └── res/
│       └── test/                        JVM unit tests
├── docs/                                Architecture, calculations, schema, guides
├── reference/FleetPulse-v4.html         Legacy single-file prototype (not built)
├── scripts/                             Verification and screenshot helpers
├── .github/                             CI, release workflow, issue/PR templates
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
├── PRIVACY.md
├── SUPPORT.md
├── THIRD_PARTY_NOTICES.md
└── LICENSE
```

`reference/FleetPulse-v4.html` is the original single-file prototype kept for parity and historical context. It is **not** part of the Android build. Its demo labels have been sanitized, and it loads its old CDN dependencies only if someone opens it in a browser. The Android app never loads it and has no network permission.

## Data, privacy, and backup

- The database is `fleetpulse.db` in the app's private storage.
- Preferences are in DataStore; Android auto-backup is disabled with `allowBackup="false"`.
- The app declares no `INTERNET` permission.
- WorkManager adds local scheduling permissions only (`WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`). They do not transmit data.
- Backups and CSV exports go only where the user points Android's system file picker.
- Removing sample data deletes only rows flagged `isSample`; genuine records are preserved. Demo vehicles are removed only when no genuine record references them.

Backup format:

```json
{
  "format": "fleetpulse-backup",
  "version": 2,
  "exportedAt": "2026-11-17T10:00:00Z",
  "data": {
    "vehicles": [],
    "usageEntries": [],
    "maintenanceEntries": [],
    "priceHistory": [],
    "calibration": [],
    "settings": {}
  }
}
```

Each usage entry may carry `"includeInMileage": true|false` (optional). Older v2 and legacy v4 files without the field import as `true`. The Room schema moved from version 1 to 2 with the additive migration:

```sql
ALTER TABLE usage_entries ADD COLUMN includeInMileage INTEGER NOT NULL DEFAULT 1;
```

See [PRIVACY.md](PRIVACY.md) and [docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md).

## Verification

The repository's JVM suite contains **42 tests**:

| Test suite | Tests | Focus |
|---|---:|---|
| `CalcEngineTest` | 25 | Workbook parity, full-to-full boundaries, eligibility, battery wear, maintenance, tariffs, calibration |
| `BackupManagerTest` | 9 | JSON v2 round-trip, legacy import, eligibility, rejection |
| `NumberTextTest` | 3 | Editable-number parsing and formatting |
| `PetrolPricingTest` | 4 | Two-of-three petrol formulas and zero-input handling |
| `VerificationSuiteTest` | 1 | All in-app verification cases pass |

Last verified locally:

- `:app:testDebugUnitTest` — 42 tests, 0 failures
- `:app:lintDebug` and `:app:lintRelease` — 0 errors, 18 non-blocking warnings each
- `:app:assembleDebug` — debug APK produced
- `:app:assembleRelease` / `:app:bundleRelease` — unsigned APK without secrets, and a signed APK with a temporary local test key; the test key was deleted and no production signing secrets are configured or published

The debug APK was **not launched** in the publication environment because there is no emulator or connected device. Device acceptance (install, first-launch choice, form entry, notification delivery, SAF pickers, keyboard behaviour, and visual review) still needs one manual pass on the target phone. The keyboard/IME changes are compile- and lint-verified only.

## Roadmap

These are maintenance intentions, not promises or dated commitments:

- Complete a device acceptance pass and add genuine screenshots from the running app.
- Add Room migration tests for future schema versions.
- Add a signed release pipeline when the maintainer's signing keys and secrets are available.
- Add accessibility and localization review, starting with TalkBack labels and RTL layout.
- Add import-validation fuzz tests for hostile or malformed backup files.
- Keep dependency and GitHub Actions updates reviewed through Dependabot.
- Evaluate an Android Gradle Plugin 9 / `compileSdk 37` migration before adopting AndroidX releases that require it; the current baseline remains AGP 8.7.3 / `compileSdk 35`.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Please:

- Use a synthetic dataset when reporting a calculation issue.
- Never post a real registration number, address, backup file, keystore, or token.
- Run `./scripts/verify.sh` before submitting.
- Report security issues privately as described in [SECURITY.md](SECURITY.md).

## License

FleetPulse is released under the [MIT License](LICENSE).

The Android app depends on third-party libraries under their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The legacy single-file prototype under `reference/` is included as historical source and is not part of the built application.

## Disclaimer

FleetPulse is a personal record-keeping and estimation tool. Battery wear, efficiency, savings, and service reminders are estimates derived from the data you enter. They are not a substitute for the vehicle manufacturer's service schedule, a qualified mechanic's inspection, or professional financial advice. Always verify safety-critical maintenance and battery condition independently.
