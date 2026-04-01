package com.skinsense.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MedicalSecondary,
    onPrimary = MedicalSurface,
    primaryContainer = MedicalSecondary.copy(alpha = 0.1f),
    onPrimaryContainer = MedicalSecondary,
    
    secondary = MedicalPrimary,
    onSecondary = MedicalSurface,
    secondaryContainer = MedicalPrimary.copy(alpha = 0.1f),
    onSecondaryContainer = MedicalPrimary,
    
    tertiary = MedicalAccent,
    onTertiary = MedicalSurface,
    
    background = MedicalBackground,
    onBackground = TextMain,
    
    surface = MedicalSurface,
    onSurface = TextMain,
    surfaceVariant = MedicalBackground,
    onSurfaceVariant = TextMuted,
    
    outline = BorderLight,
    error = Error,
    onError = Color.White
)

@Composable
fun SkinSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We'll stick to Light mode for that clean, sterile medical feel, 
    // but the system could be extended for Dark mode later.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
