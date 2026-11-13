# `.claude` — Claude Code toolkit

These files are committed so any Claude Code session starts with the same project context and safe defaults.

| File | Purpose |
|---|---|
| `CLAUDE.md` | Short pointer to the root handbook; kept here for tools that look in `.claude/`. |
| `settings.json` | Read-only/build command allow-list. Force-push and `rm -rf` are denied. |
| `commands/verify.md` | `/verify` — run tests, lint and the debug build, then report honestly. |
| `commands/release.md` | `/release <version>` — guarded release checklist with signing and workflow verification. |
| `agents/android-reviewer.md` | Review subagent for correctness, privacy, lifecycle and launcher-behaviour regressions. |

Local overrides belong in `.claude/settings.local.json`, which is git-ignored. Do not commit personal paths, tokens or keystore material.

The root [CLAUDE.md](../CLAUDE.md), [AGENT.md](../AGENT.md) and [GEMINI.md](../GEMINI.md) are the authoritative project instructions. Keep this folder free of duplicated architecture detail to avoid drift.
