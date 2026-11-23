package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun KharchuTheme(
    themeColor: AppThemeColor = AppThemeColor.DEFAULT,
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isBlackTheme = themeMode == AppThemeMode.BLACK
    val useDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.BLACK -> true
        AppThemeMode.SYSTEM -> darkTheme
    }

    val primary = when {
        useDarkTheme && themeColor == AppThemeColor.GREY -> Color(0xFF71717A)
        useDarkTheme && themeColor == AppThemeColor.SLATE -> Color(0xFF64748B)
        else -> themeColor.primaryColor
    }

    val blackScheme = darkColorScheme(
        primary = primary,
        onPrimary = themeColor.onPrimaryColor,
        primaryContainer = primary.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = UpiIndigoAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF1F1F24),
        onSecondaryContainer = Color.White,
        background = Color(0xFF000000),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF0A0A0C),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF141418),
        onSurfaceVariant = Color(0xFFA1A1AA),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF08080A),
        surfaceContainer = Color(0xFF0E0E11),
        surfaceContainerHigh = Color(0xFF141418),
        surfaceContainerHighest = Color(0xFF1C1C21),
        outline = Color(0xFF27272A),
        outlineVariant = Color(0xFF1F1F23),
        error = DangerRed,
        onError = Color.White
    )

    val darkScheme = darkColorScheme(
        primary = primary,
        onPrimary = themeColor.onPrimaryColor,
        primaryContainer = primary.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = UpiIndigoAccent,
        onSecondary = Color.White,
        secondaryContainer = Slate700,
        onSecondaryContainer = Color.White,
        background = Color(0xFF090D16),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF0F172A),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceContainerLowest = Color(0xFF090D16),
        surfaceContainerLow = Color(0xFF0F172A),
        surfaceContainer = Color(0xFF162033),
        surfaceContainerHigh = Color(0xFF1E293B),
        surfaceContainerHighest = Color(0xFF273449),
        outline = Color(0xFF334155),
        outlineVariant = Color(0xFF1E293B),
        error = DangerRed,
        onError = Color.White
    )

    val lightScheme = lightColorScheme(
        primary = primary,
        onPrimary = themeColor.onPrimaryColor,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = UpiIndigo,
        onSecondary = Color.White,
        secondaryContainer = UpiIndigoLight,
        onSecondaryContainer = UpiIndigoDark,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color.White,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8FAFC),
        surfaceContainer = Color(0xFFF1F5F9),
        surfaceContainerHigh = Color(0xFFEAEFF5),
        surfaceContainerHighest = Color(0xFFE2E8F0),
        outline = Color(0xFFE2E8F0),
        outlineVariant = Color(0xFFE2E8F0),
        error = DangerRed,
        onError = Color.White
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isBlackTheme -> blackScheme
        useDarkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatible alias
@Composable
fun SpendLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KharchuTheme(
        themeColor = AppThemeColor.DEFAULT,
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
