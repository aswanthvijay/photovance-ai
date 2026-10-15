package com.photovance.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class AppAppearance(val title: String) { System("System"), Light("Light"), Dark("Dark") }

private val Obsidian = darkColorScheme(
    primary = Color(0xFFB39AFF), onPrimary = Color(0xFF211538),
    primaryContainer = Color(0xFF302541), onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFD3C4E9), onSecondary = Color(0xFF302637),
    secondaryContainer = Color(0xFF28222F), onSecondaryContainer = Color(0xFFE9DFF4),
    tertiary = Color(0xFFB6CBAA), onTertiary = Color(0xFF21301D),
    background = Color(0xFF111014), onBackground = Color(0xFFF5F0F8),
    surface = Color(0xFF19171E), onSurface = Color(0xFFF5F0F8),
    surfaceVariant = Color(0xFF242128), onSurfaceVariant = Color(0xFFBEB5C8),
    outline = Color(0xFF79717F), outlineVariant = Color(0xFF37313E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF442426), onErrorContainer = Color(0xFFFFDAD6)
)

private val Ivory = lightColorScheme(
    primary = Color(0xFF505B42), onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E6D7), onPrimaryContainer = Color(0xFF26301F),
    secondary = Color(0xFF6B6257), onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE5DB), onSecondaryContainer = Color(0xFF373026),
    tertiary = Color(0xFF695A82), onTertiary = Color.White,
    background = Color(0xFFFAF7F0), onBackground = Color(0xFF24251F),
    surface = Color(0xFFFFFCF6), onSurface = Color(0xFF24251F),
    surfaceVariant = Color(0xFFF0ECE3), onSurfaceVariant = Color(0xFF68665F),
    outline = Color(0xFF827D71), outlineVariant = Color(0xFFDED9CE),
    error = Color(0xFF9C3932), onError = Color.White,
    errorContainer = Color(0xFFFFE9E5), onErrorContainer = Color(0xFF6E211C)
)

@Composable
fun PhotovanceTheme(
    appearance: AppAppearance = AppAppearance.System,
    content: @Composable () -> Unit
) {
    val dark = when (appearance) {
        AppAppearance.System -> isSystemInDarkTheme()
        AppAppearance.Light -> false
        AppAppearance.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    val editorial = if (dark) FontFamily.SansSerif else FontFamily.Serif
    val base = Typography()
    MaterialTheme(
        colorScheme = if (dark) Obsidian else Ivory,
        typography = base.copy(
            displaySmall = base.displaySmall.copy(fontFamily = editorial, fontSize = 38.sp,
                lineHeight = 43.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp),
            headlineLarge = base.headlineLarge.copy(fontFamily = editorial, fontSize = 32.sp,
                lineHeight = 38.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.8).sp),
            headlineMedium = base.headlineMedium.copy(fontFamily = editorial, fontSize = 27.sp,
                lineHeight = 33.sp, fontWeight = FontWeight.Normal),
            titleLarge = base.titleLarge.copy(fontFamily = editorial, fontSize = 23.sp),
            bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp)),
        content = content
    )
}
