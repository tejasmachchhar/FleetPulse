# Release Process

FleetPulse uses Semantic Versioning for the public version name and a separate, monotonically increasing Android `versionCode`. The current baseline is:

```text
versionName = "4.2.0"
versionCode = 4
```

The first public tag should be `v4.1.1`. Do not invent older release tags or back-date history that was not published.

## Versioning rules

- **MAJOR** — incompatible data/behaviour changes that require a migration or user action.
- **MINOR** — new user-facing capability, backwards-compatible.
- **PATCH** — bug fix, documentation-only release, or a rebuild with no breaking data change.
- `versionCode` must increase by at least one for every published build. Never reuse a code.
- The Git tag is `v` + `versionName`, for example `v4.2.0`.
- Pre-release tags may use a suffix, for example `v4.3.0-rc.1`. The release workflow accepts a suffix on the current `versionName`.
- A release tag is immutable. Never move, delete, or force-push a tag after publication. Ship a patch instead.

## What the automation does

### Android CI — `.github/workflows/android-ci.yml`

Triggers:

- push to `main`
- pull request targeting `main`
- manual dispatch

It runs:

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

It uploads the test/lint reports and the debug APK as workflow artifacts. It has read-only repository permissions.

### Release — `.github/workflows/release.yml`

Triggers:

- push of a tag matching `v*`
- manual dispatch with an existing tag

It:

1. Checks out the tagged revision.
2. validates the tag shape and that it matches `app/build.gradle.kts` `versionName`;
3. detects whether the four signing secrets are present;
4. runs tests and lint;
5. builds a signed release APK/AAB when signing is configured;
6. otherwise builds the debug APK and labels the release as a **development debug build**;
7. verifies the APK signature with `apksigner`;
8. creates `SHA256SUMS`;
9. extracts the matching `CHANGELOG.md` section;
10. publishes the GitHub Release with `gh release create --verify-tag`.

A debug build is published as a prerelease and is never described as a signed production release.

## Signing

Signing is optional for development and mandatory for a real production release.

### Required GitHub Actions secrets

Set these in **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `FLEETPULSE_KEYSTORE_BASE64` | Base64 of the release JKS/P12 keystore |
| `FLEETPULSE_KEYSTORE_PASSWORD` | Keystore password |
| `FLEETPULSE_KEY_ALIAS` | Key alias |
| `FLEETPULSE_KEY_PASSWORD` | Key password |

The workflow decodes the keystore to `$RUNNER_TEMP/fleetpulse-release.jks` and exports these environment variables for Gradle:

```text
FLEETPULSE_KEYSTORE_FILE
FLEETPULSE_KEYSTORE_PASSWORD
FLEETPULSE_KEY_ALIAS
FLEETPULSE_KEY_PASSWORD
```

`app/build.gradle.kts` only creates the `release` signing config when all four are present and the keystore file exists. If any is missing, the release variant remains unsigned.

### Creating a keystore locally

Use a strong password and store the keystore outside the repository:

```bash
keytool -genkeypair -v \
  -keystore ~/secure/fleetpulse-release.jks \
  -alias fleetpulse \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Base64-encode it for the GitHub secret:

```bash
base64 -w0 ~/secure/fleetpulse-release.jks > /tmp/fleetpulse-release.jks.b64
```

Upload the contents of the `.b64` file to `FLEETPULSE_KEYSTORE_BASE64`. Delete temporary files afterwards.

**Never commit** a `.jks`, `.keystore`, `.p12`, password, or base64 keystore. They are ignored by `.gitignore` and must stay outside the repository.

### Local signed build

```bash
export FLEETPULSE_KEYSTORE_FILE="$HOME/secure/fleetpulse-release.jks"
export FLEETPULSE_KEYSTORE_PASSWORD='...'
export FLEETPULSE_KEY_ALIAS='fleetpulse'
export FLEETPULSE_KEY_PASSWORD='...'
./gradlew :app:assembleRelease :app:bundleRelease
```

Verify:

```bash
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Do not echo passwords into shell history or CI logs.

## Pre-release checklist

- [ ] `versionCode` increased and `versionName` updated in `app/build.gradle.kts`.
- [ ] `BackupManager` `"app"` version label updated if `versionName` changed.
- [ ] `CHANGELOG.md` has a dated section for the version with real changes only.
- [ ] README, user guide, calculation docs, and schema docs reflect the change.
- [ ] `app/schemas/` contains the new schema JSON if the database changed.
- [ ] Migration is non-destructive and tested.
- [ ] No APK, AAB, database, backup, CSV, keystore, `.env`, or `local.properties` is tracked.
- [ ] No real registration number, personal record, token, or private backup is tracked.
- [ ] `./scripts/verify.sh` passes.
- [ ] Device acceptance pass completed, or the limitation is stated honestly.
- [ ] Screenshots, if added, are genuine and sanitized.
- [ ] Release notes describe only shipped changes.
- [ ] Signing secrets are configured, or the release is intentionally a debug prerelease.
- [ ] Tag matches `versionName`.

## Publishing a release

```bash
# From a clean main branch after the release commit
git switch main
git pull --ff-only
./scripts/verify.sh

git tag -a v4.2.0 -m "FleetPulse 4.2.0"
git push origin main
git push origin v4.2.0
```

The tag push starts the release workflow. Do not force-push. If the workflow fails before publishing, fix the cause and push a new tag only if the old tag was never published. Once a GitHub Release exists, publish a patch version instead.

Manual dispatch is available for an existing tag. Enter the tag exactly, for example `v4.2.0`; the workflow checks that it exists and matches the app version.

## Post-release verification

```bash
gh release view v4.2.0
gh release download v4.2.0 --dir /tmp/fleetpulse-release
cd /tmp/fleetpulse-release
sha256sum -c SHA256SUMS

"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs \
  FleetPulse-4.2.0-release.apk
```

Then install on a real device and complete the acceptance checklist in [DEVELOPMENT.md](DEVELOPMENT.md#manual-device-testing).

Check:

- Release is marked as a prerelease only when it is a debug/development build.
- The APK/AAB filenames and checksums match the release notes.
- The release notes match `CHANGELOG.md`.
- The workflow run is green and its artifacts are present.
- The repository README badge and release link resolve.

## Debug/development releases

When signing secrets are unavailable, the workflow publishes:

```text
FleetPulse-<version>-debug.apk
SHA256SUMS
```

It marks the GitHub Release as a prerelease and includes a prominent warning that the APK is debug-signed, not a production/Play Store build. This is an honest development artifact, not a fake signed release.

## Rollback policy

- Do not rewrite or delete a published tag.
- If a release is wrong but not dangerous, publish a patch with corrected notes and artifacts.
- If an artifact is malicious or must be withdrawn, edit the release to remove the unsafe asset and clearly explain the issue; then publish a patch.
- Never force-push `main` or a tag without the maintainer's explicit approval.

## CI and release troubleshooting

### `Tag does not match versionName`

Update `versionName` in `app/build.gradle.kts`, commit, and create the tag again only if it was never pushed. If the tag already exists, publish the next patch.

### `app-release.apk` missing

The release variant may have failed to assemble. Check the Gradle log for minification/ProGuard errors. Do not silently fall back to a debug artifact in a signed release run.

### `apksigner` not found

The workflow installs `build-tools;35.0.0` through `android-actions/setup-android`. Check the SDK setup step.

### Signing secrets present but APK unsigned

Confirm all four secrets exist and are non-empty, the alias matches the key, and the keystore password is correct. The Gradle config only activates when all four are present and the decoded file exists.

### Release already exists

`gh release create` fails if the release exists. Do not delete and recreate a published release casually; publish a patch. If the previous attempt failed before publishing, verify with `gh release view <tag>` first.

### CI cannot find the Android SDK

The workflows install `platform-tools`, `platforms;android-35`, and `build-tools;35.0.0`. Check the setup step and the runner image. Locally, set `ANDROID_HOME` or `local.properties`.
