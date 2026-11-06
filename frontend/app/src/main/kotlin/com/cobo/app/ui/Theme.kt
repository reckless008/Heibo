package com.cobo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.cobo.app.App
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme

/**
 * The light page surface. COUI's light scheme pairs a white card with this grey page; leaving the
 * page white made every card invisible and the app look like one flat sheet.
 */
private val LightPage = Color(0xFFF0F1F2)

/** 调灰的目标灰蓝。 */
private val AccentGray = Color(0xFF6E7683)

/** 强调蓝整体调灰：亮/暗两套主色都往灰蓝掺 32%（原正文链接 22% + 再灰 10%），全局统一。 */
private fun toned(c: Color): Color = lerp(c, AccentGray, 0.32f)

@Composable
fun isDarkTheme(): Boolean = when (App.settings.darkMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun CoboTheme(content: @Composable () -> Unit) {
    val mode = App.settings.darkMode
    val controller = remember(mode) {
        ThemeController(
            colorSchemeMode = when (mode) {
                1 -> ColorSchemeMode.Light
                2 -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            },
            lightColors = lightColorScheme(
                background = LightPage,
                primary = toned(Color(0xFF0080FF)),
                primaryVariant = toned(Color(0xFF0080FF)),
                primaryContainer = toned(Color(0xFF0080FF)),
                onTertiaryContainer = toned(Color(0xFF0080FF)),
                sliderKeyPointForeground = toned(Color(0xFF6EB5FF)),
            ),
            darkColors = darkColorScheme(
                primary = toned(Color(0xFF1A8CFF)),
                primaryVariant = toned(Color(0xFF1A8CFF)),
                primaryContainer = toned(Color(0xFF1A8CFF)),
                onTertiaryContainer = toned(Color(0xFF1A8CFF)),
                // Apple HIG 深色层级：基底黑 → 卡片 → 浮起 → 最高，分隔线单独一档。
                // 旧版页面 #1E1E1E + 卡片 10% 白（#353535）层级偏亮且转发块与卡片同色。
                background = Color(0xFF000000),
                onBackground = Color(0xEBFFFFFF),
                surface = Color(0xFF000000),
                onSurface = Color(0xEBFFFFFF),
                onSurfaceSecondary = Color(0x99FFFFFF),
                surfaceContainer = Color(0xFF1C1C1E),
                onSurfaceContainer = Color(0xEBFFFFFF),
                surfaceContainerHigh = Color(0xFF2C2C2E),
                surfaceContainerHighest = Color(0xFF3A3A3C),
                outline = Color(0xFF38383A),
                dividerLine = Color(0xFF38383A),
            ),
        )
    }
    val base = LocalDensity.current
    val scale = App.settings.fontScale
    val density = remember(base, scale) { Density(base.density, base.fontScale * scale) }

    COUITheme(controller = controller) {
        CompositionLocalProvider(LocalDensity provides density) {
            content()
        }
    }
}
