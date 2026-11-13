# Contributing to Still Launcher

Thanks for wanting to improve Still Launcher. This project is deliberately small, offline and privacy-preserving; those constraints shape what belongs here.

## Ground rules

- **No network code.** The app declares no `INTERNET` permission. Do not add telemetry, crash reporting, remote config, ads or cloud features.
- **No new broad permissions without discussion.** Open an issue first if a feature needs accessibility, usage access, notification listener, overlay or secure settings.
- **No copied assets or strings.** Everything in this repository must be original or properly licensed. Do not paste code or artwork from another launcher.
- **Accessibility is not a toy.** The accessibility service exists to detect the foreground app and optional short-video surfaces. Do not extend it to read or transmit screen content.

## Development setup

Requirements:

- JDK 17
- Android SDK platform 36 and build-tools 36
- A device or emulator running Android 12 (API 32) or newer

```bash
git clone https://github.com/yash244466666/still-launcher.git
cd still-launcher
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant the optional permissions from **Settings → Permissions** inside the app. For monochrome:

```bash
adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS
```

## Pull requests

1. Keep each pull request focused on one problem.
2. Add or update unit tests when changing schedule, limit or formatting logic.
3. Run tests, lint and a debug build locally.
4. Describe how you tested the change and include a screenshot for UI changes.
5. Do not bump the version in `app/build.gradle.kts`; releases are cut with tags.

CI runs the same commands on every pull request. A pull request that adds network access or unjustified permissions will be closed.

## Commit style

Use short imperative subjects, for example:

```
fix: reset launcher navigation on HOME intent
feat: add per-app grayscale
docs: explain release signing
```

## Release process

Maintainers cut releases by pushing a semantic-version tag:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The release workflow builds a minified, signed APK and publishes it with SHA-256 sums. Every push to `main` also refreshes the rolling **Latest build** prerelease.

Signing secrets are documented in the README. Never commit a keystore or its passwords.

## Reporting security issues

Do not open a public issue for a vulnerability. See [SECURITY.md](SECURITY.md).
