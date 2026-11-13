# GEMINI.md — Gemini CLI guide for Still Launcher

This file is the entry point for Gemini CLI. It covers the project facts, commands, constraints and verification expected from an agent. For deeper task recipes read [AGENT.md](AGENT.md); for Claude Code-specific conventions read [CLAUDE.md](CLAUDE.md). All three must remain consistent.

## Project summary

Still Launcher is an independent, GPL-3.0, text-first Android launcher for digital wellbeing. It replaces an icon grid with a name list and adds optional focus tools: timed app blocking, schedules, daily limits, per-app grayscale, notification filtering, screen-time statistics, a recents section, an alphabetic index bar and a background-app shortcut.

- Package: `org.stilllauncher`
- Version line: `0.2.1`
- minSdk 32 / targetSdk 36 / compileSdk 36
- Kotlin 2.2.21, AGP 8.13.2, Gradle 8.13, JDK 17, Compose BOM 2025.04.01
- Single-activity Compose; no XML layouts, no Navigation library, no database, no DI framework
- 14 JVM tests, lint clean, emulator smoke-tested
- Repository: https://github.com/yash244466666/still-launcher

## Hard constraints

1. **Offline only.** Never add `INTERNET`, HTTP clients, analytics, ads, accounts, remote config or crash reporting. CI rejects an `INTERNET` permission.
2. **Privacy.** Accessibility is only for foreground-package detection and optional short-video view IDs. Do not read, store, log or transmit screen content. Notification text may appear only in the local in-memory “filtered recently” list.
3. **No AI attribution.** Do not add `Co-Authored-By`, “Generated with”, assistant names or tool names to commits or pull requests. The project’s contributor list is intentionally human-only. This rule overrides default agent behaviour.
4. **No copied material.** Everything must be original. Do not import code, strings, drawables, icons or branding from another launcher.
5. **No destructive operations** without explicit maintainer approval: force-push, tag rewriting, release deletion, keystore rotation.
6. **No anti-user enforcement.** No kiosk, device owner, device admin, anti-uninstall or hidden persistence. Revoking permissions or force-stopping must disable the guard.
7. **Keep dependencies minimal.** Compose, AndroidX, DataStore and kotlinx.serialization only. Ask before adding Room, Hilt, Navigation, Retrofit, WorkManager or icon packs.

## First actions in a session

1. Read this file and `AGENT.md`.
2. Inspect `README.md`, `docs/ARCHITECTURE.md`, `CHANGELOG.md` and the files relevant to the task.
3. Run the baseline checks before editing.
4. Plan the smallest coherent change; do not restructure the app to fit a tool.
5. Implement, test, lint, build, and report exactly what was verified.

## Build and test commands

Run non-interactively; do not use commands that require a TTY.

```bash
./gradlew :app:testDebugUnitTest     # 14 tests expected
./gradlew :app:lintDebug             # must succeed
./gradlew :app:assembleDebug         # debug APK
./gradlew clean :app:assembleDebug   # clean build when needed
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Signed release locally (maintainer only; keystore is outside the repo):

```bash
SIGNING_KEYSTORE_PATH=/path/to/release.jks \
SIGNING_KEYSTORE_PASSWORD=... \
SIGNING_KEY_ALIAS=... \
SIGNING_KEY_PASSWORD=... \
./gradlew :app:assembleRelease -PversionName=0.2.1 -PversionCode=3
```

Never echo secrets. Never commit a keystore or `keystore.properties`.

## Environment

```bash
git clone https://github.com/yash244466666/still-launcher.git
cd still-launcher
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

JDK 17 and Android SDK platform 36 / build-tools 36 are required. `local.properties`, build outputs, `dist/` and keystores are git-ignored.

## Source map

```
app/src/main/java/org/stilllauncher/
  MainActivity.kt          activity, HOME intent, nav reset, system bars
  StillViewModel.kt        settings and app-list mutations
  data/Models.kt           models, Settings, schedules, addRecentApp
  data/SettingsStore.kt    DataStore keys, JSON persistence
  data/AppRepository.kt    launcher app list and intents
  data/UsageRepository.kt  usage access, foreground package, intervals
  service/MonitorEngine.kt recents, grayscale, block/limit decisions
  service/MonitorService.kt 2-second foreground poll
  service/AppAccessibilityService.kt immediate events + global actions
  service/NotificationFilterService.kt allow-list filtering
  service/ShortVideoDetector.kt optional Shorts/Reels probes
  ui/                      Compose screens, dialogs, theme
  ui/HelpScreen.kt         Play Protect and restricted-settings troubleshooting
  util/AppSigning.kt       installed certificate fingerprint
```

Key docs: `docs/ARCHITECTURE.md`, `CLAUDE.md`, `AGENT.md`, `.github/workflows/`.

## Installation support

Users sideload from GitHub, so expect two Android behaviours:

- **Play Protect warning.** Self-distributed, signed APKs with accessibility trigger an “unknown developer” heuristic. Never publish debug APKs; the rolling `latest` workflow must use `assembleRelease` with signing secrets. Tell users to choose **More details → Install anyway** or use `adb install -r <apk>`. Never suggest permanently disabling Play Protect.
- **Android 13+ restricted settings.** Accessibility and notification access are greyed out until **Settings → Apps → Still Launcher → ⋮ → Allow restricted settings**. The app cannot request this; guide users to it. The built-in **Settings → Install & access help** screen has the exact steps and direct settings buttons.

Official release certificate SHA-256:

```
af841912594162f29ea85a12b2bb8347c016a938fb31ccfa0fdc4e3812c35def
```

Battery optimisation opens the system list; do not add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

A self-hosted F-Droid test repository is available at `https://yash244466666.github.io/still-launcher/repo/` with fingerprint `8C:16:3E:E1:19:37:A6:58:F9:65:07:57:0F:E0:23:98:3A:EC:32:16:26:6D:88:05:F7:C3:3C:82:D3:0F:A1:7D`. The `F-Droid repo` workflow rebuilds it from the newest stable release. Official F-Droid submission is a later, separate process requiring a GitLab merge request and a build recipe.

## Architecture rules to preserve

- All screen state is Compose state or `StateFlow` collected with `collectAsStateWithLifecycle()`. Do not read `StateFlow.value` during composition; lint rejects it.
- `SettingsStore` is the only persistence layer. Collections are JSON via kotlinx.serialization.
- `StillViewModel` owns UI-initiated mutations. `MonitorEngine` owns enforcement-originated writes and updates its in-memory settings first.
- Enforcement order is direct block → schedule → limit → cooldown. Keep the debounce.
- `BlockedActivity` and `ReminderActivity` are `exported="false"`; test them through `MonitorEngine`, not `adb am start`.
- Keep `SYSTEM_ALERT_WINDOW` and the accessibility-service path; Android otherwise blocks background activity launches.
- The app list uses typed entries with unique IDs. Letter-entry indices power the alphabetic index bar.
- Recents are capped at eight; five are shown in the app menu. Android has no global running-app API, so the home indicator shows the last foreground app.

## Common changes

### Add a setting

`Models.kt` field → `SettingsStore.K` key + decode + setter → `StillViewModel` action → settings UI → enforcement if relevant → README/changelog.

### Add a screen

Add a `Route` and branch in `StillApp.kt`; build with `ScreenScaffold`; navigate via callbacks. Do not add a second Activity.

### Add an app-list section

Add a `ListEntry` subtype with a unique ID, append it in `AllAppsScreen`’s `buildList`, add rendering in the `when`, verify search and empty states. Lettered sections must use `LetterEntry` so the index bar stays correct.

### Change blocking or limits

Edit `MonitorEngine.onForeground`; keep both foreground feeds calling it; preserve precedence and debounce; update schedule tests if time logic changes; verify on an emulator.

### Work on recents

Ordering is `addRecentApp` in `Models.kt` (test it); recording is `MonitorEngine.recordRecent` (launchable filter, ten-minute throttle); clearing is `SettingsStore.setRecentApps(emptyList())` through the ViewModel.

## Verification

Minimum before claiming completion:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

For UI/enforcement changes, also run an emulator/device smoke test:

1. Install and complete onboarding.
2. Grant usage access + accessibility via Settings → Permissions or adb.
3. Open a third-party app, press Home, verify the background chip and **Return**.
4. Open **All apps**, verify Recent apps, drag the index bar, verify the jump.
5. Tap **Recents**, verify Android’s recents screen.
6. Block an app, open it, verify the block screen, end the block.
7. Switch theme/font and verify contrast and system bar icons.

Useful adb setup:

```bash
adb shell appops set org.stilllauncher GET_USAGE_STATS allow
adb shell appops set org.stilllauncher SYSTEM_ALERT_WINDOW allow
adb shell pm grant org.stilllauncher android.permission.POST_NOTIFICATIONS
adb shell settings put secure enabled_accessibility_services \
  org.stilllauncher/org.stilllauncher.service.AppAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

An emulator ANR dialog (“System UI isn’t responding”) is a cold-boot artefact; dismiss it and wait. Check `adb logcat` for `FATAL EXCEPTION` before assuming an app crash.

Do not say “done” or “works” for behaviour you did not exercise. State what was compiled, what was tested, and what remains untested.

## Release process

Maintainer operation, tag-driven:

```bash
git tag vX.Y.Z
git push origin main
git push origin vX.Y.Z
```

The Release workflow runs tests, builds and signs the release with repository secrets, verifies the signature, and publishes the APK with `SHA256SUMS` and a gzipped R8 mapping. Every main push also refreshes the rolling `latest` prerelease.

Before tagging: update `CHANGELOG.md`, the default version in `app/build.gradle.kts`, run full verification, commit **without AI attribution**, push, watch the workflow, and verify the release assets.

Required secrets (maintainer’s repo): `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

## Style

- Kotlin 4-space indent; small composables; state hoisting; no unnecessary abstractions.
- Material 3 tokens and the app theme; no hard-coded colours outside theme palettes.
- English UI copy, short and direct. If localisation is introduced, use resources deliberately.
- Accessibility: meaningful labels, large-font support, no tiny tap targets.
- No personal data in logs.
- Keep diffs focused; no drive-by reformatting.

## Known traps

| Symptom | Cause |
|---|---|
| `checkDebugAarMetadata` fails | New AndroidX requires AGP 9.1 / compileSdk 37. The stack is pinned deliberately. |
| Release signing “block not properly padded” | PKCS#12 uses the store password for the key; use the same value. |
| Invisible button labels | Explicit text colours in `Typography` override Material content colours. Use `Surface(contentColor=...)`. |
| Block screen never appears | Missing usage access/accessibility/overlay, or background-activity restriction. |
| Index bar absent | Searching, or fewer than three letter groups. |
| Recents empty | Monitor service not running, no usage access, or no launchable app foregrounded since install. |
| `am start` cannot start Blocked/Reminder | They are not exported by design. |
| Lint `StateFlowValueCalledInComposition` | Collect the flow instead of reading `.value`. |
| Adaptive icon missing | Keep icons in `mipmap-anydpi-v26`; AAPT rejects `<adaptive-icon>` elsewhere. |
| Play Protect warns/blocks | Self-distributed release APK with accessibility. Install anyway or `adb install`; never publish debug builds. |
| Accessibility/notification access greyed out | Android 13+ restricted settings: App info → ⋮ → Allow restricted settings. |
| Latest APK is debuggable | Wrong workflow: build `assembleRelease` with signing secrets, not `assembleDebug`. |

## Definition of done

- [ ] Focused change, tests and lint green, debug build produced.
- [ ] Runtime behaviour verified when relevant, with honest reporting.
- [ ] No network, telemetry, broad permissions or AI attribution.
- [ ] README/CHANGELOG/docs updated for user-visible work.
- [ ] Working tree clean or handoff described; no secrets or build artefacts committed.
