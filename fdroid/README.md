# F-Droid repository

This directory contains the metadata for a **self-hosted F-Droid repository** and the seed for an eventual official F-Droid submission.

## Public test repository

- Repository URL: `https://yash244466666.github.io/still-launcher/repo/`
- Repository fingerprint: `8C:16:3E:E1:19:37:A6:58:F9:65:07:57:0F:E0:23:98:3A:EC:32:16:26:6D:88:05:F7:C3:3C:82:D3:0F:A1:7D` (also written to `repo/fingerprint.txt` and printed by the workflow)

The repository is rebuilt from the latest stable GitHub release by `.github/workflows/fdroid-repo.yml` and published to the `gh-pages` branch. It serves our own signed release APKs; F-Droid only indexes and downloads them.

## Add it to F-Droid

1. Install the [F-Droid client](https://f-droid.org/F-Droid.apk).
2. Open **Settings → Repositories → Add repository**.
3. Paste the repository URL and the fingerprint from the workflow summary.
4. Search for **Still Launcher** and install.

Android 13+ may still require **App info → ⋮ → Allow restricted settings** before accessibility or notification access can be enabled. F-Droid does not bypass that system protection.

## How the repository is built

The workflow:

1. downloads the newest stable release APK,
2. decodes the repository signing keystore from repository secrets,
3. writes a temporary `config.yml` (never committed),
4. runs `fdroid update` with the Android SDK build-tools,
5. publishes `repo/` and a small landing page to `gh-pages`.

Required repository secrets:

- `FDROID_REPO_KEYSTORE_BASE64`
- `FDROID_REPO_KEYSTORE_PASSWORD`

The repository key is separate from the app signing key. Never commit either keystore or its password.

## Local reproduction

```bash
cd fdroid
# config.yml must point at a local keystore and the public repo URL
fdroid update --verbose
```

The generated `config.yml` and `repo/` are git-ignored. Only `metadata/` belongs in the source repository.

## Official F-Droid submission

The metadata in `metadata/org.stilllauncher.yml` is intentionally close to the format used by [fdroiddata](https://gitlab.com/fdroid/fdroiddata). Before submitting:

1. Add a `Builds:` section with the release tag, commit and Gradle build recipe.
2. Decide whether to keep `WRITE_SECURE_SETTINGS` in the main build. F-Droid reviewers may ask about grayscale requiring adb; document that it is optional.
3. Submit a Request for Packaging or a merge request to fdroiddata. This requires a GitLab account and F-Droid maintainer review; it is separate from this self-hosted repository.
4. Note that official F-Droid builds are signed by F-Droid with F-Droid's key, so users cannot switch between the self-hosted/GitHub APK and the official F-Droid APK without uninstalling.
