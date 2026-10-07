package com.shlok.taskcheck.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.shlok.taskcheck.data.Settings

data class Accent(val name: String, val color: Color)
data class Wallpaper(val name: String, val canvas: Color, val orbs: List<Color>)

val Accents = listOf(
    Accent("Indigo", Color(0xFF5B7CFD)), Accent("Cyan", Color(0xFF06C3E0)), Accent("Mint", Color(0xFF34C78A)),
    Accent("Rose", Color(0xFFFF6B8A)), Accent("Amber", Color(0xFFFFB830)), Accent("Purple", Color(0xFFA855F7)),
    Accent("Coral", Color(0xFFFF6B4A)), Accent("Sky", Color(0xFF4A9EFF)),
)

val Wallpapers = listOf(
    Wallpaper("Aurora", Color(0xFF0F0F23), listOf(Color(0x996366F1), Color(0x8022D3EE), Color(0x80A78BFA), Color(0x5934D399))),
    Wallpaper("Sunset", Color(0xFF1A0F0F), listOf(Color(0xB3FB7185), Color(0x99FBBF24), Color(0x80EF4444), Color(0x66F97316))),
    Wallpaper("Ocean", Color(0xFF051520), listOf(Color(0xA60EA5E9), Color(0x8C06B6D4), Color(0x803B82F6), Color(0x5910B981))),
    Wallpaper("Forest", Color(0xFF071210), listOf(Color(0x9922C55E), Color(0x734ADE80), Color(0x8015803D), Color(0x5965A30D))),
    Wallpaper("Lavender", Color(0xFF130F23), listOf(Color(0xA6A78BFA), Color(0x8CD8B4FE), Color(0x808B5CF6), Color(0x59C4B5FD))),
    Wallpaper("Graphite", Color(0xFF0E1117), listOf(Color(0x8064748B), Color(0x6694A3B8), Color(0x73475569), Color(0x4D334155))),
)

@Immutable
data class GlassPalette(
    val dark: Boolean, val fg: Color, val fg2: Color, val fg3: Color,
    val glass: Color, val glassStrong: Color, val highlight: Color, val edge: Color,
    val sheet: Color, val canvas: Color, val orbs: List<Color>, val accent: Color,
)

val PriorityColors = listOf(Color.Transparent, Color(0xFF4A9EFF), Color(0xFFFFB830), Color(0xFFFF4F4F))
val PriorityNames = listOf("None", "Low", "Medium", "High")
val Success = Color(0xFF34C78A)
val Danger = Color(0xFFFF4F4F)

val LocalGlass = staticCompositionLocalOf<GlassPalette> { error("GlasslistTheme missing") }
val LocalGlassK = compositionLocalOf { 1f }
val LocalMotion = compositionLocalOf { true }

private fun palette(dark: Boolean, accent: Color, wp: Wallpaper): GlassPalette =
    if (dark) GlassPalette(
        true, Color(0xF2F0F2FF), Color(0x8CF0F2FF), Color(0x4DF0F2FF),
        Color(0x73141428), Color(0xA6141428), Color(0x33FFFFFF), Color(0x0DFFFFFF),
        Color(0xF2141428), wp.canvas, wp.orbs, accent,
    ) else GlassPalette(
        false, Color(0xEB0A0C1E), Color(0x8C0A0C1E), Color(0x590A0C1E),
        Color(0x85FFFFFF), Color(0xB8FFFFFF), Color(0xE6FFFFFF), Color(0x1F3C50A0),
        Color(0xF5F4F6FF), androidx.compose.ui.graphics.lerp(wp.canvas, Color.White, 0.9f),
        wp.orbs.map { it.copy(alpha = it.alpha * 0.7f) }, accent,
    )

object Ty {
    val largeTitle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    val title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp)
    val callout = TextStyle(fontSize = 14.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val section = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

@Composable
fun GlasslistTheme(s: Settings, content: @Composable () -> Unit) {
    val dark = when (s.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val accent = Accents[s.accent.coerceIn(Accents.indices)].color
    val wp = Wallpapers[s.wallpaper.coerceIn(Wallpapers.indices)]
    val pal = remember(dark, accent, wp) { palette(dark, accent, wp) }
    val d = LocalDensity.current
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark } }
    }
    val scheme = if (dark) darkColorScheme(primary = accent, onPrimary = Color.White, surface = Color(0xFF16162B),
        surfaceContainerHigh = Color(0xFF1E1E38), surfaceContainerHighest = Color(0xFF26264A), onSurface = Color(0xFFF0F2FF), onSurfaceVariant = Color(0xFFB8BCD8))
    else lightColorScheme(primary = accent, onPrimary = Color.White, surface = Color(0xFFF4F6FF),
        surfaceContainerHigh = Color(0xFFFFFFFF), surfaceContainerHighest = Color(0xFFEAEEFF), onSurface = Color(0xFF0A0C1E), onSurfaceVariant = Color(0xFF50556E))
    CompositionLocalProvider(
        LocalGlass provides pal, LocalGlassK provides s.glass, LocalMotion provides !s.reduceMotion,
        LocalDensity provides Density(d.density, d.fontScale * s.textScale),
    ) { MaterialTheme(colorScheme = scheme, content = content) }
}
