package com.example.lugarfavorito.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    secondary = Color(0xFF4D6357),
    tertiary = Color(0xFF3E6374),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFF56DBA7),
    secondary = Color(0xFFB4CCBE),
    tertiary = Color(0xFFA6CDDF),
)

@Composable
fun LugarFavoritoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors,
        content = content,
    )
}
