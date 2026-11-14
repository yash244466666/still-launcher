# Changelog

All notable changes to this project are documented here. This project follows [Semantic Versioning](https://semver.org/) and [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.3.2] - 2026-11-14

### Changed

- The launcher is text-first again: app letter badges, the home dock icons and decorative section icons are gone. App names are plain words in the list, search results, pickers, limits, screen time and hidden-apps screens. Small control icons remain only in Settings and the app action sheet so switches and buttons stay easy to find.
- The home dock is a single row of text buttons (`Call · Recents · All apps · Settings`) with the All apps button highlighted.

### Fixed

- The home task is excluded from Recents both in the manifest and at runtime. Some OEM recents views showed a blank full-screen card for the home task when other recent apps were present; the launcher now never enters that carousel.
- In-app swipe gestures work again: vertical drags are detected before the double-tap detector, so swipe up opens the app list and swipe down opens the notification shade. Both directions were verified on the emulator.
- Stale duplicate launcher tasks from older builds are removed when the app is the default home, and starting from the app icon no longer creates a second task.

## [0.3.1] - 2026-11-14

### Fixed

- Gesturing Home while already on the home screen no longer leaves a blank, wallpaper-only screen. The home activity now uses the AOSP/Pixel Launcher configuration: `clearTaskOnLaunch`, `resumeWhilePausing`, `resizeableActivity`, an empty task affinity and no `excludeFromRecents`.
- The app icon now opens a no-display trampoline instead of the home activity. The home activity no longer advertises `CATEGORY_LAUNCHER`, so tapping the icon cannot create a second standard task; duplicate tasks left by older releases are removed once the app is the default home.
- The launcher no longer lists itself in its own app list.

## [0.3.0] - 2026-11-14

### Changed

- Redesigned the interface around labelled cards, icons, section headers and obvious actions. Switches, buttons and tappable rows are now visually distinct instead of plain text.
- Settings is grouped into Focus tools, Home screen, and Setup & help, with a setup card that shows whether the three essentials are ready.
- Permissions shows a progress summary and gives every permission an icon, a plain-language purpose and a visible status pill; missing essentials say “Needed”.
- Long-pressing an app now opens a bottom sheet with a three-column action grid: Open, Favourite, Daily limit, Block, Move to folder, Rename, Hide, App info and Uninstall.
- Onboarding ends with a three-step access checklist, and the Home screen, app list and every empty screen now tell the user what to do next and offer the action inline.
- Added a calm accent colour derived from the chosen theme, a live theme preview, font/size preview, date/time preview, app letter badges, a labelled Home dock and clearer alphabet index bars.
- Daily-limit editor now has +/− steppers, quick budget choices and a cleaner slider; schedules and limits explain themselves before the first item exists.
- Added `StillIcons` for concepts not covered by Material's core icon set and documented the shared UI conventions for future changes.

### Fixed

- Gesturing Home while already on the Home screen no longer leaves a blank wallpaper-only screen. The launcher is still excluded from Recents, but the auto-remove flag and empty task affinity that could destroy the home task are gone.
- Cold start now shows a brief branded loading state instead of a black window while settings load.
- Screen-time bars no longer turn red for the most-used app; red is reserved for a limit that is actually over budget.
- Package labels that PackageManager hides now fall back through LauncherApps, and sub-minute usage reads “<1 m” instead of “0 m”.
- Notification-access warnings now use the same warning styling as other attention states.

## [0.2.6] - 2026-11-14

### Fixed

- The launcher task no longer appears as a blank card in Android Recents, including under gesture navigation. The home activity is excluded from Recents and marked auto-remove.

## [0.2.5] - 2026-11-14

### Added

- The app-menu alphabet index is available on both the left and right edges, so it can be used with either hand.
- The letter for the current list section stays highlighted while scrolling, not only while scrubbing.
- Animated letter highlight, haptic ticks while scrubbing, a spring/fade letter bubble, and animated jumps when tapping a letter.
- The index bars no longer overlap app labels: the list reserves space at both edges.

## [0.2.4] - 2026-11-14

### Added

- Apps from every Android launcher profile are listed, so OEM dual apps, parallel apps and work-profile apps appear alongside the main app. Secondary-profile apps are labelled `(dual app)` and launched with their own user handle.
- Favourites, folders, rename and hide now work per profile using a stable `package#userId` key; existing main-profile entries keep their plain package key.
- App info opens the correct profile. Blocks and daily limits are restricted to the main profile because cross-profile foreground enforcement is not available from a normal app.

### Changed

- Unit tests now cover profile-key generation.

## [0.2.3] - 2026-11-14

### Fixed

- Permission status now refreshes automatically every two seconds while the Permissions screen is visible, in addition to resume and manual refresh.
- Accessibility detection no longer fails early when an OEM does not expose the master secure flag; the manager list is checked first.
- Usage-access detection falls back to a real usage query when an OEM reports a non-standard app-op mode.
- Default-launcher detection also uses the system HOME role.
- Version codes are now derived from the semantic version (0.2.3 -> 100203) instead of CI run numbers, so F-Droid reliably sees every update.

## [0.2.2] - 2026-11-14

### Fixed

- Permission status now re-reads automatically when returning from system settings and when the Refresh button is tapped.
- Accessibility and notification-listener detection also read the secure settings directly, avoiding stale OEM caches.

## [0.2.1] - 2026-11-13

### Fixed

- The rolling **Latest build** is now a signed, non-debuggable release APK instead of a debug build. Debug builds were the most likely source of Play Protect warnings.
- Release signing now explicitly enables APK Signature Scheme v2 and v3.
- Removed the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission; the app now opens the system battery-optimisation list instead of requesting a direct exemption.
- Added **Settings → Install & access help** with Play Protect, “Allow restricted settings”, permission paths and the installed certificate fingerprint.

### Added

- Direct buttons to app info, accessibility, notification access and usage access from the help screen.
- Display and copy of the installed APK signing certificate SHA-256.

## [0.2.0] - 2026-11-13

### Added

- Alphabetic index bar in the app menu: drag or tap a letter to jump to that section.
- Recent apps section at the top of the app menu, kept locally and capped at eight entries.
- Background-app indicator on the home screen showing the last app that was in the foreground, with a one-tap **Return** action.
- **Recents** shortcut on the home screen to open Android's minimised-app screen (requires the accessibility service).
- **Settings → Home screen → Recent apps** to clear the local recent history.

### Changed

- The app list now uses a stable shared row model so sections, folders and the index bar stay in sync.
- Version defaults to 0.2.0 (versionCode 2); release version values still come from the tag.

## [0.1.0] - 2026-11-13

### Added

- Text-first home screen with clock, date, battery arc and favourites.
- All-apps list with search, letter sections, folders, rename, hide and uninstall actions.
- Timed app blocking with a full-screen block screen, countdown and explicit early exit.
- Recurring blocking schedules with day selection, overnight windows and per-schedule app sets.
- Per-app daily limits with remind, exit and 15-minute cooldown behaviours.
- Optional per-app grayscale via Android's display daltonizer (requires `WRITE_SECURE_SETTINGS`).
- Notification allow-list filter with an in-app list of recently filtered notifications.
- Screen-time statistics for today, this week and this month.
- Experimental short-video guard for YouTube Shorts, Instagram Reels and Snapchat Spotlight.
- System, black, white, E-Ink and custom themes; four font families; seven text sizes; date/time formats.
- Swipe-up search, swipe-down notification shade, double-tap lock and Home-intent navigation reset.
- Unit tests for overnight and weekday schedule logic.
- CI (tests, lint, debug build), rolling latest build, and signed tag-based releases with checksums.
