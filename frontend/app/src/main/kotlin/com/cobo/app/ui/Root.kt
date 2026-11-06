package com.cobo.app.ui

import android.app.Activity
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.cobo.app.App
import com.cobo.app.data.UpdateCheckInterval
import com.cobo.app.data.Updater
import com.cobo.app.screen.AccountsScreen
import com.cobo.app.screen.ChangelogScreen
import com.cobo.app.screen.ChatScreen
import com.cobo.app.screen.ContainerScreen
import com.cobo.app.screen.DetailScreen
import com.cobo.app.screen.DraftsScreen
import com.cobo.app.screen.EditHistoryScreen
import com.cobo.app.screen.HistoryScreen
import com.cobo.app.screen.ImageViewerScreen
import com.cobo.app.screen.LoginScreen
import com.cobo.app.screen.NearbyScreen
import com.cobo.app.screen.ProfileScreen
import com.cobo.app.screen.PublishScreen
import com.cobo.app.screen.SearchScreen
import com.cobo.app.screen.SettingsScreen
import com.cobo.app.screen.TopicScreen
import com.cobo.app.screen.VideoScreen
import com.cobo.app.screen.VoteScreen
import com.cobo.app.screen.WebScreen
import com.cobo.app.ui.components.ImageSaver
import com.cobo.app.ui.components.LocalNavMotion
import com.cobo.app.ui.components.LocalImageSaver
import com.cobo.app.ui.components.SaveHud
import com.cobo.app.ui.components.SaveImagesMenu
import com.cobo.app.ui.components.UpdateDialog
import com.cobo.app.ui.components.rememberImageSaveState
import io.github.suqi8.coui.kmp.basic.PreciseClickState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.COUIPopupHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp

/**
 * How far a back gesture must pull before the page sits back on its card. A release from there
 * returns for good: the frame is already home, so springing the page open again reads as a bug.
 */
private const val LandedProgress = 0.2f
/** 起步阈值：边缘划动超过屏幕这个比例才进入返回预览，轻触不触发。 */
private const val PredictiveStartDrag = 0.08f
/** 提交阈值：划到屏幕这个比例松手才算完成返回。 */
private const val PredictiveCommitDrag = 0.45f
/** 关闭方向的甩动速度（进度/秒）：足够快的甩动可以越过距离阈值直接提交。 */
private const val PredictiveFlingVelocity = -2f

@Composable
fun CoboRoot() {
    val nav = remember { NavState() }
    val scope = rememberCoroutineScope()
    SideEffect { nav.attach(scope) }

    // 启动时静默查一次更新：开关打开、且距上次检查超过节流窗口才真的发请求。
    LaunchedEffect(Unit) {
        if (!App.settings.autoCheckUpdate) return@LaunchedEffect
        val elapsed = System.currentTimeMillis() - App.settings.updateCheckedAt
        if (elapsed < UpdateCheckInterval) return@LaunchedEffect
        App.checkUpdate(manual = false)
    }

    // The search field keeps focus after its result opens a page, and the IME's candidate bar
    // then floats over whatever is on top. Drop focus on every navigation.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(nav.stack.size) { focusManager.clearFocus(force = true) }

    CoboPredictiveBackHandler(nav)

    val linkHandler = remember(nav, scope) {
        { raw: String ->
            // 「评论配图」是 weibo.cn/sinaurl?u=<图片直链>，先拆出真实地址再决定去哪。
            val url = unwrapSinaUrl(raw) ?: raw
            val voteId = weiboVoteId(url)
            val statusId = weiboStatusId(url)
            val uid = weiboUserId(url)
            val mention = weiboMentionName(url)
            when {
                voteId != null -> nav.push(Route.Vote(voteId))
                isImageLink(url) -> nav.push(Route.Images(listOf(url), 0))
                statusId != null -> nav.push(Route.Detail(statusId, ""))
                uid != null -> nav.push(Route.Profile(uid))
                // @ 只有昵称，得先换成 uid；换不到才退回内置 WebView。
                mention != null -> scope.launch {
                    val target = runCatching { App.repo.uidForName(mention) }.getOrNull()
                    if (target != null) {
                        nav.push(Route.Profile(target))
                    } else {
                        resolveWeiboLink(raw)?.let { nav.push(Route.Web(it, mention)) }
                    }
                }
                isWeiboShortLink(url) -> scope.launch {
                    val resolved = App.repo.resolveShortUrl(url)
                    val vid = weiboVoteId(resolved)
                    if (vid != null) {
                        nav.push(Route.Vote(vid))
                    } else {
                        val sid = weiboStatusId(resolved)
                        if (sid != null) {
                            nav.push(Route.Detail(sid, ""))
                        } else {
                            resolveWeiboLink(resolved)?.let { nav.push(Route.Web(it, "网页")) }
                        }
                    }
                }
                else -> resolveWeiboLink(url)?.let { nav.push(Route.Web(it, "网页")) }
            }
            Unit
        }
    }

    // 长按缩略图弹「保存原图」的菜单挂在根节点，任何页面都不用自己搭浮层。
    var saveTargets by remember { mutableStateOf<List<String>?>(null) }
    var saveIndex by remember { mutableIntStateOf(0) }
    var savePrecise by remember { mutableStateOf<PreciseClickState?>(null) }
    val saveState = rememberImageSaveState()
    val imageSaver = remember {
        ImageSaver { urls, index, precise ->
            saveTargets = urls
            saveIndex = index
            savePrecise = precise
        }
    }
    // 玻璃菜单要有一层能采样的底：把整摞路由录进 backdrop，菜单浮在上面做模糊和外发光。
    // 录制节点和读 backdrop 的节点必须是兄弟，否则菜单会把自己模糊过的样子再采一遍。
    val rootBackdrop = rememberLayerBackdrop { drawContent() }
    // 缩略图把自己的屏幕位置登记在这里，看图页才能从被点的那张长出来、再缩回去。
    val imageOrigins = remember { ImageOrigins() }

    // 顶栏和底栏在各自的节点里钉密度（见 [PinnedBarDensity]），页面内容跟随系统「显示大小」。
    // 以前这里把整棵树都钉在 360dp 短边，结果正文字号也跟着缩了一档。
    CompositionLocalProvider(
        LocalLinkHandler provides linkHandler,
        LocalImageSaver provides imageSaver,
        LocalImageOrigins provides imageOrigins,
        LocalNavMotion provides nav.animating,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val screenCorner = rememberScreenCornerRadius()
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(rootBackdrop)
            ) {
                // 每页都按屏幕超级椭圆裁角，裁掉的那一圈是透明的。窗口占满屏幕时它正好落在屏幕
                // 之外，看不见；下拉通知中心、控制中心会让窗口整块下沉缩小，那一圈就翻到屏幕里
                // 侧来了，而栈里每一页裁的是同一个角，于是四角一路漏到下面那一层路由上去。
                val cornerShape = remember(screenCorner) { SquircleShape(screenCorner) }
                val pageSurface = pageContainerColor()
                Box(Modifier.fillMaxSize().background(pageSurface))
                nav.stack.forEachIndexed { index, entry ->
                    key(entry.id) {
                        RouteLayer(
                            entry = entry,
                            index = index,
                            lastIndex = nav.stack.lastIndex,
                            widthPx = widthPx,
                            screenCorner = screenCorner,
                            nav = nav,
                        )
                    }
                }
                // 所以四角补一层页面底色：只画在超级椭圆外侧，页面本身一个像素都不盖，窗口怎么
                // 缩、缩到哪个大小，漏出来的都还是这一页的调子。
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .drawBehind {
                            val cut = when (val outline = cornerShape.createOutline(size, layoutDirection, this)) {
                                is Outline.Generic -> outline.path
                                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                                else -> return@drawBehind
                            }
                            clipPath(cut, ClipOp.Difference) { drawRect(pageSurface) }
                        },
                )
            }

            // HUD 独立于菜单，菜单收起之后它还要自己活 1.6 秒。
            Box(Modifier.fillMaxSize().zIndex(10f)) { SaveHud(saveState) }

            // 菜单走独立窗口宿主（见 SaveImagesMenu）：收起一开始就把触摸交还给页面，不必等它
            // 完全收回去才能滑屏。独立窗口采不到页面内容，菜单弹出时自己抓一帧页面当玻璃底；
            // 这块 backdrop 只给本窗口里别的玻璃控件用。
            //
            // 弹窗宿主也挂在这里：COUI 的 OverlayDialog 走 glass surface，采不到 backdrop 就退回
            // 不透明底色，胶囊按钮的半透明白底会直接糊在面板上。挂进 backdrop 之后面板才真的是
            // 磨砂玻璃，按钮才浮得起来。
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            ) {
                CompositionLocalProvider(LocalBackdrop provides rootBackdrop) {
                    SaveImagesMenu(
                        urls = saveTargets,
                        index = saveIndex,
                        precise = savePrecise,
                        onDismiss = { saveTargets = null },
                        state = saveState,
                    )
                    COUIPopupHost()
                }
            }

            // 更新提示挂在根节点：自动检查与设置页的手动检查都往 App.pendingUpdate 里写。
            UpdatePrompt()
        }
    }
}

/** 单独一层，只让它读 [App.pendingUpdate]，免得更新状态一变就重组整摞路由。 */
@Composable
private fun UpdatePrompt() {
    val info = App.pendingUpdate ?: return
    UpdateDialog(
        info = info,
        preview = App.previewingUpdate,
        onDismiss = { App.dismissUpdate() },
        onSkip = {
            App.settings.applySkippedUpdate(info.version)
            App.dismissUpdate()
        },
        onDownload = {
            Updater.openDownload(info)
            App.dismissUpdate()
        },
    )
}

/**
 * Drives the page-follow back animation. Routes with their own nested back handler (the search
 * page's SearchBar) call this again after that handler so the predictive gesture reaches the
 * navigator instead of collapsing the route without an animation.
 *
 * 设置里关掉预测性返回后，这里换成普通的 [BackHandler]：返回键照常能用，只是页面不再跟着
 * 手指往回收，抬手才走。
 */
@Composable
fun CoboPredictiveBackHandler(nav: NavState) {
    val context = LocalContext.current
    if (!App.settings.predictiveBack) {
        BackHandler(enabled = true) {
            val entry = nav.stack.lastOrNull()
            when {
                entry == null || nav.stack.size <= 1 -> (context as? Activity)?.moveTaskToBack(true)
                entry.exiting -> Unit
                else -> nav.pop()
            }
        }
        return
    }
    PredictiveBackHandler(enabled = true) { events ->
        val entry = nav.stack.lastOrNull()
        if (entry == null || nav.stack.size <= 1) {
            // Nothing to reveal. Consume the gesture so the system does not run its own
            // scale-and-dim preview, then leave the app when the finger lifts.
            events.collect { }
            (context as? Activity)?.moveTaskToBack(true)
            return@PredictiveBackHandler
        }
        if (entry.exiting) {
            events.collect { }
            return@PredictiveBackHandler
        }

        var previewStarted = false
        var sawGesture = false
        var lastRawValue = 1f
        var lastProgress = 1f
        var lastDisplay = 1f
        var lastAt = 0L
        var velocity = 0f

        suspend fun cancelPreview() {
            withContext(NonCancellable) {
                coroutineScope {
                    launch { entry.progress.animateTo(1f, HigTransition) }
                    if (entry.expand) {
                        launch { entry.fade.animateTo(1f, HigTransition) }
                    }
                }
            }
            entry.previewing = false
        }

        try {
            events.collect { event ->
                val value = (1f - event.progress).coerceIn(0f, 1f)
                val rawDrag = 1f - value
                // 端点值（按钮/键盘返回的那一发终点事件）不算跟手，避免按钮返回被当成手势。
                if (value > 0.001f && value < 0.999f) sawGesture = true
                lastRawValue = value

                // Apple §10：先要一个明确的起步阈值，再开始跟手；不能一碰边缘就退。
                if (!previewStarted && sawGesture && rawDrag >= PredictiveStartDrag) {
                    previewStarted = true
                    entry.previewing = true
                    entry.origin?.let { it.refreshKey++ }
                }
                if (previewStarted) {
                    // 前 8% 只用来确认手势；越过阈值后从 1 开始 1:1 跟手，避免起手跳变。
                    val display = (value / (1f - PredictiveStartDrag)).coerceIn(0f, 1f)
                    val now = android.os.SystemClock.uptimeMillis()
                    if (lastAt != 0L && now > lastAt) {
                        val dt = ((now - lastAt) / 1000f).coerceAtLeast(1f / 240f)
                        val instant = (display - lastDisplay) / dt
                        val alpha = (1f - exp(-dt / 0.08f)).coerceIn(0f, 1f)
                        velocity += (instant - velocity) * alpha
                        velocity = velocity.coerceIn(-6f, 0f)
                    }
                    lastAt = now
                    lastDisplay = display
                    lastProgress = display
                    entry.progress.snapTo(display)
                    if (entry.expand) entry.fade.snapTo(display)
                }
            }

            if (!sawGesture) {
                // 按钮 / 键盘返回：没有跟手过程，走完整退场。
                entry.previewing = false
                nav.pop()
                return@PredictiveBackHandler
            }
            if (!previewStarted) {
                // 只是边缘轻微划动：不返回，页面留在原地。
                entry.previewing = false
                return@PredictiveBackHandler
            }

            val rawDrag = 1f - lastRawValue
            val commit = lastProgress <= LandedProgress ||
                rawDrag >= PredictiveCommitDrag ||
                velocity <= PredictiveFlingVelocity
            if (commit) {
                if (lastProgress <= LandedProgress) nav.completePredictivePop(entry)
                else nav.pop(velocity = velocity)
            } else {
                // 没划到位、也不是有效甩动：弹簧回原位，不触发返回。
                cancelPreview()
            }
        } catch (cancelled: CancellationException) {
            if (lastProgress <= LandedProgress && previewStarted) {
                nav.completePredictivePop(entry)
                return@PredictiveBackHandler
            }
            if (previewStarted) cancelPreview() else entry.previewing = false
            throw cancelled
        }
    }
}

@Composable
private fun RouteLayer(
    entry: NavEntry,
    index: Int,
    lastIndex: Int,
    widthPx: Float,
    screenCorner: Dp,
    nav: NavState,
) {
    // Read in layer and draw blocks only, so a running gesture invalidates the layer
    // instead of recomposing the whole route.
    val progress: () -> Float = { entry.progress.value }
    val isTop = index == lastIndex
    val belowTop = index == lastIndex - 1
    val top = nav.stack.lastOrNull()
    val topProgress: () -> Float = { top?.progress?.value ?: 1f }
    // The viewer draws its own growth out of the thumbnail, so the host must leave it in place
    // instead of sliding it in from the right.
    val slide = top != null && !top.expand && !top.growsFromImage
    // ColorOS draws the screen corners as an Lp superellipse (hwui "G2", n≈2.5), not a circular
    // arc. A RoundedCornerShape parts from that curve by 0.0718 * radius along the diagonal —
    // 11px at this device's 150px corner — so the four corners showed a crescent of window
    // background. SquircleShape traces the same silhouette.
    val cornerShape = remember(screenCorner) { SquircleShape(screenCorner) }

    val layer = Modifier
        .fillMaxSize()
        .zIndex(if (isTop) 2f else 1f)
        .then(
            if (isTop) {
                // Consume taps on empty areas so they cannot fall through to the page
                // below, but let drags reach the scrollable content.
                Modifier.pointerInput(Unit) { detectTapGestures { } }
            } else {
                Modifier
            }
        )
        .graphicsLayer {
            if (slide) {
                val p = progress().coerceIn(0f, 1f)
                val below = topProgress().coerceIn(0f, 1f)
                translationX = when {
                    isTop -> (1f - p) * widthPx
                    belowTop -> -below * widthPx / RouteParallaxDivisor
                    else -> 0f
                }
                shape = cornerShape
                clip = true
            }
        }

    // The viewer reads its own entrance curve through this; everything else is moved by the host.
    val transition = remember(entry) {
        RouteTransition(
            progress = progress,
            exiting = { entry.exiting || entry.previewing },
        )
    }

    CompositionLocalProvider(LocalRouteTransition provides transition) {
        Box(layer) {
            if (entry.expand) {
                ExpandLayer(entry, screenCorner, progress, { entry.fade.value }) {
                    RouteContent(entry.route, nav)
                }
            } else {
                RouteContent(entry.route, nav)
            }
        }
    }
}

@Composable
private fun ExpandLayer(
    entry: NavEntry,
    screenCorner: Dp,
    progress: () -> Float,
    fade: () -> Float,
    content: @Composable () -> Unit,
) {
    val origin = entry.origin ?: return
    val bounds = origin.bounds ?: return
    val density = LocalDensity.current
    val statusBarTop = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val surface = pageContainerColor()
    // A gesture pulling the page away is an exit too: the frame has to hand back to the card
    // under the finger instead of waiting for the pop.
    val exiting = entry.exiting || entry.previewing
    // 帧完整映射行程：跟手时 1:1 跟随手指到落点，不设提前落定的死区；
    // 按钮 / 普通返回同样走完整行程。弹簧带初速时可能过冲，视觉上钳在 [0,1]，
    // 否则帧会缩得比卡片还小再弹回来。
    fun frameProgress(p: Float): Float = p.coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // Only while the page grows over the one it came from. On the way out the
                // page underneath is exactly what the user is returning to, and dimming it
                // made the reveal land on a brighter screen than the one the gesture showed.
                if (!exiting) {
                    drawRect(Color.Black, alpha = 0.3f * frameProgress(progress()))
                }
            }
            .graphicsLayer {
                val p = frameProgress(progress())
                val left = lerp(bounds.left, 0f, p)
                val top = lerp(bounds.top, 0f, p)
                val width = lerp(bounds.width, size.width, p)
                val height = lerp(bounds.height, size.height, p)
                val scale = if (size.width > 0f && width > 0f) width / size.width else 1f
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = left
                translationY = top
                scaleX = scale
                scaleY = scale
                val radius = lerp(
                    origin.cornerRadius.toPx(),
                    screenCorner.toPx(),
                    p,
                ) / scale
                shape = ExpandShape(height / scale, radius)
                clip = true
            }
            // Drawn inside the frame's layer, so the card is clipped to the frame and travels
            // with it instead of being painted over the list around it.
            .drawWithContent {
                val p = frameProgress(progress())
                // On the way out the card fades back in over the page between these two points,
                // cross-fading instead of leaving a gap: a back gesture can hold the frame
                // anywhere, and a gap would show the bare fill for as long as the finger stays.
                val cardAlpha = if (exiting) {
                    ((0.55f - p) / 0.45f).coerceIn(0f, 1f)
                } else {
                    1f - (fade() / 0.25f).coerceIn(0f, 1f)
                }
                drawContent()
                val layerSize = origin.layer.size
                if (cardAlpha > 0f && layerSize.width > 0 && layerSize.height > 0 && bounds.width > 0f) {
                    // Scaled by width alone and anchored at the frame's top-left: the card keeps
                    // its own proportions, and a snapshot taller than the card is cropped by the
                    // frame rather than squashed into it.
                    val k = size.width / bounds.width
                    withTransform({ scale(k, k, pivot = Offset.Zero) }) {
                        clipRect(0f, 0f, bounds.width, bounds.height) {
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(
                                    Rect(0f, 0f, bounds.width, bounds.height),
                                    Paint().apply { alpha = cardAlpha },
                                )
                                // The card's surface is a translucent tint that the snapshot carries
                                // without a base; filling the page tone behind it keeps the page from
                                // showing through and keeps the card the same tone as in the list.
                                drawRect(surface, size = bounds.size)
                                drawLayer(origin.layer)
                                canvas.restore()
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // The frame keeps the page's own tone whatever the page is doing: on the way out
                // the list underneath would otherwise show through the shrinking frame.
                .background(surface)
                .graphicsLayer {
                    // Give way to the card as it fades back in, so one of the two always covers
                    // the frame and there is never a bare fill on screen.
                    val p = frameProgress(progress())
                    alpha = if (exiting) {
                        1f - ((0.55f - p) / 0.45f).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val f = fade()
                        val p = frameProgress(progress())
                        alpha = if (exiting) 1f else ((f - 0.25f) / 0.3f).coerceIn(0f, 1f)
                        translationY = -statusBarTop * (1f - p)
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun RouteContent(route: Route, nav: NavState) {
    when (route) {
        Route.Main -> MainShell(nav)
        is Route.Login -> LoginScreen(nav, route.addAccount, route.fresh, route.relogin)
        is Route.Detail -> DetailScreen(nav, route.id, route.mid, route.readOnly || App.repo.guest)
        is Route.EditHistory -> EditHistoryScreen(nav, route.mid)
        is Route.Profile -> ProfileScreen(nav, route.uid)
        Route.Search -> SearchScreen(nav)
        is Route.Publish -> PublishScreen(nav, route.draft)
        is Route.Chat -> ChatScreen(nav, route.uid, route.name)
        is Route.Web -> WebScreen(nav, route.url, route.title)
        is Route.Container -> ContainerScreen(nav, route.cid, route.title)
        Route.Drafts -> DraftsScreen(nav)
        Route.History -> HistoryScreen(nav)
        Route.Accounts -> AccountsScreen(nav)
        Route.Settings -> SettingsScreen(nav)
        Route.Changelog -> ChangelogScreen(nav)
        Route.Nearby -> NearbyScreen(nav)
        is Route.Topic -> TopicScreen(nav, route.name)
        is Route.Images -> ImageViewerScreen(nav, route.urls, route.index)
        is Route.Video -> VideoScreen(nav, route.video, route.statusId)
        is Route.Vote -> VoteScreen(nav, route.voteId)
    }
}

private class ExpandShape(
    private val heightPx: Float,
    private val radiusPx: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val height = heightPx.coerceIn(0f, size.height)
        // Same smooth-corner silhouette as the resting frame: a circular corner here would
        // pull away from the screen's squircle for the last stretch of the growth.
        val path = Path()
        path.addSquircleRect(
            width = size.width,
            height = height,
            cornerRadius = radiusPx,
        )
        return Outline.Generic(path)
    }
}

@Composable
private fun rememberScreenCornerRadius(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val radius = context.display
                    ?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                    ?.radius ?: 0
                with(density) { radius.toDp() }
            }.getOrDefault(0.dp)
        } else {
            0.dp
        }
    }
}
