package com.cobo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.theme.COUITheme

/** 页面底色：浅色 #F0F1F2，深色 #000000（Apple HIG 基底黑）。 */
@Composable
fun pageContainerColor(): Color = COUITheme.colorScheme.background
