# Claude Code project instructions

The canonical Claude Code handbook for this repository is the root **[CLAUDE.md](../CLAUDE.md)**. Read it first, together with [AGENT.md](../AGENT.md) for tool-agnostic task recipes.

This `.claude/` folder adds:

- `settings.json` — allow-listed build/test commands, with force-push and recursive deletion denied.
- `commands/verify.md` — `/verify`: full test, lint and build report.
- `commands/release.md` — `/release <version>`: guarded signed-release checklist.
- `agents/android-reviewer.md` — a review subagent for Android and privacy regressions.

Project rule: **do not add AI/assistant attribution to commits or pull requests**.
