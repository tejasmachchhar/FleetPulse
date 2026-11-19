# Support

FleetPulse is maintained as an open-source project. Support is best-effort; there is no paid support, account system, or response SLA.

## Where to ask

- **From the app** — open **More → Feedback & Support**. Choose Report a Bug, Suggest an Improvement, or Send General Feedback. FleetPulse prepares the report, shows a preview, and lets you remove optional details before opening GitHub, the Android share sheet, or the clipboard. No account is required inside FleetPulse.
- **Bug report** — open an issue using the bug report template.
- **Feature request** — open an issue using the feature request template.
- **General feedback** — use the general feedback template or the in-app action.
- **Usage question** — read [docs/USER_GUIDE.md](docs/USER_GUIDE.md) first, then open a question issue if needed.
- **Security issue** — do **not** open a public issue. Follow [SECURITY.md](SECURITY.md).

## Diagnostics and consent

The app can include app version, Android version, device model, screen size, language, current screen name, timestamp, and redacted recent error information. It does **not** collect vehicle names or registrations, odometer values, fuel/charging records, expenses, notes, database rows, files, location, or credentials.

- The exact report is shown before anything leaves FleetPulse.
- App, device/environment, and recent-diagnostics sections can each be disabled.
- A consent checkbox must be ticked before GitHub, share, copy, or email actions are enabled.
- Opening GitHub does not mean an issue was submitted. GitHub may require sign-in and you must press the final submit button.
- Diagnostics are stored only in the app's private storage, capped at two 64 KiB rotated files, and never uploaded automatically.
- Email is hidden because no support mailbox is currently configured. Do not send reports to an address you find elsewhere unless it is listed in this repository.

## Supporting FleetPulse

FleetPulse is free and fully usable without donating. Support is voluntary and does not unlock features, priority responses, or a support contract.

### UPI

- UPI ID: `tejasmachchhar1@axl`
- Use the in-app **Support FleetPulse** screen to copy the ID, scan the locally generated QR code, or open an installed UPI app.
- No fixed amount is set. Enter any amount in your UPI app.
- FleetPulse cannot verify whether a payment completed. The thank-you dialog is not a receipt. Check your UPI app for transaction status.
- FleetPulse does not read SMS or banking data and does not store payment credentials or transaction records.

### Buy Me a Coffee

The verified public page is [buymeacoffee.com/tejasmachchhar](https://buymeacoffee.com/tejasmachchhar). It opens externally in your browser.

### GitHub Sponsors

Not enabled yet. On 2026-11-19, `https://github.com/sponsors/tejasmachchhar` redirected to the normal GitHub profile and the GitHub API reported `hasSponsorsListing: false`. To enable it, the maintainer must create and publish a GitHub Sponsors profile, then set `SupportConfig.GITHUB_SPONSORS_URL` to the public sponsor URL and add it here. The app deliberately hides the button until that exists.

### Other providers

Razorpay or another provider is not configured. Do not add a provider until its account, payment URL, fees, payout requirements, and privacy implications are verified. A payment SDK or custom backend is not required for the current UPI/Buy Me a Coffee options.

## What to include

For a calculation or app issue:

- FleetPulse version (`versionName` and `versionCode` if known);
- Android version and device model;
- whether you installed a release, a debug build, or built from source;
- exact steps to reproduce;
- expected and actual result;
- a minimal synthetic example (names, dates, and registration numbers replaced);
- relevant screenshots only if they contain no personal data.

For a build issue:

- the command you ran;
- JDK, Gradle, Android SDK, and build-tools versions;
- the first meaningful error, not the whole log;
- whether the host is x86_64 or arm64.

## What not to post

Do not post:

- a real registration number, address, name, or notes;
- a real `fleetpulse.db`, JSON backup, or CSV;
- screenshots with personal data;
- tokens, keystores, passwords, or `.env` contents;
- logs containing private file paths or account details.

If you accidentally post a secret, rotate it immediately and ask for the content to be removed. Assume it is compromised.

## Common answers

- **No internet is required.** The app has no `INTERNET` permission.
- **No sync.** Export/import JSON is the transfer mechanism.
- **No OBD-II or GPS.** Entries are manual.
- **Battery wear is an allowance**, not a cash transaction.
- **Unavailable metrics are not zero.** The UI labels missing bases.
- **Debug APKs are not production-signed.** See [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md).

## Maintenance expectations

The repository uses Dependabot and GitHub Actions for routine checks. Releases are created from version tags. Device-only checks (notifications, SAF pickers, keyboard behaviour, screenshots) may remain pending when no Android runtime is available to the maintainer.

Thank you for helping keep reports reproducible and privacy-safe.
