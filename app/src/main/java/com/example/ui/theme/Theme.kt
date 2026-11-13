package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  onPrimary = EmeraldOnPrimaryDark,
  primaryContainer = EmeraldContainerDark,
  onPrimaryContainer = EmeraldOnContainerDark,
  secondary = GoldSecondaryDark,
  onSecondary = GoldOnSecondaryDark,
  secondaryContainer = GoldContainerDark,
  onSecondaryContainer = GoldOnContainerDark,
  tertiary = SlateTertiaryDark,
  onTertiary = SlateOnTertiaryDark,
  tertiaryContainer = SlateContainerDark,
  onTertiaryContainer = SlateOnContainerDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceContainerLow = SurfaceContainerLowDark,
  surfaceContainer = SurfaceContainerLowDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  error = ErrorDark,
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimaryLight,
  onPrimary = EmeraldOnPrimaryLight,
  primaryContainer = EmeraldContainerLight,
  onPrimaryContainer = EmeraldOnContainerLight,
  secondary = GoldSecondaryLight,
  onSecondary = GoldOnSecondaryLight,
  secondaryContainer = GoldContainerLight,
  onSecondaryContainer = GoldOnContainerLight,
  tertiary = SlateTertiaryLight,
  onTertiary = SlateOnTertiaryLight,
  tertiaryContainer = SlateContainerLight,
  onTertiaryContainer = SlateOnContainerLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceContainerLow = SurfaceContainerLowLight,
  surfaceContainer = SurfaceContainerLowLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  error = ErrorLight,
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun ChallengeTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
