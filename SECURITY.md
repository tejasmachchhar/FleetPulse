# Security Policy

## Supported versions

The latest published release and the current `main` branch receive security fixes.

| Version | Supported |
|---|---|
| 4.1.x | Yes |
| Older private iterations | No public support |

FleetPulse is a local-first Android app. It does not provide a server, account system, or network API.

## Reporting a vulnerability

**Do not open a public issue for a security vulnerability.**

Use GitHub's private security advisory flow:

1. Open the repository's **Security** tab.
2. Choose **Advisories** → **New draft security advisory**.
3. Describe the issue privately.

If private advisories are unavailable, contact the maintainer through the repository's private contact options. Do not post exploit details, a real database, a backup file, personal records, a token, or a signing key in a public issue.

Please include:

- affected version and commit;
- Android version and device type (if relevant);
- a minimal reproduction with synthetic data;
- impact and attack prerequisites;
- any suggested fix;
- whether the issue is already public.

## Scope

Examples of in-scope issues:

- a path traversal, file overwrite, or unexpected file access in import/export;
- processing a malicious backup that causes code execution, data loss, or a crash outside normal validation;
- an exported file being written somewhere the user did not choose;
- a permission or manifest mistake that exposes data or enables network access;
- a release artifact that is mislabeled, unsigned, or differs from its checksum;
- a leaked secret, keystore, or private record in the repository history.

Examples of out-of-scope reports:

- the app cannot sync between devices (by design);
- a rooted device can read the app's private database (platform trust model; documented);
- an estimate is not financially exact;
- a third-party library issue that does not affect FleetPulse;
- social engineering, spam, or denial-of-service against GitHub itself.

## Security model

- The Android app declares no `INTERNET` permission.
- Android auto-backup is disabled (`android:allowBackup="false"`).
- Data is stored in the app's private Room database and DataStore files.
- The database is not application-encrypted; it relies on the Android app sandbox and device storage encryption.
- Backups and CSV exports leave the app only through user-selected Storage Access Framework destinations.
- WorkManager adds local scheduling permissions, not network access.
- The legacy file under `reference/` is not built or loaded by the Android app. If opened directly in a browser, it uses its old CDN assets and browser storage; that is outside the Android app's runtime.
- Release signing is optional and environment-driven. Without all signing secrets, the release workflow publishes a clearly labelled debug development build, never a fake signed release.

## Secrets and personal data

Never commit:

- `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, private keys, or base64 keystores;
- GitHub tokens, API keys, passwords, or `.env` files;
- `local.properties`;
- a real `fleetpulse.db`, JSON backup, or CSV;
- screenshots or logs containing a registration number, address, name, or notes.

If you believe a secret was published, rotate it immediately and report privately. Removing a commit from the tip is not sufficient; assume the value is compromised.

## Release verification

For a release artifact:

1. Download the APK/AAB and `SHA256SUMS` from the same GitHub Release.
2. Verify the checksum:
   ```bash
   sha256sum -c SHA256SUMS
   ```
3. Verify the signature:
   ```bash
   apksigner verify --verbose --print-certs FleetPulse-<version>-release.apk
   ```
4. Confirm the release is not marked as a debug/development prerelease if it claims to be production-signed.

A debug APK is debug-signed. Do not treat it as a production release.

## Disclosure

The maintainer will acknowledge a private report as soon as practical, investigate, and coordinate a fix and disclosure. No response SLA is promised. Please allow reasonable time before public disclosure.

## Credits

Responsible reports can be credited in the advisory and release notes if you wish.
