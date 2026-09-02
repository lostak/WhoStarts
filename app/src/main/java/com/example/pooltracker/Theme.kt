package com.example.pooltracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pool-hall palette: felt green + brass/gold rail accents on a near-black backdrop.
val FeltGreenDark = Color(0xFF0B2F22)
val FeltGreen = Color(0xFF14432F)
val FeltGreenLight = Color(0xFF1C5A3D)
val Brass = Color(0xFFC9A24B)
val BrassLight = Color(0xFFE0C070)
val ChalkBlue = Color(0xFF4E7C8C)
val CueCream = Color(0xFFF3EFE2)
val Surface = Color(0xFF0E1A15)
val SurfaceVariant = Color(0xFF16261F)
val OnSurfaceMuted = Color(0xFFB7C6BE)

private val PoolColorScheme = darkColorScheme(
    primary = FeltGreenLight,
    onPrimary = CueCream,
    secondary = Brass,
    onSecondary = Color(0xFF241A00),
    tertiary = ChalkBlue,
    background = Surface,
    onBackground = CueCream,
    surface = SurfaceVariant,
    onSurface = CueCream,
    surfaceVariant = FeltGreenDark,
    onSurfaceVariant = OnSurfaceMuted,
    outline = FeltGreenLight,
    error = Color(0xFFCF6679)
)

@Composable
fun PoolTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PoolColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
