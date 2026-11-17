# Contributing to FleetPulse

Thanks for helping improve FleetPulse. The project is a local-first Android app with a pure-Kotlin calculation engine, so careful tests and honest data handling matter more than flashy changes.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to help

- Report a reproducible bug using the issue template.
- Suggest a focused feature with a clear user problem.
- Improve documentation, examples, or accessibility.
- Add tests for calculation, import, or schema edge cases.
- Fix a confirmed issue with a small, reviewable pull request.

Please do not open a public issue for a security vulnerability. Use the private process in [SECURITY.md](SECURITY.md).

## Before you start

- Search existing issues and pull requests.
- For a large change, open an issue first so the approach can be agreed.
- Keep the app offline-first. Do not add networking, analytics, ads, cloud accounts, or tracking.
- Do not include real vehicle records, registration numbers, addresses, backups, screenshots with personal data, tokens, or keystores in any issue or commit.

## Development setup

Requirements: JDK 17, Android SDK platform 35, build-tools 35.0.0, and Gradle 8.9 through the wrapper.

```bash
git clone https://github.com/tejasmachchhar/FleetPulse.git
cd FleetPulse
./scripts/verify.sh
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for platform notes, the arm64 AAPT2 workaround, schema changes, device testing, and screenshots.

## Pull requests

A good pull request:

- has one clear purpose;
- explains the user problem and the chosen solution;
- includes tests for calculation, parsing, import, or schema changes;
- updates the relevant docs and changelog for user-visible changes;
- does not include generated files, APKs, databases, backups, keystores, or secrets;
- preserves the app's offline-only privacy model;
- passes `./scripts/verify.sh`.

Use a Conventional Commit-style subject when practical:

```text
feat: add petrol amount helper
fix: keep focused field above the keyboard
docs: clarify provisional efficiency
test: cover legacy backup without eligibility
chore: bump Gradle wrapper
```

Keep commits focused. Do not force-push a shared branch without coordination.

### UI changes

If the change affects a screen:

- Check small screens and the keyboard-open state.
- Keep the edge-to-edge and `safeDrawing` rules described in the development guide.
- Add a genuine screenshot only if you captured it from the running app and sanitized it.
- Never add a mockup and call it a screenshot.

### Calculation changes

The calculation engine preserves reference parity. For a change:

1. Add a failing test with a minimal synthetic state.
2. Update the engine.
3. Update `VerificationSuite` if the reference case is intentional.
4. Update [docs/CALCULATION_LOGIC.md](docs/CALCULATION_LOGIC.md).
5. Explain any user-visible change to historical metrics.

Do not change reference values only to make a test pass. Explain why the old value was wrong and what migration or compatibility impact exists.

### Schema or backup changes

- Use a non-destructive Room migration.
- Export and commit the new schema JSON.
- Add a migration test.
- Keep older backups importable.
- Update [docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) and the release notes.

### Dependencies

- Prefer the AndroidX/Compose/Room/WorkManager stack already in use.
- Pin versions in `app/build.gradle.kts`; Dependabot will propose updates.
- Explain why a new dependency is necessary and its license.
- Do not add a dependency that introduces network access, telemetry, or a proprietary backend.

## Privacy and data in reports

Use the synthetic demo dataset when reporting a calculation issue. If you must describe a real record, replace names, registration numbers, dates, and notes with artificial values while preserving the numeric relationships.

Do not attach:

- a real `fleetpulse.db`;
- a real JSON backup;
- a CSV with personal records;
- screenshots showing names, registration numbers, notes, or locations;
- logs containing file paths or personal data;
- signing material or tokens.

## Issue templates

- **Bug report** — include app version, Android version, build source, exact steps, expected/actual, and a synthetic reproduction.
- **Feature request** — describe the problem first, then the proposed solution and alternatives.
- **Security** — private advisory only; see [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contribution is licensed under the repository's [MIT License](LICENSE). Do not submit code or assets you do not have the right to license.

## Questions

Use [SUPPORT.md](SUPPORT.md) for non-security support.
