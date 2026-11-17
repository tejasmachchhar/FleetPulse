# Privacy Policy

**Effective date:** 2026-11-17

FleetPulse is an offline-first Android application. This policy describes what the app does with data. It applies to the Android app and the source repository. It does not describe GitHub's own collection when you visit the repository; GitHub's privacy statement governs that.

## Summary

- FleetPulse has **no `INTERNET` permission**.
- There is no account, sign-in, sync, cloud backend, analytics, advertising, or tracking SDK.
- Vehicle, usage, maintenance, tariff, calibration, and settings data stay in the app's private local storage.
- Backups and CSV exports leave the app only when **you** choose a destination through Android's system file picker.
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

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | Optional local service reminders. You can deny it and use the app without notifications. |
| WorkManager merged permissions | Schedule and run the local daily reminder check. No network transmission by FleetPulse. |

The app does not request location, camera, microphone, contacts, SMS, call logs, or broad storage permissions. Backup/export uses Android's Storage Access Framework, which lets you choose a file and destination without granting broad storage access.

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

The Android app uses open-source AndroidX, Jetpack Compose, Room, WorkManager, Kotlin, and coroutine libraries. They run locally and do not add FleetPulse telemetry. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Legacy web prototype

`reference/FleetPulse-v4.html` is a historical single-file prototype included in the source repository. It is **not** built into or loaded by the Android app.

If you open that HTML file directly in a browser, it loads its old CDN dependencies (Google Fonts, Tailwind CSS, and Chart.js) and uses browser `localStorage` for its own state. Those requests and storage are browser behaviour, not FleetPulse Android runtime behaviour. The Android app has no network permission and never opens the file.

## Repository and issue reports

Do not put personal data in a public issue, pull request, screenshot, log, or test fixture. Use synthetic names, dates, and registration numbers. Security reports should use the private process in [SECURITY.md](SECURITY.md).

## Changes to this policy

Material changes will be documented in the repository and, where appropriate, in `CHANGELOG.md`. The effective date above identifies the current version. Because the app has no account or mailing list, there is no separate email notification.

## Contact

For privacy questions that do not contain personal data, open a repository issue. For security-sensitive reports, use the private advisory flow described in [SECURITY.md](SECURITY.md). Never send a token, keystore, real backup, or registration number in a public issue.
