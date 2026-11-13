package org.stilllauncher.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.stilllauncher.util.Permissions
import org.stilllauncher.util.signingCertificateSha256

/**
 * Plain-language installation and permission troubleshooting. This screen
 * exists because self-distributed Android apps with accessibility features hit
 * Play Protect warnings and Android 13+ "restricted settings" by design.
 */
@Composable
fun InstallHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val fingerprint = remember { signingCertificateSha256(context) }
    var copied by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Install & access help", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp)
        ) {
            SectionLabel("Play Protect says the app may be unsafe")
            HelpParagraph(
                "Still Launcher is distributed directly from GitHub, not through Google Play. " +
                    "Play Protect shows a warning for apps from unknown developers, and it is " +
                    "extra cautious about apps that use accessibility features. The warning is a " +
                    "heuristic, not proof of a problem."
            )
            HelpParagraph(
                "Tap \"More details\" and then \"Install anyway\". If no install option appears, " +
                    "install from a computer with adb instead:"
            )
            HelpCode("adb install -r still-launcher-<version>-release.apk")
            HelpParagraph(
                "Only install APKs you got from this project's GitHub releases. Verify the " +
                    "SHA-256 sums and the signing fingerprint below. Do not permanently disable " +
                    "Play Protect for apps you do not trust."
            )

            SectionLabel("Accessibility is greyed out or says restricted")
            HelpParagraph(
                "Android 13 and newer block accessibility and notification access for apps " +
                    "installed outside an app store until you allow restricted settings. This is " +
                    "a system protection, not a bug in Still Launcher."
            )
            HelpStep(
                "1",
                "Open Settings → Apps → Still Launcher. On some phones the path is Settings → " +
                    "Apps → Manage apps (Xiaomi/HyperOS) or Apps → Still Launcher (Samsung)."
            )
            HelpStep(
                "2",
                "Tap the ⋮ menu in the top-right corner of the app info page and choose " +
                    "\"Allow restricted settings\" (the label may be translated). If the option " +
                    "is missing, first try to enable the service or notification access; " +
                    "Android will refuse once and then expose the menu item. Return to app info " +
                    "and try again."
            )
            HelpStep(
                "3",
                "Return to Accessibility and enable \"Still Launcher focus guard\". " +
                    "For notifications, open Settings → Notifications → Device & app " +
                    "notifications (or Special app access → Notification access) and allow " +
                    "Still Launcher."
            )
            HelpStep(
                "4",
                "Usage access is under Settings → Apps → Special app access → Usage access. " +
                    "It does not use restricted settings."
            )

            Column(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = { Permissions.openAppSettings(context) }) {
                    Text("Open app info (for “Allow restricted settings”)")
                }
                TextButton(onClick = { Permissions.openAccessibility(context) }) {
                    Text("Open accessibility settings")
                }
                TextButton(onClick = { Permissions.openNotificationListener(context) }) {
                    Text("Open notification access")
                }
                TextButton(onClick = { Permissions.openUsageAccess(context) }) {
                    Text("Open usage access")
                }
            }

            SectionLabel("Verify this build")
            HelpParagraph(
                "The signing certificate below belongs to this installed build. Official " +
                    "release builds are signed with the certificate published in the README. " +
                    "Debug builds show a different certificate."
            )
            Text(
                fingerprint ?: "Fingerprint unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("certificate SHA-256", fingerprint ?: "")
                    )
                    copied = true
                },
                enabled = fingerprint != null
            ) {
                Text(if (copied) "Copied" else "Copy fingerprint")
            }

            SectionLabel("Why these permissions?")
            HelpBullet("Usage access — detects the app you just left so blocking, limits, recents and screen time work.")
            HelpBullet("Accessibility — immediate foreground detection, Recents shortcut and the optional short-video guard. It does not read screen content for storage or upload.")
            HelpBullet("Notification access — the optional allow-list that filters distracting notifications.")
            HelpBullet("Display over other apps — lets the block or reminder screen appear over a blocked app on modern Android.")
            HelpBullet("Secure settings (adb only) — optional per-app grayscale. Not required for anything else.")
            Spacer(Modifier.height(20.dp))
            Text(
                "Every permission is optional and can be revoked at any time. Revoking access stops " +
                    "the feature that depends on it; it does not damage the app or your data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun HelpParagraph(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun HelpStep(number: String, text: String) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            number + ".",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HelpBullet(text: String) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HelpCode(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}
