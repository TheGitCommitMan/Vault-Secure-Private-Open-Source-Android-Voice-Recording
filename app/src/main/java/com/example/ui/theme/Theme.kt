package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CleanMinimalColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = TextMedium,
    tertiary = BadgeText,
    background = BgLight,
    surface = CardBgLight,
    onPrimary = CardBgLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanMinimalColorScheme,
        typography = Typography,
        content = content
    )
}
