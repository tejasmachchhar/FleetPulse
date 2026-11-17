# Pull Request

## What changed

<!-- A short, specific summary. -->

## Why

<!-- The user problem or maintenance need. -->

## Type of change

- [ ] Bug fix
- [ ] Feature
- [ ] Calculation or data change
- [ ] Documentation
- [ ] Build, CI, or dependency update
- [ ] Refactor with no user-visible behaviour change

## Checklist

- [ ] I read [CONTRIBUTING.md](../CONTRIBUTING.md).
- [ ] I ran `./scripts/verify.sh` and it passes.
- [ ] I added or updated tests for calculation, parsing, backup, or schema changes.
- [ ] I updated the relevant docs and `CHANGELOG.md` for user-visible changes.
- [ ] I did not add networking, analytics, ads, cloud accounts, or tracking.
- [ ] I did not include a real database, backup, CSV, registration number, personal record, token, keystore, or secret.
- [ ] Screenshots, if included, came from the running app, use synthetic data, and are not mockups.
- [ ] If the database schema changed, the new schema JSON is committed and the migration is non-destructive.
- [ ] If the release version changed, `versionCode` is monotonic and `versionName` matches the planned tag.

## Verification

<!-- Paste the commands and a short result. Do not paste secrets or personal data. -->

```text
./scripts/verify.sh
```

## Screenshots or recordings

<!-- Optional. Model-generated images and mockups must be labelled as such. -->

## Related issues

<!-- Fixes #... or Relates to #... -->
