package org.stilllauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.stilllauncher.data.Settings

@Composable
fun StillTheme(settings: Settings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val background = when (settings.themeId) {
        "black" -> Color(0xFF000000)
        "white" -> Color(0xFFFFFFFF)
        "eink" -> Color(0xFFC8C8C8)
        "custom" -> Color(settings.customColor.toInt())
        else -> if (systemDark) Color(0xFF0B0B0B) else Color(0xFFFFFFFF)
    }
    val dark = if (settings.themeId == "custom") {
        settings.customDark
    } else {
        background.luminance() < 0.5f
    }
    val foreground = if (dark) Color(0xFFF2F2F2) else Color(0xFF111111)
    val muted = if (dark) Color(0xFF9A9A9A) else Color(0xFF6A6A6A)
    val surfaceVariant = if (dark) Color(0xFF1C1C1C) else Color(0xFFEDEAE3)
    val error = if (dark) Color(0xFFFF8A80) else Color(0xFFB3261E)

    val scheme = if (dark) {
        darkColorScheme(
            primary = foreground,
            onPrimary = background,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            outline = muted,
            secondary = foreground,
            onSecondary = background,
            error = error,
            onError = background
        )
    } else {
        lightColorScheme(
            primary = foreground,
            onPrimary = background,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            outline = muted,
            secondary = foreground,
            onSecondary = background,
            error = error,
            onError = background
        )
    }

    val family = when (settings.fontId) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    val size = settings.fontSizeSp.sp
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = family, fontSize = size),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp - 2).coerceAtLeast(11).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp + 6).sp
        ),
        labelLarge = TextStyle(fontFamily = family, fontSize = size)
    )

    MaterialTheme(colorScheme = scheme, typography = typography) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = background,
            contentColor = foreground
        ) {
            content()
        }
    }
}
