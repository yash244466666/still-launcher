# Architecture

Still Launcher is a single-activity Jetpack Compose app with a small set of Android system integrations. It has no network permission and no server component.

## Layers

```
MainActivity (single activity, HOME intent)
└── StillApp (in-memory route stack)
    └── Compose screens
        └── StillViewModel
            ├── SettingsStore  (Preferences DataStore, JSON collections)
            ├── AppRepository  (LauncherApps / PackageManager)
            └── UsageRepository (UsageStatsManager)

Foreground enforcement
├── AppAccessibilityService ──┐
└── MonitorService (2 s poll) ─┴── MonitorEngine ── BlockedActivity
                                                └─ ReminderActivity
NotificationFilterService (NotificationListenerService)
```

## State and persistence

All persistent state lives in a single Preferences DataStore (`still_settings`). Scalars use primitive preference keys; collections are JSON-encoded with kotlinx.serialization:

- `favorites`, `hidden`, `folders`, `renamed`
- `recentApps` (`RecentApp`: package + last-used epoch millis, capped at eight)
- `blocked` (`BlockedApp`: package + epoch millis)
- `schedules` (`BlockSchedule`: days, start/end minute, packages, enabled)
- `limits` (`AppLimit`: minutes, behaviour, last reminder, cooldown)
- `monochromePackages`, `notificationAllowed`
- appearance and gesture settings

No database or file store is needed at this scale. The app has no `INTERNET` permission, so none of this can leave the device.

## Foreground detection and enforcement

Two independent signals feed `MonitorEngine.onForeground(packageName)`:

1. **Accessibility events** (`TYPE_WINDOW_STATE_CHANGED`, `TYPE_WINDOW_CONTENT_CHANGED`) for immediate detection. The service is optional.
2. **A 2-second `UsageStatsManager.queryEvents` poll** in `MonitorService` for devices without the accessibility service. This service requires `PACKAGE_USAGE_STATS`.

The engine checks, in order:

1. a direct `BlockedApp` whose `blockedUntil` is in the future,
2. an enabled `BlockSchedule` active at the current local time and containing the package,
3. a daily `AppLimit` whose used time (reconstructed from `UsageStats` events since midnight) has reached the budget,
4. an active cooldown.

A match launches `BlockedActivity` or `ReminderActivity` with the reason and end time. Both are `exported="false"`, excluded from recents and single-task. `SYSTEM_ALERT_WINDOW` is requested because Android otherwise restricts background activity launches; the accessibility service is also a system-bound service exempt from those restrictions on most builds.

Every launchable foreground package is also recorded in the local recent list (`RecentApp`, capped at eight). The home screen shows the newest entry as a background-app indicator, and the app list renders the newest five in a Recent apps section. Tapping the indicator launches the package, which brings its existing task forward when the app allows it. The **Recents** shortcut uses the accessibility service's `GLOBAL_ACTION_RECENTS` to open Android's own minimised-app screen.

Monochrome is applied on the same transition:

```kotlin
Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", on ? 1 : 0)
Settings.Secure.putInt(cr, "accessibility_display_daltonizer", on ? 0 : -1)
```

It is cleared on screen-off and when leaving the selected app. This path needs `WRITE_SECURE_SETTINGS`, which can only be granted from adb.

## Multiple Android profiles

OEM dual apps, parallel apps and work profiles are separate Android users. `AppRepository.loadApps()` enumerates `LauncherApps.getProfiles()` (falling back to `UserManager.userProfiles`) and queries each profile. Every app carries its `UserHandle`, a stable `profileKey` (`package` for the current user, `package#userId` for secondary profiles), and a `(dual app)` suffix for display.

Launches use `LauncherApps.startMainActivity(..., userHandle, ...)`, so a clone opens in the correct profile. Favourites, folders, rename and hide are keyed by `profileKey`; existing main-profile settings remain compatible. Blocks, daily limits, notification filtering and grayscale are deliberately limited to the main profile: foreground monitoring runs in the current user and cannot enforce behaviour in another profile.

## Notifications

`NotificationFilterService` keeps a live copy of `Settings` from DataStore. When filtering is active and an allow-list is configured, it cancels notifications from packages outside the list (except its own and ongoing/foreground-service notifications) and records a title/text snapshot in an in-memory, 50-item store for the settings screen. With no allow-list, all notifications pass.

## Screen time

`UsageRepository.usageBetween` reconstructs foreground intervals from `UsageEvents`: `ACTIVITY_RESUMED` opens an interval, `ACTIVITY_PAUSED`/`ACTIVITY_STOPPED` closes it, and intervals still open at the query end are counted up to that moment. This avoids the coarser aggregates in `UsageStats` and works for arbitrary ranges.

## App list index

`AllAppsScreen` builds a flat list of typed entries (section header, letter header, app, folder, folder child). Letter headers carry their list index, so the overlay index bar can call `LazyListState.scrollToItem` directly. The bar appears on both edges, highlights the section containing the first visible item, and handles tap and drag through a single `awaitEachGesture` loop. Dragging updates sections immediately with haptic ticks and a spring letter bubble; tapping animates to the section. The index is hidden while searching or when there are fewer than three letter sections, and the list reserves horizontal space so labels are never covered.

## UI system

The 0.3 redesign is built from a small set of shared composables in `Components.kt`:

- `ScreenScaffold` — header, back button, optional subtitle and trailing action.
- `SectionHeader` and `SettingsCard` — labelled groups on a rounded card surface.
- `NavRow`, `ToggleRow`, `AppToggleRow`, `ChoiceRow` — rows with an icon tile and an explicit affordance (chevron, switch or radio button).
- `PrimaryButton`, `SecondaryButton`, `AddButton` — the three button weights used across editors.
- `EmptyState`, `InfoCard`, `StatusPill`, `AppMonogram`, `CodeBlock` — empty lists always offer the action that fills them; explanations and state never rely on colour alone.
- `StillIcons` — original vectors for timer, contrast, palette, folder, gesture, history, shield, grid, chart, eye-off and clock, drawn on the same 24×24 grid as Material's core icons.

`Theme.kt` derives a calm accent, soft card surfaces and outline tones from the user's background colour rather than hard-coding a second palette. Text colours are left to `Surface(contentColor = ...)`; setting colours inside `Typography` breaks Material button labels. System bar icon appearance follows the resolved light/dark theme in `MainActivity`.

## Design constraints

- No analytics, ads, accounts, remote config or network access.
- No `QUERY_ALL_PACKAGES`; app visibility comes from a `<queries>` MAIN/LAUNCHER intent and `LauncherApps`.
- Enforcement is user-owned: force-stopping the app or revoking a permission disables it. This is a wellbeing tool, not a device lock.
- Short-video detection is best-effort and fails open; it depends on view IDs that apps may change.
