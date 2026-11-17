# Support

FleetPulse is maintained as an open-source project. Support is best-effort; there is no paid support, account system, or response SLA.

## Where to ask

- **Bug report** — open an issue using the bug report template.
- **Feature request** — open an issue using the feature request template.
- **Usage question** — read [docs/USER_GUIDE.md](docs/USER_GUIDE.md) first, then open a question issue if needed.
- **Security issue** — do **not** open a public issue. Follow [SECURITY.md](SECURITY.md).

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
