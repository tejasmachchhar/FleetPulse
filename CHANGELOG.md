# Changelog

All notable changes to FleetPulse are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Version codes are monotonic Android integers and are independent of the Git tag suffix.

The public repository history begins with the 4.1.1 baseline. Earlier private iterations are not reconstructed or back-dated.

## [Unreleased]

### Planned

- Genuine device screenshots and a full device acceptance pass.
- Migration tests for future Room schema versions.
- A signed release pipeline when signing credentials are available.
- A deliberate Android Gradle Plugin 9 / `compileSdk 37` migration before adopting AndroidX releases that require it.
- Accessibility and localization review.

## [5.0.0] - 2026-11-20

### Added

- Universal vehicle support: two-wheelers, three-wheelers, cars, pickups, tractors, trucks, buses, and construction equipment can be managed without a separate app.
- Flexible vehicle taxonomy with future-extensible categories and subcategories.
- Batch 1 vehicle reference database integration with `database_version: 1.0.0-batch1`: 30 verified records, 23 manufacturers, 133 citations, complete specification data, source provenance, and discrepancy reports.
- Remote reference retrieval with local DataStore/Room caching, offline fallback, and manual-entry fallback.
- Guided 7-step vehicle setup wizard: category, subtype, propulsion, manufacturer search, model search, reference-spec review, and personalisation.
- Universal calculations with distance for road vehicles and operating hours for agricultural/construction machinery.
- Dashboard, usage log, and quick-entry support for operating-hours tracking.

### Changed

- Existing EV/bike behaviour remains backward compatible.
- Database version is now 3 with additive migrations.

### Verification

- `./gradlew :app:testDebugUnitTest` — 54 tests, 0 failures.
- `./gradlew :app:lintDebug` — 0 errors.
- `./gradlew :app:assembleRelease` — signed release APK produced with versionName 5.0.0 and versionCode 8, SHA-256 `e0fdb364cb4bed3e8ed8d2f67e102ddf785179472707ddf7e3748b3dcde0b936`.
- `vehicle-reference/scripts/validate_database.py` — 30 records, 133 sources, 0 errors.

## [4.5.0] - 2026-11-20

### Added

- Persistent floating onboarding companion: when the user starts vehicle setup, onboarding minimizes into a dismissible, draggable pill while the user completes the vehicle creation flow.
- Tapping the pill restores onboarding at the saved step and progress.
- Completion state is based on a successful new-vehicle save; the bubble updates to show “Vehicle added”.
- Setup progress, animation, and restoration survive screen transitions and configuration changes.

### Changed

- Vehicle creation from onboarding now opens the real Vehicles screen, then minimizes onboarding.
- Existing users are not forced into onboarding if vehicle/usage data is already present.

### Verification

- `./gradlew :app:testDebugUnitTest` — 54 tests, 0 failures.
- `./gradlew :app:lintDebug` — 0 errors, 18 non-blocking warnings.
- `./gradlew :app:assembleRelease` — signed release APK produced with versionName 4.5.0 and versionCode 7.
- APK verified with `apksigner`: valid signature, not debuggable, package `com.fleetpulse.app`, SHA-256 `4d1df5ff068f171eb2160c330a712086cc5632b954a5876cc7329b40a0f06408`.

## [4.4.0] - 2026-11-20

### Added

- Self-guided first-time onboarding with Welcome, Add Vehicle, Usage Tracking, Mileage & Analytics, Maintenance, Backup & Data Safety, and Ready screens.
- Replayable onboarding from Settings and More → Getting Started.
- Automatic completion for existing installations that already contain vehicle or usage data, so upgrades do not force onboarding.

### Changed

- Production release is now signed with the dedicated FleetPulse release key instead of being published as an unsigned/debug-signed artifact.
- Onboarding completion persists in app preferences and survives process recreation.

### Fixed

- Release signing setup now uses the verified keystore at `~/.fleetpulse-release/fleetpulse-release.jks`.

### Verification

- `./gradlew :app:testDebugUnitTest` — 54 tests, 0 failures.
- `./gradlew :app:lintDebug` — 0 errors, 18 non-blocking warnings.
- `./gradlew :app:assembleRelease` — signed release APK produced with versionName 4.4.0 and versionCode 6.
- APK verified with `apksigner`: valid signature, not debuggable, package `com.fleetpulse.app`.

## [4.3.0] - 2026-11-19

### Added

- Dedicated **About FleetPulse** screen under More with verified project links for the GitHub repository, user guide, changelog, latest release, privacy policy, and support/reporting guide, plus version, versionCode, build type, and package details.
- **Email Feedback & Support** flow with Bug, Improvement, and General Feedback drafts, prefilled recipient `tejasmachchhar2019@gmail.com`, generated subjects and bodies, optional app/device diagnostics, exact preview, consent gate, mailto chooser, and copy/share/copy-address fallbacks.
- Structured **UI/UX improvement** GitHub issue form with affected screen, current behaviour, suggested improvement, expected benefit, improvement area, optional version/screenshot, and privacy confirmations.
- Review-first email builder coverage for bug, feature, and general messages, including opt-outs for app, device, and recent diagnostics.
- `mailto` package-visibility query so Android 11+ can identify installed email handlers.

### Changed

- Project links moved from Feedback & Support to **More → About FleetPulse**; Feedback & Support now focuses on reporting, email, and support actions without a duplicate link list.
- Bug and feature GitHub issue forms now use clearer required/optional fields, frequency, separate Android/device inputs, optional screenshots, privacy reminders, and triage labels.
- Issue template chooser links to email support, the user guide, support policy, UPI/Buy Me a Coffee support, and the private security policy. Blank issues remain disabled.
- The support email is configured as a public FleetPulse feedback address; app and documentation no longer describe it as hidden.
- Backup app metadata now uses the current build version instead of a hard-coded string.
- Version name is `4.3.0` and versionCode is `5`.

### Fixed

- **Pay using UPI** now resolves handlers with `queryIntentActivities`, builds explicit package targets for the chooser, includes `cu=INR` in the URI, skips the chooser for a single handler, and catches launch failures. No amount is fixed.
- UPI fallback now offers install/configure guidance, QR scanning, UPI ID copy, and payment-details sharing instead of leaving the button apparently unresponsive.
- Email hand-offs use a proper `mailto:` URI instead of an empty `mailto` with extras, so more email clients receive the prefilled subject and body.

### Security

- No email is sent silently, no SMTP credentials or mail backend are included, and no delivery confirmation is claimed.
- Email drafts never attach database files, backups, usage records, maintenance records, expenses, notes, location, or private logs automatically.
- UPI remains amount-free and does not verify payment completion.

### Verification

- `./gradlew :app:testDebugUnitTest` — 54 tests, 0 failures.
- `./gradlew :app:lintDebug` — 0 errors, 18 non-blocking dependency/legacy warnings.
- `./gradlew :app:assembleDebug`, `:app:assembleRelease`, and `:app:bundleRelease` — all succeeded; debug APK is versionName 4.3.0 / versionCode 5. The release APK is signed with a newly generated local release key and published as stable GitHub Release `v4.3.0`.
- UPI and email intent construction, report/draft formatting, opt-outs, and fallback copy are JVM/compile-verified. Actual Google Pay, PhonePe, Paytm, BHIM, email-client, GitHub, and share-sheet hand-offs still require a device pass because the publication environment has no Android runtime.

## [4.2.0] - 2026-11-19

### Added

- Native **Feedback & Support Centre** under More with Report a Bug, Suggest an Improvement, Send General Feedback, Support FleetPulse, and verified project links.
- Structured bug, feature, and general-feedback reports with automatic app/device/environment details and optional redacted local diagnostics.
- Exact report preview with per-section diagnostic opt-outs and an explicit consent checkbox before GitHub, share, copy, or email actions.
- Prefilled GitHub issue URLs with category labels and honest handling: opening a browser is never reported as a submitted issue. Reports longer than the reliable prefilled-link size are shortened locally with an explicit note, and the preview shows that exact shortened text.
- Android Sharesheet and clipboard alternatives that work without a GitHub account. Email remains hidden because no support mailbox is configured.
- Contextual report actions on Dashboard, Usage Log, Analysis, Maintenance, Quick Entry, and failed backup imports.
- Local diagnostics logger with rotation, a 64 KiB per-file cap, redaction of common secrets/URIs/paths/identifiers, safe exception type/stack-frame logging without exception messages, and an uncaught-exception hook that delegates to Android's normal crash handler.
- Optional support screen with verified UPI ID `tejasmachchhar1@axl`, locally generated QR code, UPI chooser, copy button, and no fixed amount.
- A non-verifying thank-you dialog after returning from a UPI app, plus explicit statements that FleetPulse cannot confirm payment.
- Verified Buy Me a Coffee link. GitHub Sponsors stays hidden until a public Sponsors profile exists; the required configuration step is documented.
- Central configuration for repository, documentation, privacy, releases, issue labels, UPI, Buy Me a Coffee, Sponsors, and optional support email.
- New bug, feature-request, and general-feedback issue forms; category labels; README, user-guide, architecture, development, privacy, support, and release documentation updates.
- `FeedbackSupportTest` coverage for central links, UPI URI, report opt-outs, GitHub URL encoding and length capping, safe stack traces, categories, and redaction.

### Changed

- Version name is `4.2.0` and versionCode is `4`. Backup app label matches the current version.
- `buildConfig` generation is enabled for version/build metadata.
- ZXing core 3.5.3 is used only for local QR rendering.
- The More screen now exposes Feedback & Support and describes the expanded tools.

### Security

- No `INTERNET` permission, backend, analytics, crash-reporting SDK, payment SDK, or payment verification.
- Diagnostics remain local unless the user reviews the report, ticks consent, and chooses an external destination.
- No vehicle names/registrations, odometer values, fuel/charging records, expenses, notes, database contents, files, location, or credentials are included automatically.
- The manifest adds a `<queries>` declaration for the `upi` scheme, not a runtime permission.
- No tokens, payment secrets, or keystores are embedded in the APK.

### Verification

- `./gradlew :app:testDebugUnitTest` — 51 tests, 0 failures.
- `./gradlew :app:lintDebug` — 0 errors, 18 non-blocking dependency/legacy warnings.
- `./gradlew :app:assembleDebug` — debug APK produced with versionName 4.2.0 and versionCode 4.
- Feedback reports, UPI URI, redaction, and URL encoding are JVM-tested. GitHub, browser, share-sheet, email, and UPI hand-offs still require a device pass because the publication environment has no Android runtime.

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

[Unreleased]: https://github.com/tejasmachchhar/FleetPulse/compare/v5.0.0...HEAD
[5.0.0]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v5.0.0
[4.5.0]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.5.0
[4.4.0]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.4.0
[4.3.0]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.3.0
[4.2.0]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.2.0
[4.1.1]: https://github.com/tejasmachchhar/FleetPulse/releases/tag/v4.1.1
