---
description: Run the full verification suite and report exactly what passed
allowed-tools: Bash(./gradlew:*), Bash(adb:*), Bash(git status:*), Bash(git diff:*), Read, Grep, Glob
---

Run the project verification protocol for Still Launcher.

1. Read `CLAUDE.md` and `AGENT.md` if you have not already.
2. Run, in order:
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:lintDebug`
   - `./gradlew :app:assembleDebug`
3. If any command fails, capture the failing task and the first actionable error. Do not try random dependency upgrades; the AndroidX/AGP stack is pinned deliberately.
4. If the working tree has app-code changes that affect UI or enforcement, and a device/emulator is connected, run the smoke checks:
   - install the debug APK,
   - grant usage access and accessibility via Settings → Permissions,
   - open a third-party app, press Home, verify the background-app chip and **Return**,
   - open **All apps**, verify Recent apps and drag the index bar,
   - tap **Recents** and verify Android's recents screen,
   - create a one-hour block, open the app, verify the block screen.
5. Report a checklist with each command and its result. State explicitly what was not tested. Do not claim runtime success from compilation alone.

Do not modify files as part of this command unless the user asks for fixes; report first.
