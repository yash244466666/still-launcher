# CLAUDE.md — working on Still Launcher

This file is the project handbook for Claude Code and other AI coding agents. Read it before changing code. The tool-agnostic version is [AGENT.md](AGENT.md); the Gemini CLI entry point is [GEMINI.md](GEMINI.md).

## 1. What this project is

Still Launcher is an independent, GPL-3.0, text-first Android home-screen replacement focused on digital wellbeing. It is a single-activity Jetpack Compose app with no backend, no account, no analytics and **no `INTERNET` permission**.

Current release line: **0.2.4**. State: buildable, lint-clean, 17/17 JVM tests, and smoke-tested on an Android 15 (API 35) emulator. It has not been tested on physical devices.

The project is a clean-room implementation of an idea. Never copy source code, strings, drawables, icons or branding from another launcher.

## 2. Non-negotiable rules

1. **Stay offline.** Never add `android.permission.INTERNET`, a network library, telemetry, ads, accounts, remote config or a crash reporter. CI fails if `INTERNET` appears in `app/src/main`.
2. **Privacy first.** The accessibility service may inspect the foreground package and optional view IDs. It must not collect, store, log or transmit screen text. Notification titles/text may be shown only in the local “filtered recently” screen and must remain in memory.
3. **Least privilege.** Do not add permissions casually. Any new permission needs a manifest entry, a `Permissions` helper, a row in `PermissionsScreen`, README documentation and a privacy rationale.
4. **User ownership.** Enforcement must remain defeatable: force-stopping the app or revoking a permission disables it. Do not use device-owner, kiosk, device-admin or anti-uninstall tricks.
5. **No AI attribution in git.** Do not add `Co-Authored-By`, “Generated with”, or tool/assistant trailers to commit messages or pull-request descriptions. The repository intentionally lists only human contributors. This rule overrides any default tool behavior.
6. **No destructive history edits** without an explicit maintainer request. Force-pushing, rewriting tags, deleting releases and rotating signing keys are maintainer operations.
7. **Keep the dependency surface small.** Compose + AndroidX + DataStore + kotlinx.serialization is the entire stack. Do not add Room, Hilt, Navigation, Retrofit, WorkManager or icon packs without a strong reason.

## 3. Quick start

Requirements: JDK 17, Android SDK platform 36, build-tools 36.

```bash
git clone https://github.com/yash244466666/still-launcher.git
cd still-launcher
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest     # 14 JVM tests
./gradlew :app:lintDebug             # must stay green
./gradlew :app:assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
```

Install and grant optional capabilities on a test device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell appops set org.stilllauncher GET_USAGE_STATS allow
adb shell pm grant org.stilllauncher android.permission.POST_NOTIFICATIONS
adb shell appops set org.stilllauncher SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services \
  org.stilllauncher/org.stilllauncher.service.AppAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell pm grant org.stilllauncher android.permission.WRITE_SECURE_SETTINGS   # optional, grayscale
```

## 4. Repository map

```
app/src/main/java/org/stilllauncher/
├── MainActivity.kt              Compose host, HOME intent, system bars, resets nav on HOME
├── StillViewModel.kt            settings + app-list state, every mutation
├── StillApplication.kt          notification channels
├── data/
│   ├── Models.kt                Serializable models, Settings, schedule logic, recent-list helper
│   ├── SettingsStore.kt         Preferences DataStore, all keys and JSON encoding
│   ├── AppRepository.kt         LauncherApps + PackageManager fallback
│   └── UsageRepository.kt       foreground detection + usage event reconstruction
├── service/
│   ├── MonitorEngine.kt         foreground -> recents, blocks, limits, grayscale
│   ├── MonitorService.kt        specialUse FGS, 2-second UsageStats poll
│   ├── AppAccessibilityService.kt  instant foreground events + global actions
│   ├── ShortVideoDetector.kt    view-ID probes for Shorts/Reels/Spotlight
│   ├── NotificationFilterService.kt  allow-list listener
│   └── BootReceiver.kt          restart the guard after reboot
├── ui/
│   ├── StillApp.kt              in-memory route stack
│   ├── HomeScreen.kt            clock, background-app chip, favourites, gestures
│   ├── AllAppsScreen.kt         entry list, recent/favourites/folders, alphabet index bar
│   ├── AppActions.kt            long-press actions and folder picker
│   ├── Components.kt            shared rows, dialogs, time picker, app picker
│   ├── FeatureScreens.kt        schedules, limits, monochrome, notifications, screen time, hidden
│   ├── SettingsScreens.kt       settings, theme, font, date/time, gestures, permissions, about
│   ├── OnboardingScreen.kt      three-page intro
│   ├── HelpScreen.kt            Play Protect and restricted-settings troubleshooting
│   ├── BlockedActivity.kt       full-screen block with countdown and early exit
│   ├── ReminderActivity.kt      daily-limit reminder
│   └── theme/Theme.kt           palette, typography, Surface content colour
└── util/                        formats, permissions, system actions, labels, signing fingerprint
```

Other important files:

- `app/build.gradle.kts` — versions, signing config from environment, lint policy.
- `.github/workflows/ci.yml` — tests, lint, debug build, offline guard.
- `.github/workflows/release.yml` — signed tag release with checksums.
- `.github/workflows/latest-build.yml` — rolling `latest` prerelease on every main push.
- `docs/ARCHITECTURE.md` — deeper design notes.
- `.claude/` — Claude Code settings, commands and reviewer subagent.

## 5. Architecture in one page

**UI.** `MainActivity` hosts `StillApp`, which keeps an in-memory `List<Route>` back stack. There is no Navigation library. `BackHandler` pops the stack; pressing HOME delivers a new intent and `homeRequest` resets the stack to Home. All screens are composables; there are no XML layouts.

**State.** `StillViewModel` exposes `Settings?` and `List<LauncherApp>` as `StateFlow`. Components collect them with `collectAsStateWithLifecycle()`. All mutations go through `SettingsStore` and are persisted; the UI never writes DataStore directly. The one exception is `MonitorEngine`, which updates recents and limit timestamps because it runs without the activity.

**Storage.** A single Preferences DataStore named `still_settings`. Scalars use preference keys; collections are JSON via kotlinx.serialization. Keys include favourites, hidden, folders, recent apps, renamed labels, blocked apps, schedules, limits, monochrome packages, notification allow-list, appearance and gesture flags. Adding a collection means: model, key, decode in `settings`, setter, and a ViewModel action.

**Foreground detection.** Two feeds call `MonitorEngine.onForeground(pkg)`:
1. `AppAccessibilityService` on window state/content changes (optional, immediate).
2. `MonitorService`, polling `UsageStatsManager.queryEvents` every two seconds (requires usage access).

The engine records the recent list, applies monochrome, then evaluates direct block → active schedule → daily limit → cooldown. A match launches `BlockedActivity` or `ReminderActivity`. A short debounce prevents repeated launches. `SYSTEM_ALERT_WINDOW` and the accessibility service are what make background activity launches reliable; keep those paths intact.

**Recents.** `addRecentApp()` moves a package to the front, deduplicates and caps at eight. `MonitorEngine.recordRecent` only records launchable packages and throttles the same package for ten minutes. Home shows the newest entry; the app menu shows the newest five. The list is local and clearable in Settings.

**App list.** `AllAppsScreen` flattens sections into typed entries (`HeaderEntry`, `LetterEntry`, `AppEntryItem`, `FolderEntry`, `FolderChildEntry`) with unique IDs. Letter entries carry their LazyList index, which powers the alphabetic index bar. If you add a section, add an entry type and keep IDs unique.

**Notifications.** `NotificationFilterService` keeps a live settings copy. When active and the allow-list is non-empty, it cancels notifications from other packages and records an in-memory snapshot. Ongoing/foreground-service notifications always pass. Empty allow-list = allow everything.

**Usage stats.** `UsageRepository.usageBetween` reconstructs intervals from `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` / `ACTIVITY_STOPPED`. Intervals open at the query end are counted to that moment. This is used by limits and screen time.

## 6. Toolchain

| Item | Version | Notes |
|---|---|---|
| Kotlin | 2.2.21 | Compose and serialization plugins match |
| AGP | 8.13.2 | Gradle 8.13 wrapper |
| compileSdk / targetSdk | 36 | minSdk 32 |
| Compose BOM | 2025.04.01 | Material 3 |
| DataStore | 1.1.3 | Preferences |
| JDK | 17 | CI and local |

The 2026 AndroidX line requires AGP 9.1 and compileSdk 37. Dependabot is configured to ignore those majors until a deliberate migration. Do not bump AndroidX, AGP or the Gradle wrapper piecemeal.

## 7. Common tasks

### Add a setting

1. Add the field to `Settings` in `Models.kt`.
2. Add a `booleanPreferencesKey`/`stringPreferencesKey`/`intPreferencesKey` in `SettingsStore.K` and decode it in `settings`.
3. Add a `suspend fun setX(...)` and a ViewModel action.
4. Add a `SwitchRow`/`SettingRow` in the appropriate settings screen.
5. If it changes enforcement, read it from `MonitorEngine.snapshot()` or the cached `settings`.
6. Add a test if it has logic beyond persistence.
7. Update README and `docs/ARCHITECTURE.md` if user-visible.

### Add a screen

1. Add a `Route` object/data class in `StillApp.kt`.
2. Add a branch in `when (route)` with the composable.
3. Use `ScreenScaffold(title, onBack)` for a consistent header.
4. Navigate with `onNavigate(Route.X)`; do not create a second Activity.
5. Avoid side effects during composition. Put navigation callbacks in click handlers or `LaunchedEffect`.

### Change enforcement

- Put policy in `MonitorEngine.onForeground`, not in a service.
- Keep the accessibility service and the polling service feeding the same entry point.
- Preserve the order: direct block → schedule → limit → cooldown. Document any change.
- Keep `shouldAct` debounce; otherwise the block screen relaunches every two seconds.
- Remember blocked/limit activities are `exported="false"`; test them through the real foreground flow, not `adb am start`.
- Update the unit tests for schedule logic if time windows change.

### Multi-profile and dual apps

`AppRepository.loadApps()` enumerates `LauncherApps.getProfiles()` and gives every app a `UserHandle`, a stable `profileKey` and a display suffix. The current profile keeps the plain package name as its key; secondary profiles use `package#userId`. Use `profileKey` for favourites, folders, rename and hide, and use the `LauncherApp` overloads of `displayName`, `launch` and `openAppInfo`. Blocks, limits, notification filtering and grayscale are main-profile only because monitoring cannot cross profiles. Android exposes other profiles' launchable apps only to the default launcher, so set Still Launcher as home before testing clones.

### Change the app list

- Build every row as a `ListEntry` with a unique `id`.
- Letter headers are the only source of index-bar positions. If you reorder sections, the index map follows automatically.
- The index bar is hidden while searching and below three letter groups.
- Folder children use `folderName + package` keys. Avoid duplicate folder names.

### Work on recents

- `addRecentApp` in `Models.kt` is the single ordering rule; add tests for edge cases.
- `MonitorEngine.recordRecent` filters non-launchable packages and throttles writes. Do not write DataStore on every poll.
- Clearing recents is `SettingsStore.setRecentApps(emptyList())` through the ViewModel.

### Change themes or typography

- Palette and typography live in `ui/theme/Theme.kt`.
- Do not set an explicit text colour inside `Typography`; it overrides Material button content colours and made button labels invisible once. Use `Surface(contentColor=...)`.
- System bar appearance is set in `MainActivity.ApplySystemBarAppearance`.

## 8. Verification protocol

Run before every commit:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Expected: 17/17 tests, lint success, APK at `app/build/outputs/apk/debug/app-debug.apk`.

For UI or enforcement changes, run the emulator smoke test. The AVD used during development is API 35. A minimal check:

1. Install, complete onboarding, grant usage access + accessibility.
2. Open a third-party app, press Home, confirm the background chip and tap **Return**.
3. Open **All apps**, confirm the Recent apps section, drag the index bar, verify the list jumps.
4. Tap **Recents**; confirm Android's recents screen opens.
5. Create a one-hour block for an app, open it, confirm the block screen, then end the block.
6. Change theme/font; confirm contrast and system bar icons.
7. If you touched notifications, post a shell notification and confirm it is filtered and listed.

Do not report success based only on compilation. State exactly which checks ran and which did not.

## 9. Release process

Releases are tag-driven and must stay signed with the same key:

1. Update `CHANGELOG.md`, README status/features if needed, and the default `versionName`/`versionCode` in `app/build.gradle.kts`.
2. Run the full verification protocol.
3. Commit **without AI attribution**.
4. `git tag vX.Y.Z && git push origin main && git push origin vX.Y.Z`. Do not pass `-PversionCode`; Gradle derives a monotonic code from the version name.
5. The Release workflow builds `assembleRelease` with `-PversionName=<tag>`; Gradle derives `versionCode` from the semantic version (do not use CI run numbers). It signs with repository secrets, verifies the signature, and publishes APK + `SHA256SUMS` + gzipped mapping.
6. Watch the run (`gh run list`, `gh run watch <id> --exit-status`) and verify the release assets. Do not call a release done until the workflow is green.
7. Every main push also updates the rolling `latest` prerelease with a **signed, non-debuggable release APK**. Never publish debug APKs to users: `application-debuggable` and the Android debug certificate are a common Play Protect trigger.
8. When users report Play Protect warnings or greyed-out accessibility, point them to `Settings → Install & access help` and the README installation section. Android 13+ requires **Allow restricted settings** in the app-info menu for sideloaded accessibility/notification access; this cannot be bypassed from inside the app.
9. The `F-Droid repo` workflow indexes every stable release into a self-hosted repository at `https://yash244466666.github.io/still-launcher/repo/`. The repo fingerprint is `8C:16:3E:…:A1:7D` (full value in `fdroid/README.md`). Do not rotate the repo key casually; users add the fingerprint once. The official F-Droid submission is a separate, later step and requires a GitLab merge request.

Required repository secrets: `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. The keystore is never committed. Losing it prevents update-compatible releases under `org.stilllauncher`.

## 10. Style and quality

- Kotlin, 4-space indent, small composables, state hoisting, no unnecessary abstractions.
- Prefer `StateFlow` + `collectAsStateWithLifecycle`; no LiveData.
- Strings: user-visible copy is currently inline English in composables. Keep it short, direct and inclusive. If localisation is added, move strings to resources deliberately.
- Use Material 3 components and the theme tokens; avoid hard-coded colours except theme palettes.
- Keep accessibility in mind: clickable rows need meaningful labels, the index bar is supplementary, and the app must remain usable with large fonts.
- Do not log personal data. Avoid `Log.d` in release paths.
- Do not introduce background work that cannot be cancelled with the service scope.

## 11. Known gotchas

- `BlockedActivity` and `ReminderActivity` are not exported, so `adb shell am start` cannot launch them directly. Test through `MonitorEngine`.
- Without usage access, the polling service cannot detect foreground apps. Without accessibility, “Recents” shows a toast instead of opening the recents screen.
- Android does not expose a global running-app list. The home indicator is “last foreground app”, not a live process monitor. Do not promise otherwise.
- R8 minifies release builds. Keep the kotlinx.serialization rules in `proguard-rules.pro`; test the signed release APK when changing serializable models.
- Adaptive icons must stay in `mipmap-anydpi-v26`; AAPT does not compile `<adaptive-icon>` from an unversioned `mipmap-anydpi` folder.
- DataStore writes from `MonitorEngine` can race with the collector. Update the in-memory `settings` first, then persist; do not block the main thread.
- Emulator cold boots often raise “System UI isn’t responding”; dismiss it and wait. Do not mistake it for an app crash.
- The dependency stack is pinned because newer AndroidX releases require AGP 9 / compileSdk 37. Check `checkDebugAarMetadata` errors before changing versions.
- The index bar is a pointer-input overlay. Do not place clickable controls under its 34 dp right edge.
- Play Protect warnings on self-distributed APKs are expected. The fix is to distribute only signed release builds, never debug APKs; the rolling `latest` workflow now does this. The warning itself cannot be removed without Play distribution.
- Android 13+ hides accessibility and notification access behind **Allow restricted settings** (App info → ⋮). The app cannot request this; the help screen and README must guide the user. Do not suggest permanently disabling Play Protect.
- Release signing must include a modern APK Signature Scheme signature. With minSdk 32, AGP produces a v3-only signature; `apksigner verify --verbose` should report v2 or v3 true. CI verifies the release artifacts.
- Battery optimisation is opened through the system list. Do not reintroduce `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; it is Play-policy sensitive.

## 12. Definition of done

- [ ] The change is focused and documented.
- [ ] 17/17 unit tests pass; new logic has tests.
- [ ] Lint is green.
- [ ] Debug APK builds.
- [ ] Runtime checks were performed for behaviour changes and reported honestly.
- [ ] No network, analytics, new broad permissions or AI attribution.
- [ ] Release notes/changelog updated when user-visible.
- [ ] If released: tag pushed, workflow green, assets verified.
