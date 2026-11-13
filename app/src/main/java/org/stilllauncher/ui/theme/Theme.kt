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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.stilllauncher.data.Settings

private val LightAccent = Color(0xFF2E6F61)
private val DarkAccent = Color(0xFF9ADBC9)

/**
 * Blends [base] toward [target]. Used to derive card, outline and accent
 * tints from the user's chosen background instead of hard-coding a second
 * palette per theme.
 */
private fun blend(base: Color, target: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = base.red + (target.red - base.red) * f,
        green = base.green + (target.green - base.green) * f,
        blue = base.blue + (target.blue - base.blue) * f,
        alpha = base.alpha + (target.alpha - base.alpha) * f
    )
}

private fun accentFor(settings: Settings, background: Color, dark: Boolean): Color = when {
    settings.themeId == "eink" -> Color(0xFF34514A)
    settings.themeId == "custom" -> {
        val base = Color(settings.customColor.toInt())
        if (dark) blend(base, Color.White, 0.55f) else blend(base, Color.Black, 0.42f)
    }
    dark -> DarkAccent
    else -> LightAccent
}

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
    val muted = if (dark) Color(0xFFA8A8A8) else Color(0xFF5D625F)
    val accent = accentFor(settings, background, dark)
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF0B1F1A) else Color.White
    val surfaceContainer = blend(background, foreground, if (dark) 0.09f else 0.055f)
    val surfaceContainerHigh = blend(background, foreground, if (dark) 0.14f else 0.10f)
    val primaryContainer = blend(background, accent, if (dark) 0.22f else 0.15f)
    val outline = blend(foreground, background, 0.48f)
    val outlineVariant = blend(foreground, background, 0.86f)
    val error = if (dark) Color(0xFFFF9A90) else Color(0xFFB3261E)
    val errorContainer = blend(background, error, if (dark) 0.22f else 0.12f)

    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainer,
            onPrimaryContainer = foreground,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = primaryContainer,
            onSecondaryContainer = foreground,
            tertiary = accent,
            onTertiary = onAccent,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = surfaceContainerHigh,
            onSurfaceVariant = muted,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerLow = blend(background, foreground, 0.045f),
            outline = outline,
            outlineVariant = outlineVariant,
            error = error,
            onError = background,
            errorContainer = errorContainer,
            onErrorContainer = error
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainer,
            onPrimaryContainer = foreground,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = primaryContainer,
            onSecondaryContainer = foreground,
            tertiary = accent,
            onTertiary = onAccent,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = surfaceContainerHigh,
            onSurfaceVariant = muted,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerLow = blend(background, foreground, 0.03f),
            outline = outline,
            outlineVariant = outlineVariant,
            error = error,
            onError = Color.White,
            errorContainer = errorContainer,
            onErrorContainer = error
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
        bodyLarge = TextStyle(
            fontFamily = family,
            fontSize = size,
            lineHeight = (settings.fontSizeSp * 1.35f).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp - 2).coerceAtLeast(11).sp,
            lineHeight = ((settings.fontSizeSp - 2).coerceAtLeast(11) * 1.4f).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp + 7).sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = ((settings.fontSizeSp + 7) * 1.2f).sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp + 1).sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = ((settings.fontSizeSp + 1) * 1.3f).sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontSize = size,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontSize = (settings.fontSizeSp - 3).coerceAtLeast(10).sp,
            fontWeight = FontWeight.Medium
        )
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
