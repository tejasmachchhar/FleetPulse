# Privacy Policy

**Effective date:** 2026-11-19

FleetPulse is an offline-first Android application. This policy describes what the app does with data. It applies to the Android app and the source repository. It does not describe GitHub's own collection when you visit the repository; GitHub's privacy statement governs that.

## Summary

- FleetPulse has **no `INTERNET` permission**.
- There is no account, sign-in, sync, cloud backend, analytics, advertising, or tracking SDK.
- Vehicle, usage, maintenance, tariff, calibration, and settings data stay in the app's private local storage.
- Backups and CSV exports leave the app only when **you** choose a destination through Android's system file picker.
- Feedback reports are prepared locally. The exact text is previewed, optional diagnostics can be removed, and nothing is sent until you choose GitHub, the Android share sheet, copy, or a configured email app and confirm consent.
- Local diagnostics are capped, rotated, and never uploaded automatically.
- Android auto-backup is disabled.

## What the app stores

The app stores the information you enter:

- vehicle names, types, registration numbers, purchase dates, odometers, notes, and colour;
- EV battery capacity, replacement rate, expected life, cycles, rated km/charge, and daily km plan;
- petrol tank capacity;
- usage dates, odometers, fill types, kWh, litres, voltages, unit prices, and remarks;
- maintenance dates, odometers, categories, descriptions, amounts, next-due dates/km, and remarks;
- historical electricity and petrol tariffs;
- calibration points and notes;
- calculation/validation settings and maintenance categories;
- theme, onboarding completion, and notification preference.

The app does not ask for your name, email address, phone number, contacts, location, photos, microphone, camera, or advertising identifier.

## Where data is stored

- The main dataset is stored in a Room database named `fleetpulse.db` in the app's private internal storage.
- UI/onboarding preferences are stored in AndroidX DataStore under the app's private files.
- No FleetPulse server receives the data.

The database is not encrypted with a separate app password. It relies on the Android app sandbox and the device's storage encryption. A rooted device or a full device backup could expose app-private data, as with most local Android apps.

## Network access

The Android app declares no `INTERNET` permission and makes no HTTP requests. It does not use:

- analytics or crash reporting;
- advertising or attribution SDKs;
- cloud storage or sync;
- push notifications;
- location services;
- remote logging.

WorkManager contributes local scheduling permissions to the merged manifest (for example `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`). They are used to run the daily local reminder check; FleetPulse does not transmit data over a network.

The feedback and support screens use Android intents to hand an explicit action to another app: a browser for GitHub, the Android share sheet, a clipboard, an email app only when a support address is configured, a UPI app, or the Buy Me a Coffee page. Those apps may have network access and their own privacy policies. FleetPulse still has no `INTERNET` permission and does not receive the contents of a GitHub issue, a payment result, or a third-party page.

## Feedback, diagnostics, and support

### What a report can contain

When you open **More → Feedback & Support → Report a Bug**, the app can prepare:

- the category and optional description you choose;
- the screen or navigation context, such as `Dashboard` or `Usage entry · EV charge`;
- FleetPulse version name, version code, and build type;
- Android version, API level, device manufacturer and model;
- screen size in dp, density, app language, and timestamp;
- redacted error type, stack frames, and recent local diagnostics, only when that toggle remains on.

The report does **not** include vehicle names or registration numbers, odometer readings, fuel/charging records, maintenance expenses, user-entered notes, database contents, personal files, location, account credentials, or advertising identifiers.

### Local diagnostics log

- The log is stored in the app's private `files/diagnostics` directory as `fleetpulse-diagnostics.log` with one rotated `.1` file.
- Each file is capped at 64 KiB. The logger rotates or removes older content instead of growing without limit.
- It records timestamps, component names, app startup, and exception type/stack frames. Exception messages are not written, because a parser or file error can contain record labels or values. It is not a general activity log and does not record vehicle or expense values.
- Redaction is best-effort: it removes common email addresses, registration-like values, file/content URIs, file paths, long numbers, and secret-like `token=...` patterns from any text written to the log.
- The log is read into a report only when **Recent diagnostics** is enabled. You can disable it and still submit the category and description.
- FleetPulse never uploads the log automatically, never sends it to a backend, and has no crash-reporting SDK.

### Your choices before sharing

The review dialog shows the exact report. A checkbox must be ticked before the GitHub, share, copy, or email actions become available. You can cancel, go back, and remove app/device/log sections first. Opening GitHub does not create an issue; GitHub may require sign-in, and only you can press the final submit button. FleetPulse does not claim an issue was submitted just because a browser opened.

### Donations

- UPI support uses the verified ID `tejasmachchhar1@axl`. The QR code is generated locally; FleetPulse does not call a payment server.
- The UPI chooser hands the payment URI to an installed UPI app. FleetPulse does not read SMS, bank data, or UPI credentials, and does not store transaction details.
- A thank-you dialog is only an expression of appreciation. It does not verify that a payment occurred.
- Buy Me a Coffee opens its verified public page externally.
- GitHub Sponsors is disabled until a public Sponsors profile exists.
- No Razorpay or other payment SDK/backend is included.

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | Optional local service reminders. You can deny it and use the app without notifications. |
| WorkManager merged permissions | Schedule and run the local daily reminder check. No network transmission by FleetPulse. |

The app does not request location, camera, microphone, contacts, SMS, call logs, or broad storage permissions. Backup/export uses Android's Storage Access Framework, which lets you choose a file and destination without granting broad storage access.

The manifest contains a `<queries>` declaration for the `upi` scheme so the app can check whether a UPI app is available on Android 11+. This is a package-visibility declaration, not a runtime permission, and grants no access to payment data.

## Exports and backups

When you export a JSON backup or CSV:

- the app creates the content only after you start the export;
- Android's system file picker asks where to put it;
- the resulting file is outside the app's private storage and is protected by the destination and your device's file permissions;
- FleetPulse does not upload the file anywhere.

Treat exported files as personal data. Store them securely and delete copies you no longer need.

## Notifications

Service reminders are generated locally by WorkManager. A notification may show a vehicle name, maintenance category, due date, and due odometer. Notifications are posted only when the preference is enabled, permission is granted, and the vehicle is not archived. Tapping a notification opens the Maintenance screen.

## Retention and deletion

- Data remains until you delete it in the app, clear the app's storage, or uninstall the app.
- Deleting a vehicle removes its usage, maintenance, and calibration records through database cascade rules, after confirmation.
- Removing sample data deletes only records flagged `isSample`; genuine records are preserved.
- Uninstalling the app deletes the local database and preferences. Android auto-backup is disabled, so they are not restored from Google's backup service.
- Export a backup before uninstalling if you want to keep the data.

## Children

FleetPulse is a general-purpose record-keeping tool and is not directed at children. It does not knowingly collect personal information from children because it does not collect data centrally at all.

## Third-party components

The Android app uses open-source AndroidX, Jetpack Compose, Room, WorkManager, Kotlin, coroutine libraries, and ZXing core for local QR generation. They run locally and do not add FleetPulse telemetry. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Legacy web prototype

`reference/FleetPulse-v4.html` is a historical single-file prototype included in the source repository. It is **not** built into or loaded by the Android app.

If you open that HTML file directly in a browser, it loads its old CDN dependencies (Google Fonts, Tailwind CSS, and Chart.js) and uses browser `localStorage` for its own state. Those requests and storage are browser behaviour, not FleetPulse Android runtime behaviour. The Android app has no network permission and never opens the file.

## Repository and issue reports

Do not put personal data in a public issue, pull request, screenshot, log, or test fixture. Use synthetic names, dates, and registration numbers. If you use the in-app report preview, remove anything you do not want public before opening GitHub or sharing the text. Security reports should use the private process in [SECURITY.md](SECURITY.md).

## Changes to this policy

Material changes will be documented in the repository and, where appropriate, in `CHANGELOG.md`. The effective date above identifies the current version. Because the app has no account or mailing list, there is no separate email notification.

## Contact

For privacy questions that do not contain personal data, open a repository issue. For security-sensitive reports, use the private advisory flow described in [SECURITY.md](SECURITY.md). Never send a token, keystore, real backup, or registration number in a public issue.
