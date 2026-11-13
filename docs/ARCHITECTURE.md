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

Monochrome is applied on the same transition:

```kotlin
Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", on ? 1 : 0)
Settings.Secure.putInt(cr, "accessibility_display_daltonizer", on ? 0 : -1)
```

It is cleared on screen-off and when leaving the selected app. This path needs `WRITE_SECURE_SETTINGS`, which can only be granted from adb.

## Notifications

`NotificationFilterService` keeps a live copy of `Settings` from DataStore. When filtering is active and an allow-list is configured, it cancels notifications from packages outside the list (except its own and ongoing/foreground-service notifications) and records a title/text snapshot in an in-memory, 50-item store for the settings screen. With no allow-list, all notifications pass.

## Screen time

`UsageRepository.usageBetween` reconstructs foreground intervals from `UsageEvents`: `ACTIVITY_RESUMED` opens an interval, `ACTIVITY_PAUSED`/`ACTIVITY_STOPPED` closes it, and intervals still open at the query end are counted up to that moment. This avoids the coarser aggregates in `UsageStats` and works for arbitrary ranges.

## Design constraints

- No analytics, ads, accounts, remote config or network access.
- No `QUERY_ALL_PACKAGES`; app visibility comes from a `<queries>` MAIN/LAUNCHER intent and `LauncherApps`.
- Enforcement is user-owned: force-stopping the app or revoking a permission disables it. This is a wellbeing tool, not a device lock.
- Short-video detection is best-effort and fails open; it depends on view IDs that apps may change.
