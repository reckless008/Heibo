package com.cobo.app.ui.components

import kotlin.math.min

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cobo.app.App
import com.cobo.app.data.Membership
import com.cobo.app.ui.pageContainerColor
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.GlassContainer
import io.github.suqi8.coui.kmp.basic.HighlightedIconButtonDefaults
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.basic.rememberGlassHighlight
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.blur
import io.github.suqi8.coui.kmp.blur.colorControls
import io.github.suqi8.coui.kmp.blur.drawBackdrop
import io.github.suqi8.coui.kmp.blur.runtimeShaderEffect
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefreshTexts
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
fun Avatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val scheme = COUITheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 昵称后面的认证勾。搜索结果的用户行和信息流共用同一个尺寸比例。 */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val scheme = COUITheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", fontSize = (size.value * 0.64f).sp, color = scheme.onPrimary)
    }
}

/** 微博会员的红标：VIP / SVIP 两种，和微博客户端的两种标识一一对应。 */
@Composable
fun MemberBadge(
    membership: Membership,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (membership == Membership.None) return
    val svip = membership == Membership.Svip
    val shape = RoundedCornerShape(if (compact) 4.dp else 5.dp)
    Box(
        modifier = modifier
            .height(if (compact) 15.dp else 16.dp)
            .clip(shape)
            .background(if (svip) MemberSvipRed else MemberVipRed)
            .padding(horizontal = if (compact) 4.dp else 5.dp)
            .testTag(if (svip) "vip_badge_svip" else "vip_badge_vip"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (svip) "SVIP" else "VIP",
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 8.sp else 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** 微博会员的品牌红；超级会员用更深的一档，两者并排也能一眼分开。 */
private val MemberVipRed = Color(0xFFE6162D)
private val MemberSvipRed = Color(0xFFC4000B)

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val scheme = COUITheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(44.dp), tint = scheme.onSurfaceSecondary)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = text,
            color = scheme.onSurfaceSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = scheme.onSurfaceSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(text = "重试", onClick = onRetry)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = COUITheme.colorScheme.onSurfaceSecondary,
    )
}

@Composable
fun ActionIcon(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = COUITheme.colorScheme.onSurfaceSecondary,
    /** 只读流里按钮留着给人看计数，但不响应点击。 */
    enabled: Boolean = true,
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.38f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, text.ifBlank { null }, Modifier.size(17.dp), tint = color)
        if (text.isNotBlank()) {
            Text(text, fontSize = 12.sp, color = color)
        }
    }
}

/**
 * 玻璃边缘光影：一条受光发丝边 + 顶部受光 + 底缘收光。
 *
 * 折射不再用第二道 1dp 描边表达：原来的「1.6dp 内缩色散线」放大看就是第二圈轮廓，
 * 和底缘暗折线一起读成「双层」。冷暖色相直接混进唯一那条外缘的渐变里，内缘折光交给
 * 向内的柔光/暗折，边界始终只有一条。
 *
 * [lens] 为选中透镜：透镜嵌在容器玻璃里，边界由容器负责；透镜只保留内部受光，
 * 不再另画一圈边，避免容器边缘 + 透镜边缘叠成双线。
 */
fun Modifier.glassEdgeLight(dark: Boolean, lens: Boolean = false): Modifier = this.drawBehind {
        val w = 1.dp.toPx()
        val r = size.minDimension / 2f
        if (!lens) {
            // 唯一的外缘：顶亮、中段冷暖色散、底暗；一条线完成受光 + 色相。
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (dark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.95f),
                        Color(0xFFA8C8FF).copy(alpha = if (dark) 0.14f else 0.20f),
                        Color(0xFFFFD9A8).copy(alpha = if (dark) 0.10f else 0.16f),
                        if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                cornerRadius = CornerRadius(r - w / 2f),
                style = Stroke(w),
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
            )
        }
        // 倒角：贴近边缘的窄柔光，不是铺满半个表面的白雾。
        // 参考 Glasswork 的 box-shadow bevel：顶部 6px 白 0.5、左右 4px 0.22/0.12、底部 6px 黑 0.06。
        val bevelTop = min(6.dp.toPx(), size.height * 0.2f)
        val bevelSide = min(5.dp.toPx(), size.width * 0.15f)
        val bevelBottom = min(6.dp.toPx(), size.height * 0.2f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(
                        alpha = when {
                            dark -> 0.26f
                            lens -> 0.32f
                            else -> 0.5f
                        }
                    ),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = bevelTop,
            ),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(
                        alpha = when {
                            dark -> 0.10f
                            lens -> 0.12f
                            else -> 0.2f
                        }
                    ),
                    (bevelSide / size.width).coerceIn(0f, 0.45f) to Color.Transparent,
                    (1f - bevelSide / size.width).coerceIn(0.55f, 1f) to Color.Transparent,
                    1f to Color.White.copy(
                        alpha = when {
                            dark -> 0.05f
                            lens -> 0.06f
                            else -> 0.1f
                        }
                    ),
                ),
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(
                        alpha = when {
                            dark -> 0.10f
                            lens -> 0.04f
                            else -> 0.06f
                        }
                    ),
                ),
                startY = size.height - bevelBottom,
                endY = size.height,
            ),
        )
}

/**
 * 玻璃透镜：亮一档的半透玻璃 + [glassEdgeLight] 边缘光影（可选投影）。
 * 底栏 / 顶栏选中块 / 玻璃圆钮共用这一套绘制——层级来自材质厚度，不是颜色。
 */
fun Modifier.glassLens(
    shape: Shape,
    dark: Boolean,
    shadowElevation: Dp = 3.dp,
): Modifier = this
    .background(glassLensFill(dark))
    .glassEdgeLight(dark)
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.16f),
        spotColor = Color.Black.copy(alpha = 0.2f),
    )

/**
 * 圆形玻璃钮的专用绘制。
 *
 * 圆钮不能走 `background + clip(CircleShape)`：部分渲染器会把圆形 clip 的路径近似成八边形，
 * 圆钮里就浮出一圈八边形的亮面。这里直接用 drawCircle 画填充、唯一一条受光边、顶部受光和
 * 底缘收光，不依赖 clip，形状始终是真圆；边界参数与 [glassEdgeLight] 同源。
 */
fun Modifier.glassCircle(dark: Boolean): Modifier = this
    .drawBehind {
        val w = 1.dp.toPx()
        val r = size.minDimension / 2f
        drawCircle(
            color = glassLensFill(dark),
            radius = r,
        )
        // 唯一外缘：顶亮、中段冷暖色散、底暗；色相混在这一条线里，不另画内圈。
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (dark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.95f),
                    Color(0xFFA8C8FF).copy(alpha = if (dark) 0.14f else 0.20f),
                    Color(0xFFFFD9A8).copy(alpha = if (dark) 0.10f else 0.16f),
                    if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                ),
                startY = 0f,
                endY = size.height,
            ),
            radius = r - w / 2f,
            style = Stroke(w),
        )
        // 倒角：与胶囊同源的窄柔光，不铺满半个圆。
        val bevelTop = min(6.dp.toPx(), size.height * 0.2f)
        val bevelSide = min(5.dp.toPx(), size.width * 0.15f)
        val bevelBottom = min(6.dp.toPx(), size.height * 0.2f)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (dark) 0.26f else 0.5f),
                    Color.White.copy(alpha = 0f),
                ),
                startY = 0f,
                endY = bevelTop,
            ),
            radius = r,
        )
        drawCircle(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = if (dark) 0.10f else 0.2f),
                    (bevelSide / size.width).coerceIn(0f, 0.45f) to Color.Transparent,
                    (1f - bevelSide / size.width).coerceIn(0.55f, 1f) to Color.Transparent,
                    1f to Color.White.copy(alpha = if (dark) 0.05f else 0.1f),
                ),
            ),
            radius = r,
        )
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = if (dark) 0.10f else 0.06f),
                ),
                startY = size.height - bevelBottom,
                endY = size.height,
            ),
            radius = r,
        )
    }
    // 不再叠 Modifier.shadow：CircleShape 的 RenderNode 阴影轮廓在部分渲染器上会被近似成
    // 八边形，并盖在填充上（clip = false 也挡不住）。圆钮的区分度交给边缘光影与发丝边。

/**
 * 玻璃圆形图标钮：与导航栏同一套玻璃透镜材质（40dp 圆、按下回弹），
 * 顶栏搜索 / 发布用它，替换 COUI 的径向渐变圆钮。
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = HighlightedIconButtonDefaults.PressSpring,
        label = "glassIconPress",
    )
    Box(
        modifier
            .size(40.dp)
            .graphicsLayer {
                val scale = 1f - (1f - HighlightedIconButtonDefaults.PressedScale) * press
                scaleX = scale
                scaleY = scale
            }
            .glassCircle(isDark)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 玻璃胶囊文字钮：与玻璃透镜同款材质的 40dp 胶囊（消息页「通知」等顶栏动作用）。
 */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = HighlightedIconButtonDefaults.PressSpring,
        label = "glassPillPress",
    )
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .graphicsLayer {
                val scale = 1f - (1f - HighlightedIconButtonDefaults.PressedScale) * press
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .glassLens(shape, isDark)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = COUITheme.colorScheme.onSurface,
            )
        }
    }
}

/** 液态玻璃底色：亮底白玻璃、暗底近黑玻璃（与图片查看器控件同系）。 */
fun glassPlate(dark: Boolean): Color =
    if (dark) Color(0xFF141414) else Color.White

/** 导航栏胶囊的底色浓度：更透一点，苹果风靠边缘和折射说话，不靠色块。 */
fun glassPlateAlpha(dark: Boolean): Float =
    ((if (dark) 0.26f else 0.16f) * glassAlphaFactor()).coerceIn(0.04f, 0.92f)

/** 选中透镜/圆钮的填充：比容器亮一档，但仍是玻璃而不是白色实心块。 */
fun glassLensFill(dark: Boolean): Color =
    if (dark) Color(0x24FFFFFF) else Color(0x4DFFFFFF)

/**
 * 设置 → 显示与交互 → 玻璃透明度 的映射：40 = 标准（系数 1），数值越大玻璃越透。
 * 所有玻璃件（顶栏胶囊、底栏胶囊、动作小胶囊）共用这一档。
 */
fun glassAlphaFactor(): Float = (1.4f - App.settings.glassAlpha / 100f).coerceIn(0.35f, 1.35f)

/**
 * 液态玻璃材质：取样 backdrop 柔化 + 提饱和（苹果 vibrancy 的手感）+ 半透明底 + 高光边。
 * 比 COUI 默认的磨砂半透明更「透」——模糊更重、底色更淡、边缘高光更亮。
 *
 * couiGlassSurface 系会丢弃排在前面的 modifier，这里直接走 drawBackdrop，
 * 尺寸 / 位置 / 内边距接在调用之后即可。
 */
/** 边缘透镜：Glasswork 的 rim lens。把模糊后的背景在边缘向内侧偏移，R/B 各多/少一点形成色散。 */
private const val GLASS_LENS_SHADER = """
uniform shader child;
uniform float2 in_size;
uniform float in_padding;
uniform float in_radius;
uniform float in_rim;
uniform float in_edge;
uniform float in_power;
uniform float in_dispersion;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 coord) {
    float2 size = in_size;
    float2 center = size * 0.5 + float2(in_padding);
    float2 p = coord - center;
    float2 b = size * 0.5 - float2(0.5);
    float r = min(in_radius, min(b.x, b.y));
    float d = sdRoundRect(p, b, r);
    float inside = max(-d, 0.0);
    float t = clamp(inside / max(in_rim, 0.001), 0.0, 1.0);
    float fall = pow(1.0 - t, in_power);
    float2 gx = float2(
        sdRoundRect(p + float2(0.5, 0.0), b, r) - d,
        sdRoundRect(p + float2(0.0, 0.5), b, r) - d
    );
    float2 dir = normalize(gx + float2(0.0001));
    float off = in_rim * in_edge * fall;
    float2 uvR = coord - dir * (off * (1.0 + in_dispersion));
    float2 uvG = coord - dir * off;
    float2 uvB = coord - dir * (off * (1.0 - in_dispersion));
    half4 cR = child.eval(uvR);
    half4 cG = child.eval(uvG);
    half4 cB = child.eval(uvB);
    return half4(cR.r, cG.g, cB.b, cG.a);
}
"""

/** 路由转场/预测性返回进行中：玻璃暂停 backdrop 采样与折射，避免每帧重跑重效果。 */
val LocalNavMotion = compositionLocalOf { false }

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    alpha: Float,
    highlightAlpha: Float = 1f,
    /** 模糊半径（px）：大表面用更强的模糊读作「更厚的材质」（Apple Design §12）。 */
    blurRadius: Float = 12f,
): Modifier {
    val backdrop = LocalBackdrop.current
    val motion = LocalNavMotion.current
    if (backdrop == null || motion) return Modifier.background(tint.copy(alpha = alpha), shape)
    val highlight = rememberGlassHighlight(GlassContainer.Small, backdrop)?.copy(alpha = highlightAlpha)
    val dark = tint.luminance() < 0.5f
    return Modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            // Glasswork 的核心不是把背景糊白：2px 级轻模糊 + 高饱和 + 亮度，
            // 让玻璃取背后的颜色；这里按设备像素取 12px（底栏 16px）。
            effects = {
                blur(blurRadius)
                colorControls(
                    brightness = if (dark) -0.2f else 0.06f,
                    saturation = if (dark) 1.6f else 1.8f,
                )
                // 边缘折射：Glasswork 的 rim lens。必须在 blur/colorControls 之后，
                // 采样的是已经处理好色调的 backdrop。
                runtimeShaderEffect("HeiboGlassLens", GLASS_LENS_SHADER, "child") {
                    val ds = downscaleFactor.coerceAtLeast(1).toFloat()
                    val w = size.width / ds
                    val h = size.height / ds
                    val radius = min(w, h) / 2f
                    val rim = min(min(w, h) * 0.42f, 44.dp.toPx() / ds)
                    setFloatUniform("in_size", w, h)
                    setFloatUniform("in_padding", padding / ds)
                    setFloatUniform("in_radius", radius)
                    setFloatUniform("in_rim", rim)
                    setFloatUniform("in_edge", 1.1f)
                    setFloatUniform("in_power", 2.6f)
                    setFloatUniform("in_dispersion", 0.15f)
                }
            },
            highlight = { highlight },
        )
        .background(tint.copy(alpha = alpha), shape)
}

/**
 * 动作玻璃小胶囊（转发 / 评论 / 点赞）：液态玻璃底 + 发丝描边 + 图标与计数，
 * 与顶栏圆钮、底栏同属一套玻璃材质。深色下观感参考「分享 | 1136 | 10584」。
 * 区分度靠玻璃边缘而不是加深底色——底色刻意比导航栏还透，免得比上下栏抢眼。
 */
@Composable
fun GlassActionPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = COUITheme.colorScheme.onSurfaceSecondary,
    /** 只读流里胶囊留着给人看计数，但不响应点击。 */
    enabled: Boolean = true,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    // 比导航栏更透的一档：轮廓交给高光边和发丝线；整体随「玻璃透明度」设置缩放。
    // 小胶囊不适合玻璃透镜材质（用户要求），保持液态玻璃底 + 发丝描边。
    val plateAlpha = (glassPlateAlpha(isDark) * 0.72f).coerceIn(0.04f, 0.85f)
    val edgeColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.13f)
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "glassPillScale",
    )
    val color = if (enabled) tint else tint.copy(alpha = 0.38f)
    Row(
        modifier = modifier
            // 动作小胶囊是规范里的例外件：不采样背景（否则每张卡 3 枚 blur + 折射，
            // 列表滑动/转场时非常贵），只保留半透玻璃底 + 发丝边。
            .background(glassPlate(isDark).copy(alpha = plateAlpha), shape)
            // 固定尺寸：三枚等宽，不随计数长短变形。
            .width(96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .drawBehind {
                // 发丝边：玻璃边缘的第二圈轮廓，白底上也要看得出形状。
                val w = 1.dp.toPx()
                drawRoundRect(
                    color = edgeColor,
                    cornerRadius = CornerRadius(18.dp.toPx() - w / 2f),
                    style = Stroke(w),
                    topLeft = Offset(w / 2f, w / 2f),
                    size = Size(size.width - w, size.height - w),
                )
            }
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, text.ifBlank { null }, Modifier.size(17.dp), tint = color)
        if (text.isNotBlank()) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

/**
 * 顶栏文字按钮：高度与 [BackIconButton] 的正圆一致（40dp），与 [GlassPillButton] 同一套玻璃胶囊材质。
 */
@Composable
fun TopBarTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassPillButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

/**
 * 返回键：与顶栏搜索 / 发布同一套玻璃圆钮材质，替换 COUI 的亮面径向渐变圆钮。
 */
@Composable
fun BackIconButton(onClick: () -> Unit) {
    GlassIconButton(onClick = onClick) {
        Icon(COUIIcons.Back, "返回", Modifier.size(24.dp))
    }
}

/**
 * 设置卡片里分隔两行的细线。
 *
 * COUI 的 [HorizontalDivider] 是整宽画到卡片边缘的，而 ColorOS 17 的设置卡片里分割线只从
 * 行标题的左缘起画、右侧留同样的内缩。标题的左缘就是 [BasicComponentDefaults.InsideMargin]
 * 的横向值，这里直接取它，将来 COUI 改了这个常量分割线会自己跟上。
 */
@Composable
fun PreferenceDivider() {
    val inset = BasicComponentDefaults.InsideMargin.calculateLeftPadding(LayoutDirection.Ltr)
    HorizontalDivider(Modifier.padding(horizontal = inset))
}

@Composable
fun ClickableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        content()
    }
}

fun fmtCount(value: Int): String {
    if (value <= 0) return "0"
    if (value < 10000) return value.toString()
    val wan = value / 10000.0
    return if (wan >= 100) "${wan.toInt()}万" else String.format("%.1f万", wan)
}

val FullPageRefreshTexts = FullPagePullToRefreshTexts(
    pulling = "下拉刷新",
    thresholdReached = "松开刷新",
    refreshing = "刷新中…",
    complete = "刷新完成",
)

/** How long the success hint stays up before the page rebounds; COUI's 800ms default drags. */
const val FullPageRefreshHoldMillis = 200L

/**
 * 悬浮栏边缘的渐隐层（iOS 26 scroll edge effect）：内容滚到栏下方时先淡向页面底色。
 * 必须画在页面的 `layerBackdrop` 容器里，栏的玻璃才会取样到渐隐后的画面；
 * 静止时它铺在页面底色上，视觉无变化。
 */
@Composable
fun ScrollEdgeFade(
    height: Dp,
    fromBottom: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = pageContainerColor()
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                if (fromBottom) {
                    Brush.verticalGradient(
                        0f to color.copy(alpha = 0f),
                        0.30f to color.copy(alpha = 0.22f),
                        0.55f to color.copy(alpha = 0.66f),
                        0.75f to color.copy(alpha = 0.88f),
                        1f to color.copy(alpha = 0.96f),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to color.copy(alpha = 0.94f),
                        0.40f to color.copy(alpha = 0.72f),
                        0.75f to color.copy(alpha = 0.24f),
                        1f to color.copy(alpha = 0f),
                    )
                }
            )
    )
}

@Composable
fun navBottomPadding(): Dp =
    androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

@Composable
fun systemNavigationBarHeight(): Dp {
    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val px = view.rootWindowInsets
        ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        ?.bottom ?: 0
    val fallback = navBottomPadding()
    return if (px > 0) with(density) { px.toDp() } else maxOf(fallback, 20.dp)
}
