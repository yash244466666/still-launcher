---
description: Cut a signed release from a semantic-version tag
argument-hint: <version, for example 0.2.1>
allowed-tools: Bash(./gradlew:*), Bash(git:*), Bash(gh:*), Read, Edit, Grep, Glob
---

Prepare and publish Still Launcher release `$ARGUMENTS`.

Guardrails:

- Never force-push or rewrite history unless the maintainer explicitly asks.
- Never commit a keystore, `keystore.properties`, `local.properties`, tokens or build outputs.
- Never add `Co-Authored-By`, assistant names or tool attribution to commits or release notes. The project has a human-only contributor list.
- Do not change the dependency stack to make a release pass.

Steps:

1. Confirm `$ARGUMENTS` is a semantic version without the leading `v` and that it is greater than the latest tag (`git tag --sort=-v:refname | head`).
2. Update `CHANGELOG.md` with the release date and user-visible changes.
3. Update the default `appVersionName` / `appVersionCode` in `app/build.gradle.kts` if the version line changed.
4. Run the full `/verify` protocol: tests, lint, debug build. Investigate failures instead of skipping them.
5. Review `git diff` and `git status`; commit with a conventional message and **no AI attribution**.
6. Push `main`, then create and push the tag:
   - `git push origin main`
   - `git tag v$ARGUMENTS`
   - `git push origin v$ARGUMENTS`
7. Watch the Release workflow (`gh run list`, `gh run watch <id> --exit-status`). It builds, signs, verifies and publishes the APK with `SHA256SUMS` and a gzipped mapping.
8. Verify the release with `gh release view v$ARGUMENTS`, download the APK and confirm `sha256sum -c SHA256SUMS` passes.
9. Report the release URL, asset names, checksum result and workflow status. If anything is not verified, say so.

Every push to `main` also refreshes the `latest` prerelease with a debug APK; that is expected.
