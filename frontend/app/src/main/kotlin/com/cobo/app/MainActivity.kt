package com.cobo.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.cobo.app.ui.CoboRoot
import com.cobo.app.ui.CoboTheme
import com.cobo.app.ui.isDarkTheme

class MainActivity : ComponentActivity() {

    /**
     * 请求屏幕最高刷新率。部分系统对未声明的应用压到 60Hz，
     * 这里按当前屏幕支持的最高模式显式指定（行程动画的顺滑度直接依赖它）。
     */
    /** 系统「移除动画」（无障碍）：animator 时长缩放为 0。 */
    private fun isAnimationDisabled(): Boolean = runCatching {
        android.provider.Settings.Global.getFloat(
            contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)

    private fun requestHighestRefreshRate() {
        @Suppress("DEPRECATION")
        val modes = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
        else windowManager.defaultDisplay)?.supportedModes ?: return
        val best = modes.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        App.init(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()
        App.reducedMotion = isAnimationDisabled()

        setContent {
            CoboTheme {
                val dark = isDarkTheme()
                DisposableEffect(dark) {
                    // The window background is what shows during activity transitions and
                    // before the first Compose frame. Follow the app's own theme instead of
                    // the system's, or a dark app on a light system flashes white.
                    window.setBackgroundDrawable(
                        ColorDrawable(
                            if (dark) Color.rgb(0x30, 0x30, 0x30)
                            else Color.rgb(0xF0, 0xF1, 0xF2)
                        )
                    )
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    onDispose {}
                }
                CoboRoot()
            }
        }
    }
}
