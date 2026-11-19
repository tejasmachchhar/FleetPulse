# Development Guide

This guide is for contributors building, testing, and changing FleetPulse. Read [ARCHITECTURE.md](ARCHITECTURE.md), [CALCULATION_LOGIC.md](CALCULATION_LOGIC.md), and [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) before changing data or formulas.

## Toolchain

| Component | Required version |
|---|---|
| JDK | 17 |
| Gradle | 8.9 via the wrapper |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Android SDK platform | 35 |
| Android build-tools | 35.0.0 |
| `minSdk` | 26 |
| `compileSdk` / `targetSdk` | 35 |

Use a JDK 17 installation. Newer JDKs may work with Gradle 8.9, but they are not the tested configuration.

## First checkout

```bash
git clone https://github.com/tejasmachchhar/FleetPulse.git
cd FleetPulse
```

Create `local.properties` with your SDK path if Gradle cannot find it:

```properties
sdk.dir=/path/to/android-sdk
```

`local.properties` is ignored and must never be committed. Do not put signing passwords or tokens in it.

## Build and verify

```bash
# Everything used by CI
./scripts/verify.sh

# Or individually
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug

# Device/emulator install (requires adb and a connected target)
./gradlew :app:installDebug
```

Reports:

- Tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Do not commit `build/`, `dist/`, APKs, AABs, databases, exported backups, CSVs, or keystores. They are ignored by `.gitignore`.

## arm64 Linux hosts

The x86_64 AAPT2 binary bundled with AGP cannot run natively on arm64. On an arm64 host, point Gradle at a local wrapper from a machine-specific Gradle property outside the repository:

```properties
# ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/path/to/aapt2-wrapper
```

The wrapper must execute an amd64 AAPT2 with an amd64 sysroot through `qemu-x86_64-static` or an equivalent setup. Never add this override to the project `gradle.properties`; it is not portable.

## Memory-constrained hosts

`gradle.properties` is deliberately conservative:

```properties
org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx1024m
org.gradle.parallel=false
org.gradle.workers.max=2
org.gradle.caching=true
```

On a normal desktop, you can raise `-Xmx` and `org.gradle.workers.max` in your user-level `~/.gradle/gradle.properties`. Do not commit machine-specific memory tuning unless it is a deliberate project-wide change.

## Source layout

| Path | Responsibility |
|---|---|
| `app/src/main/java/com/fleetpulse/app/MainActivity.kt` | Single-activity Compose shell, nav, FAB, onboarding gate |
| `app/src/main/java/com/fleetpulse/app/FleetPulseApp.kt` | Application bootstrap, Room, DataStore, WorkManager |
| `app/src/main/java/com/fleetpulse/app/data/db/` | Entities, DAOs, database, migration |
| `app/src/main/java/com/fleetpulse/app/data/repo/` | Repository flows, demo data, transactional restore |
| `app/src/main/java/com/fleetpulse/app/data/backup/` | JSON v2, legacy import, CSV |
| `app/src/main/java/com/fleetpulse/app/data/prefs/` | DataStore preferences |
| `app/src/main/java/com/fleetpulse/app/domain/` | Pure calculation, parsing, verification, sample data |
| `app/src/main/java/com/fleetpulse/app/notifications/` | Daily reminder worker |
| `app/src/main/java/com/fleetpulse/app/support/` | Central links, UPI URI, report builders, diagnostics, redaction, external intents |
| `app/src/main/java/com/fleetpulse/app/ui/` | ViewModel, components, theme, screens |
| `app/src/test/java/` | JVM tests |
| `app/schemas/` | Exported Room schemas; tracked |
| `docs/` | Architecture, calculation, schema, release, user documentation |
| `reference/FleetPulse-v4.html` | Legacy prototype; not built or shipped |

## Code style

- Kotlin official style, 4-space indentation, trailing commas where the surrounding code uses them.
- Keep Compose functions small and stateless where practical. Pass data and callbacks down; do not open the database in a screen.
- Use Material 3 theme colours and existing components. Avoid hard-coded colours except the defined vehicle palette.
- Keep user-facing strings explicit and honest. If a value is unavailable, show `—` or a labelled unavailable state, not `0`.
- Do not introduce `INTERNET`, analytics, ads, or a network SDK.
- Keep external destinations in `support/SupportConfig.kt`; never scatter unverified URLs, UPI IDs, or payment links through screens. Optional values stay null and the UI hides them.
- Do not add a dependency for something the standard library or existing Compose/Room APIs can do.
- Never add automatic diagnostic upload, a crash-reporting SDK, or payment verification logic. Reports must remain previewed and consent-gated.
- Preserve the calculation engine's pure-Kotlin boundary. It must not import Android classes.
- Add KDoc for non-obvious formulas and state-machine transitions.
- Update the relevant docs when behaviour changes.

## Testing

The JVM suite is the main safety net:

```bash
./gradlew :app:testDebugUnitTest
```

Test suites:

- `CalcEngineTest` — workbook parity, full-to-full boundaries, eligibility, battery wear, maintenance, tariffs, calibration, periods.
- `BackupManagerTest` — JSON v2 round-trip, legacy v4 import, eligibility defaults, rejection of malformed input.
- `NumberTextTest` — editable-number parsing/formatting.
- `PetrolPricingTest` — two-of-three formulas and zero/non-finite input.
- `VerificationSuiteTest` — all in-app verification cases pass.
- `FeedbackSupportTest` — central link/UPI config, report opt-outs, GitHub URL encoding and length capping, categories, and redaction.

When fixing a calculation bug, add a failing test first that uses a minimal synthetic state. Do not use real vehicle records in tests.

When changing a feedback report, update `FeedbackSupportTest` and verify the preview still contains no vehicle/transaction fields. Do not embed a real GitHub token, email address, UPI PIN, or keystore in the app; external actions are intents only.

When changing backup JSON, test both:

1. a v2 round-trip; and
2. a legacy payload without the new field.

When changing the database schema, see [Schema changes](#schema-changes).

## Schema changes

1. Change the entity/DAO.
2. Increment `@Database(version = ...)`.
3. Add a `Migration(old, new)` with explicit SQL.
4. Register it in `FleetPulseApp`.
5. Run `./gradlew :app:kspDebugKotlin` or a normal build to export the new schema.
6. Commit the new `app/schemas/.../<version>.json`.
7. Add a migration test with `MigrationTestHelper` (recommended; not yet present).
8. Update [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) and the backup notes.
9. Never use destructive migration on upgrade. Destructive fallback is only configured for downgrade.

## Backup format changes

The current format is `"format": "fleetpulse-backup"`, `"version": 2`.

- Keep old files importable. If a field is optional, default it on import.
- Bump `BackupManager.VERSION` only when the change is incompatible or needs explicit versioning.
- Update `BackupManagerTest` with both new and legacy payloads.
- Update `README.md`, [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md), and [USER_GUIDE.md](USER_GUIDE.md).
- `BackupManager` writes an `"app"` label with the current Android version. Keep it in sync with `versionName`.

## UI and IME changes

The app is edge-to-edge. Keep these rules:

- `Scaffold` uses `WindowInsets.safeDrawing`.
- Full-screen dialogs use `safeDrawing` padding around the scrollable body.
- Form actions stay outside the scrolling body.
- Editable fields request `BringIntoViewRequester` when the IME inset changes.
- Do not use `Modifier.imePadding()` in multiple nested containers; it causes double padding.
- Test the form with the keyboard open on a small screen.

The current environment has no Android runtime, so IME changes are compile- and lint-verified only. A device pass is required before claiming visual success.

## Manual device testing

When a device or emulator is available:

1. Install the debug APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Complete onboarding twice: once clean, once with sample data.
3. Check Dashboard filters and period changes.
4. Add an EV charge in meter mode and voltage mode.
5. Add a petrol refill with each pair of the two-of-three inputs.
6. Add a partial charge/refill and verify it does not produce independent mileage.
7. Toggle `Include this entry for mileage estimation` and verify the denominator and boundary behaviour.
8. Edit and delete usage records.
9. Add/edit/archive/restore a vehicle; try deleting the last vehicle.
10. Add maintenance with date and km reminders; verify Overdue/Due Soon/Completed.
11. Enable notifications; wait for or trigger the worker; verify the notification opens Maintenance.
12. Export a JSON backup, modify the dataset, import, and confirm the preview and transactional restore.
13. Export both CSV files.
14. Remove sample data and verify genuine records survive.
15. Run the in-app Calculation Verification screen.
16. Open every long form with the keyboard and check the focused field remains visible.
17. Check light/dark/system themes and a small screen.
18. Open **More → Feedback & Support** and check every action and project link.
19. Create a bug report with only a category; verify diagnostics are filled in automatically and no vehicle/odometer/expense values appear.
20. Disable each diagnostic toggle and verify the preview omits that section.
21. Verify the consent checkbox is required before GitHub/share/copy/email; cancel and confirm no external app opens.
22. Open the GitHub URL and confirm it is prefilled; do not treat the browser opening as a submitted issue.
23. Use Share and Copy; confirm the clipboard text matches the preview.
24. Open contextual bug icons from Dashboard, Usage Log, Analysis, Maintenance, and Quick Entry; confirm only the screen label is prefilled.
25. On **Support FleetPulse**, copy the UPI ID, scan the QR with another device, open the UPI chooser, and confirm the thank-you dialog does not claim payment success. Test on a device with no UPI app if possible.
26. Verify Buy Me a Coffee opens externally, GitHub Sponsors is hidden while unconfigured, and no email button appears while `SUPPORT_EMAIL` is null.

## Device testing and screenshots

Screenshots must come from the real app. Do not create mockups or label a web prototype screenshot as the Android UI. A Robolectric/Compose JVM render is not available on the current Linux aarch64 host because Robolectric's native runtime does not support that platform; leave screenshots pending rather than substituting generated images.

```bash
# Start the app on the connected device first
./scripts/capture-screenshots.sh docs/screenshots
```

The helper uses `adb exec-out screencap -p` and writes:

- `01-dashboard.png`
- `02-vehicles.png`
- `03-usage-log.png`
- `04-analysis.png`
- `05-maintenance.png`
- `06-settings.png`
- `07-feedback-hub.png`
- `08-bug-report.png`
- `09-report-preview.png`
- `10-support-fleetpulse.png`

Navigate to each screen before capturing the corresponding file, or adapt the script for your device automation. Review the images for personal data before committing. Use the synthetic demo dataset.

If no device is available, leave the screenshot section honest and pending. Do not add fake images.

## Debugging

- Use Android Studio's Logcat for runtime issues.
- The app has no network calls; there is no remote logging.
- To inspect the database on a debug build, use Android Studio's Database Inspector or `adb exec-out run-as com.fleetpulse.app cat databases/fleetpulse.db > /tmp/fleetpulse.db`. Treat the extracted database as personal data and delete it afterwards.
- To inspect DataStore, use the Device File Explorer under `files/datastore/`.
- To reset the app, clear app storage or uninstall/reinstall. This deletes the local database and preferences.

## Common problems

### `SDK location not found`

Create `local.properties` with `sdk.dir=...` or set `ANDROID_HOME`. Do not commit the file.

### AAPT2 fails on arm64

Use the local `android.aapt2FromMavenOverride` workaround described above. Do not replace project binaries.

### Gradle runs out of memory

Raise memory in your user-level `~/.gradle/gradle.properties` or stop other builds. The project defaults are tuned for a phone-hosted environment.

### Tests pass but the UI is untested

The JVM suite does not exercise Compose, Room integration, SAF pickers, notifications, or IME behaviour. Those need an emulator or device.

### The release build is unsigned

This is expected when signing environment variables are absent. See [RELEASE_PROCESS.md](RELEASE_PROCESS.md). Never label an unsigned/debug artifact as a signed production release.

## Before opening a pull request

```bash
./scripts/verify.sh
```

Also:

- Review `git diff` for accidental personal data, generated files, and secrets.
- Update tests and docs.
- Keep the change focused.
- Follow [CONTRIBUTING.md](../CONTRIBUTING.md).
