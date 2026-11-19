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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val primary = if (darkTheme && themeColor == AppThemeColor.GREY) {
        Color(0xFF71717A)
    } else {
        themeColor.primaryColor
    }

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
        outline = Color(0xFF334155),
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
        outline = Color(0xFFE2E8F0),
        error = DangerRed,
        onError = Color.White
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
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
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
