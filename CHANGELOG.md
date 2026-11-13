# Changelog

All notable changes to this project are documented here. This project follows [Semantic Versioning](https://semver.org/) and [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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
