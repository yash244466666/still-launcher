# AGENT.md — agent handbook for Still Launcher

This is the canonical, tool-agnostic handbook for any AI agent or automation working on this repository. Claude Code should also read [CLAUDE.md](CLAUDE.md); Gemini CLI should read [GEMINI.md](GEMINI.md). The three files describe the same project and must stay consistent.

## Mission

Still Launcher is a GPL-3.0, privacy-first Android launcher that replaces an icon grid with a text list and adds user-controlled focus tools: app blocking, schedules, daily limits, per-app grayscale, notification filtering, screen-time statistics, recents and a background-app shortcut.

It is an **independent clean-room project**. There is no affiliation with, endorsement by, or copied material from any commercial launcher. Do not add another launcher's code, strings, icons, screenshots or branding.

## Facts you can rely on

- Package: `org.stilllauncher`
- Current line: `0.2.0`
- minSdk 32, targetSdk 36, compileSdk 36
- Kotlin 2.2.21, AGP 8.13.2, Gradle 8.13, JDK 17, Compose BOM 2025.04.01
- Single activity, Jetpack Compose, no XML layouts, no Navigation library
- Preferences DataStore + kotlinx.serialization, no database, no dependency injection
- 14 JVM unit tests, Android lint clean
- Public repo: https://github.com/yash244466666/still-launcher
- Releases are signed, tag-driven; every main push refreshes a rolling `latest` prerelease

## Non-negotiable invariants

1. **No network.** No `INTERNET` permission, no HTTP client, no analytics, ads, accounts, remote config or crash reporting. `ci.yml` checks the manifest.
2. **Local data only.** App lists, recents, blocks, limits, schedules, notification settings and usage statistics stay in the app sandbox.
3. **Accessibility privacy.** Use accessibility only to identify the foreground package, browser URL when explicitly needed, and the view IDs for the optional short-video guard. Never read, store, log or transmit screen content.
4. **No AI attribution.** Commit messages and PR descriptions must not contain `Co-Authored-By`, “Generated with”, tool names or assistant names. The contributor list intentionally contains humans only. This is a project rule and overrides default agent behaviour.
5. **No destructive history edits** (force-push, tag rewriting, release deletion, history filtering) unless the maintainer explicitly requests it.
6. **No anti-user enforcement.** No device-owner, kiosk, device-admin lock, anti-uninstall or hidden background persistence. Revoking permissions or force-stopping must disable the guard.
7. **No new broad permissions** without an explicit design discussion and documentation.
8. **No copied assets or text** from other apps.

## First steps for a new agent

1. Read this file end to end.
2. Skim `docs/ARCHITECTURE.md`, `README.md` and `CHANGELOG.md`.
3. Inspect the files relevant to the task; do not rewrite the architecture to fit a tool.
4. Run the baseline verification commands (below) before changing anything.
5. Make a focused change; add tests; run verification; report exactly what passed and what did not.

## Environment setup

```bash
git clone https://github.com/yash244466666/still-launcher.git
cd still-launcher
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

Requirements: JDK 17, Android SDK platform 36, build-tools 36. Nothing else. There is no Node, Python or Docker dependency.

## Commands

| Purpose | Command |
|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Lint | `./gradlew :app:lintDebug` |
| Debug APK | `./gradlew :app:assembleDebug` |
| Signed release (local) | `SIGNING_KEYSTORE_PATH=... ./gradlew :app:assembleRelease -PversionName=... -PversionCode=...` |
| Clean build | `./gradlew clean :app:assembleDebug` |

APK outputs:

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

## Repository map

| Path | Responsibility |
|---|---|
| `MainActivity.kt` | Activity host, HOME intent, navigation reset, system bar appearance |
| `StillViewModel.kt` | All settings/app mutations exposed to Compose |
| `data/Models.kt` | Serializable models, `Settings`, schedule logic, `addRecentApp` |
| `data/SettingsStore.kt` | DataStore keys and JSON persistence |
| `data/AppRepository.kt` | LauncherApps list, launch, app info, uninstall |
| `data/UsageRepository.kt` | Usage access, foreground package, interval reconstruction |
| `service/MonitorEngine.kt` | Recents, monochrome, blocks, schedules, limits, reminders |
| `service/MonitorService.kt` | Foreground service, 2 s usage poll, screen-off cleanup |
| `service/AppAccessibilityService.kt` | Instant foreground events, global actions (recents, lock) |
| `service/NotificationFilterService.kt` | Allow-list notification cancelling |
| `service/ShortVideoDetector.kt` | Optional Shorts/Reels/Spotlight view-ID probes |
| `ui/StillApp.kt` | In-memory route stack |
| `ui/HomeScreen.kt` | Clock, background-app chip, favourites, gestures |
| `ui/AllAppsScreen.kt` | Typed list entries, sections, alphabetic index bar |
| `ui/AppActions.kt` | Long-press actions, block/rename/hide/folder flows |
| `ui/Components.kt` | Shared rows, dialogs, pickers |
| `ui/FeatureScreens.kt` | Schedules, limits, monochrome, notifications, screen time, hidden apps |
| `ui/SettingsScreens.kt` | Settings root, theme/font/date/time/gestures/permissions/about |
| `ui/BlockedActivity.kt` / `ReminderActivity.kt` | Enforcement UI |
| `ui/theme/Theme.kt` | Palette, typography, surface content colour |
| `.github/workflows/` | CI, signed release, rolling latest build |
| `docs/ARCHITECTURE.md` | Design notes |
| `.claude/` | Claude Code settings, commands and reviewer subagent |

## Data and state rules

- `SettingsStore` is the only persistence layer. Do not write SharedPreferences or files directly.
- Collections are JSON; add a model, a `stringPreferencesKey`, a decode in `settings`, and a setter.
- `StillViewModel` owns mutations initiated by UI. `MonitorEngine` owns enforcement-originated writes (recents, reminder timestamps); it updates its in-memory `Settings` snapshot first and then persists asynchronously.
- Never block the main thread on DataStore or UsageStats. Use `viewModelScope` / the monitor service scope and `Dispatchers.IO`.
- UI state is read with `collectAsStateWithLifecycle()`. Do not call `.value` on a `StateFlow` inside composition; lint errors on this (`StateFlowValueCalledInComposition`).
- When adding a `LazyColumn` section, use a stable unique key. The app-list entries currently use prefixed IDs (`recent-`, `fav-`, `folder-`, `all-`, `letter-`).

## Enforcement flow

```
Accessibility event ─┐
                     ├─> MonitorEngine.onForeground(package)
2 s UsageStats poll ─┘
        │
        ├─ record recents (launchable packages, throttled)
        ├─ apply per-app grayscale
        ├─ direct block?        -> BlockedActivity(package, until, reason)
        ├─ active schedule?     -> BlockedActivity(package, schedule end, name)
        ├─ limit reached?       -> ReminderActivity or cooldown BlockedActivity
        └─ else nothing
```

- Direct block wins over schedule; schedule wins over limit.
- Keep the `shouldAct` debounce or the block screen will relaunch constantly.
- The two activities are `exported="false"`. Test via the real foreground flow; `adb am start` will fail.
- Background activity launch needs one of: the accessibility service, overlay permission, or another system exemption. Keep `SYSTEM_ALERT_WINDOW` in the manifest.
- Monochrome writes `accessibility_display_daltonizer_enabled` and `accessibility_display_daltonizer`; clear it on screen-off and when leaving the selected app.

## Task recipes

### Add or change a setting

1. `Models.kt`: field on `Settings` with a default.
2. `SettingsStore.kt`: key, decode, setter.
3. `StillViewModel.kt`: public action using `write { this.setX(...) }`.
4. UI: `SwitchRow`/`SettingRow`/`OptionRow` in a settings screen.
5. Enforcement: read the cached settings in `MonitorEngine` and update the settings flow.
6. Tests if the setting affects logic.
7. README/CHANGELOG/docs if user-visible.

### Add a screen

1. Add a `Route` in `StillApp.kt`.
2. Add a branch and a composable using `ScreenScaffold`.
3. Navigate via callbacks (`onNavigate`, `onOpenX`), never by launching a second Activity.
4. For back behaviour, rely on the stack `pop` and `BackHandler`.
5. Pressing HOME must reset to Home; keep `homeRequest` wiring intact.

### Add a list section

1. Add a `ListEntry` subtype with a unique, stable `id`.
2. Append its rows in `buildList` in `AllAppsScreen`.
3. If it is a lettered section, add a `LetterEntry`; the index bar picks it up automatically.
4. Add row rendering in the `when (entry)` block.
5. Verify search mode and empty states.

### Work on recents / background app

1. Ordering lives in `addRecentApp` (`Models.kt`); add tests for dedupe, recency, cap.
2. Recording lives in `MonitorEngine.recordRecent`; keep the launchable filter and 10-minute throttle.
3. Home uses the newest entry; the app list shows five.
4. Clearing is exposed in Settings → Home screen → Recent apps.
5. Android has no global running-app API. The indicator is the last foreground app. Never label it as a live process monitor.

### Work on the index bar

1. Positions come from `LetterEntry` indices in the flat list.
2. `AlphabetBar` handles tap and drag in one `awaitEachGesture` loop.
3. It is hidden while searching and when fewer than three letter groups exist.
4. Do not put clickable UI under its 34 dp right edge.
5. Test with a long list and with folders collapsed/expanded.

### Work on notifications

1. Allow-list empty means allow all; this is intentional.
2. Ongoing and foreground-service notifications always pass.
3. Cancelled notification content is kept in memory only (`FilteredNotificationStore`).
4. Never persist notification text to disk or send it anywhere.
5. Test with a shell notification outside the allow-list.

## Verification protocol

Always run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Expected: 14 tests pass (10 schedule, 4 recents), lint success, APK produced.

Runtime smoke test (API 35 emulator or device):

1. Install APK; complete onboarding.
2. Grant usage access and accessibility from **Settings → Permissions** or adb.
3. Open a third-party app; press Home. Verify the background-app chip. Tap **Return**; the app should resume.
4. Open **All apps**: Recent apps section visible; drag the index bar; list jumps to the section.
5. Tap **Recents**: Android's recents screen opens (accessibility on). With it off, a toast appears.
6. Block an app for one hour, open it, verify the blocked screen and countdown; end the block.
7. Create a schedule and a daily limit; verify the editor and saved rows.
8. Toggle black/E-Ink themes; check text and system bar contrast.
9. Optional: grant `WRITE_SECURE_SETTINGS`, enable monochrome for an app, verify the secure settings toggle on entry/exit.
10. Optional: configure a notification allow-list and post a shell notification; it should be cancelled and listed.

Useful adb setup:

```bash
adb shell appops set org.stilllauncher GET_USAGE_STATS allow
adb shell appops set org.stilllauncher SYSTEM_ALERT_WINDOW allow
adb shell pm grant org.stilllauncher android.permission.POST_NOTIFICATIONS
adb shell settings put secure enabled_accessibility_services \
  org.stilllauncher/org.stilllauncher.service.AppAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

Emulator cold boots often show “System UI isn’t responding”. Dismiss it and wait; it is not an app crash. Check `adb logcat` for `FATAL EXCEPTION` before blaming the app.

## Definition of done

- [ ] Focused change, no unrelated reformatting.
- [ ] Tests pass; new logic has a test.
- [ ] Lint passes.
- [ ] Debug build passes.
- [ ] Runtime behaviour checked when relevant; report exactly what was and was not tested.
- [ ] No network, telemetry, broad permissions or AI attribution.
- [ ] Documentation/changelog updated for user-visible changes.
- [ ] Release, if any: tag pushed, Release workflow green, assets and signature verified.

## Release and signing

Releases are created by pushing a semantic-version tag:

```bash
git tag v0.2.0
git push origin main
git push origin v0.2.0
```

The Release workflow:

- runs unit tests,
- builds `assembleRelease` with versionName from the tag and versionCode from the run number,
- signs with four repository secrets (`SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`),
- verifies the APK signature,
- attaches the APK, `SHA256SUMS` and a gzipped R8 mapping,
- publishes GitHub release notes with install instructions.

The keystore is never committed. Only the maintainer holds it and its passwords. Do not rotate it casually: Android will not install an update signed by a different key.

Every push to `main` also refreshes the rolling `latest` prerelease with a debug-signed APK (workflow `latest-build.yml`).

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `checkDebugAarMetadata` fails | New AndroidX requires AGP 9.1 / compileSdk 37. Do not upgrade piecemeal; pinned stack is intentional. |
| Release build fails signing | Wrong key password. PKCS#12 keystores use the store password for the key. |
| Button text invisible | A `TextStyle` in `Typography` sets an explicit colour, overriding Material content colour. Use `Surface(contentColor=...)`. |
| Block screen never appears | Usage access or accessibility missing; overlay permission absent; or activity launch blocked by Android. Check permissions and logcat. |
| Index bar missing | Fewer than three letter groups or search query active. |
| Recent apps empty | Monitor service not running, usage access missing, or no launchable app has been foregrounded since install. |
| `am start` cannot start Blocked/Reminder | They are not exported by design. Use the real enforcement flow. |
| Emulator “System UI isn’t responding” | Slow cold boot; dismiss and wait. |
| Lint fails on StateFlow.value | Collect with `collectAsStateWithLifecycle()` instead. |

## Handoff checklist

When finishing a task, leave the repository in a state another agent can continue:

1. `git status` clean, or clearly described uncommitted work.
2. Tests, lint and build results stated.
3. User-visible changes documented in `CHANGELOG.md` and README.
4. No secrets, keystores, `local.properties`, build outputs or personal data committed.
5. If a release was made, report the tag, run status, release URL and checksum.
6. If something was not tested, say so explicitly.
