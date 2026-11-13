# Security policy

## Supported versions

Security fixes are applied to the latest tagged release and the `main` branch. Development builds from the **Latest build** prerelease are provided for testing and receive no separate support.

| Version | Supported |
|---|---|
| latest release | yes |
| `main` / latest build | best effort |
| older releases | no |

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Use GitHub's private vulnerability reporting:

https://github.com/yash244466666/still-launcher/security/advisories/new

Include:

- the affected version and Android version,
- steps to reproduce, if safe to share,
- the impact you believe it has,
- whether the issue requires a malicious app, a physical device, or user interaction.

You can expect an initial response within seven days. Please allow time for a fix before public disclosure.

## Scope

The attack surface is deliberately narrow: the app has no network permission, no analytics and no third-party runtime services. Areas worth reviewing:

- exported components (`MainActivity` is the only exported activity; the accessibility service is protected by `BIND_ACCESSIBILITY_SERVICE`),
- the notification listener's allow-list logic,
- handling of intents passed to `BlockedActivity` and `ReminderActivity` (both are `exported="false"`),
- the optional `WRITE_SECURE_SETTINGS` path for grayscale,
- DataStore persistence of app lists, blocks and limits.

Out of scope: force-stopping the app, revoking its permissions, or using a rooted/modified device to bypass enforcement. Those are documented limitations of a user-owned wellbeing tool, not vulnerabilities.
