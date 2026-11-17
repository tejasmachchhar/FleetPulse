# Changelog

All notable changes to FleetPulse are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Version codes are monotonic Android integers and are independent of the Git tag suffix.

The public repository history begins with the 4.1.1 baseline. Earlier private iterations are not reconstructed or back-dated.

## [Unreleased]

### Planned

- Genuine device screenshots and a full device acceptance pass.
- Migration tests for future Room schema versions.
- A signed release pipeline when signing credentials are available.
- Accessibility and localization review.

## [4.1.1] - 2026-11-17

### Added

- Native Kotlin/Jetpack Compose Android application with an offline-first Room database.
- Dashboard with fleet KPIs, vehicle/period filters, energy charts, efficiency trends, data-quality counts, and Sample Mode.
- Vehicle management for electric and petrol two-wheelers, including archiving and cascade-safe deletion.
- Quick entry for EV charging, petrol refills, ride-only odometer readings, and maintenance.
- Full-to-full mileage engine with verified and provisional bases, partial-fill accumulation, and per-entry mileage eligibility.
- Metered and voltage-derived EV energy calculation with per-vehicle calibration curves and linear interpolation.
- Historical tariffs, battery-wear allowance, running-cost per km, period totals, EV-vs-petrol savings, and all-in maintenance cost.
- Usage log with status filters, eligibility markers, and data-quality explanations.
- Maintenance records, automatic retirement of superseded reminders, due/overdue states, and daily local notifications.
- JSON backup export/import (format v2), legacy FleetPulse v4 state import, transactional restore, and evaluated CSV exports.
- In-app calculation verification suite and JVM test coverage.
- Synthetic demo dataset clearly separated from user records with `isSample`.
- Repository documentation, architecture and calculation guides, privacy/security/support policies, issue and pull-request templates, Dependabot configuration, CI, and a tag-driven release workflow.

### Changed

- Vehicle, maintenance, Quick Entry, Settings, calibration, and tariff forms now use IME-safe layout and scrolling.
- Full-screen editors use `WindowInsets.safeDrawing`, and editable fields request bring-into-view when the keyboard inset changes.
- Tariff and calibration-point dialogs share a keyboard-aware form dialog with fixed actions and a scrollable body.
- Demo vehicle names and the registration number were replaced with synthetic `Demo EV 1`, `Demo EV 2`, `Demo Petrol`, and `DEMO-0001` labels before public publication.

### Fixed

- Keyboard and IME overlap on long forms and dialogs.
- Focused fields remaining hidden behind the keyboard after the IME animation.
- Vehicle notes field not scrolling into view while editing.

### Security

- No `INTERNET` permission, analytics, ads, cloud account, or network API.
- Android auto-backup disabled; backups and CSV exports are user-initiated through the system file picker.
- No signing keys, tokens, keystores, local databases, or generated APKs are tracked in the repository.
- Release signing is environment-driven and never falls back to a fake signed build.

### Verification

- `./gradlew :app:testDebugUnitTest` — 42 tests, 0 failures.
- `./gradlew :app:lintDebug` and `:app:lintRelease` — 0 errors, 18 non-blocking warnings each.
- `./gradlew :app:assembleDebug` — debug APK produced with versionName 4.1.1 and versionCode 3.
- `./gradlew :app:assembleRelease` / `:app:bundleRelease` — unsigned without signing secrets; the conditional signing path was verified with a temporary local test key, which was deleted afterwards.
- Device launch, screenshots, notification delivery, SAF pickers, and visual keyboard behaviour remain pending on a real Android runtime.

[Unreleased]: https://github.com/tejasmachchhar/FleetPulse/compare/v4.1.1...HEAD
[4.1.1]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.1.1
