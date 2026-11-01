package com.cobo.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobo.app.App
import com.cobo.app.data.FeedPage
import com.cobo.app.data.FeedTabItem
import com.cobo.app.screen.FeedFilter
import com.cobo.app.screen.FeedStore
import com.cobo.app.screen.LocalHome
import com.cobo.app.screen.LocalMessagesPlaceholder
import com.cobo.app.screen.MessagesScreen
import com.cobo.app.screen.ProfileContent
import com.cobo.app.screen.TimelineScreen
import com.cobo.app.ui.components.glassPlate
import com.cobo.app.ui.components.glassPlateAlpha
import com.cobo.app.ui.components.liquidGlass
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.ListPopupDefaults
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.basic.SearchBarActionButton
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.ExpandableIndicator
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

internal sealed class FeedTabSpec {
    abstract val key: String
    abstract val label: String
    abstract val tag: String
    abstract val emptyText: String
    abstract val isFollow: Boolean

    data class Custom(val item: FeedTabItem, val position: String, val guest: Boolean) : FeedTabSpec() {
        override val key: String get() = "${position}_${item.key}"
        override val label: String get() = item.label
        override val tag: String get() = item.tag
        override val isFollow: Boolean get() = false
        override val emptyText: String get() = when (item) {
            FeedTabItem.Follow -> if (guest) "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里" else "还没有关注的人"
            FeedTabItem.Circle -> "好友圈还没有内容"
            FeedTabItem.Recommend -> "这里还没有微博"
            FeedTabItem.Local -> if (guest) "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里" else "还没有本地关注"
            FeedTabItem.Original -> "还没有原创微博"
            FeedTabItem.Special -> "特别关注还没有内容"
            FeedTabItem.Secret -> "还没有悄悄关注的人"
            FeedTabItem.Favorites -> "还没有收藏的微博"
            FeedTabItem.None -> "这里还没有微博"
        }
    }

    data class Follow(val filter: FeedFilter, val guest: Boolean, val position: String = "center") : FeedTabSpec() {
        override val key: String get() = if (position == "center") "follow" else "${position}_follow"
        override val label: String get() = if (guest) "本地关注" else filter.label
        override val tag: String get() = "feed_tab_follow"
        override val isFollow: Boolean get() = true
        override val emptyText: String get() = if (guest) {
            "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里"
        } else {
            filter.emptyText
        }
    }
}

@Composable
fun MainShell(nav: NavState) {
    val tab = nav.mainTab
    val holder = rememberSaveableStateHolder()
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    // The bar floats over all three pages, so the line it ends at is reported once here and read
    // by whatever has to be placed clear of it.
    val topBarBounds = remember { TopBarBounds() }

    // 纯本地模式：没有账号可读关注流，首页只剩「本地关注」和「推荐」两段，筛选菜单也收起来。
    // 登录 / 退出都会 reset 导航栈，这个值在整个 MainShell 生命周期里不会变。
    val guest = remember { App.repo.guest }
    val leftTab = App.settings.feedLeftTab
    val centerTab = App.settings.feedCenterTab
    val rightTab = App.settings.feedRightTab

    var filterIndex by remember {
        mutableIntStateOf(App.settings.feedFilter.coerceIn(0, FeedFilter.entries.lastIndex))
    }
    val filter = if (guest) FeedFilter.Local else FeedFilter.entries[filterIndex]
    val friendsStore = remember {
        FeedStore("friends") { cursor, force -> App.repo.friends(cursor, force) }
    }
    val filterStores = remember {
        mapOf(
            FeedFilter.All to friendsStore,
            FeedFilter.Following to friendsStore,
            FeedFilter.Original to FeedStore("original") { cursor, force ->
                originalFeed(cursor, force)
            },
            FeedFilter.Unread to FeedStore("unread") { _, _ -> FeedPage(emptyList(), null) },
            FeedFilter.Favorites to FeedStore("favorites") { _, force -> App.repo.favorites(force) },
            FeedFilter.Special to FeedStore("special") { cursor, force ->
                App.repo.special(cursor, force)
            },
            FeedFilter.Circle to FeedStore("circle") { cursor, force -> App.repo.circle(cursor, force) },
            FeedFilter.Secret to FeedStore("secret") { cursor, force ->
                App.repo.secret(cursor, force)
            },
            FeedFilter.Local to FeedStore("local", readOnly = true) { cursor, force ->
                App.repo.localFollows(cursor, force)
            },
        )
    }
    val recommendStore = remember { FeedStore("102803", readOnly = guest) }
    val circleStore = filterStores.getValue(FeedFilter.Circle)
    val followStore = filterStores.getValue(filter)

    fun storeFor(item: FeedTabItem): FeedStore = when (item) {
        FeedTabItem.Follow -> followStore
        FeedTabItem.Circle -> circleStore
        FeedTabItem.Recommend -> recommendStore
        FeedTabItem.Local -> filterStores.getValue(FeedFilter.Local)
        FeedTabItem.Original -> filterStores.getValue(FeedFilter.Original)
        FeedTabItem.Special -> filterStores.getValue(FeedFilter.Special)
        FeedTabItem.Secret -> filterStores.getValue(FeedFilter.Secret)
        FeedTabItem.Favorites -> filterStores.getValue(FeedFilter.Favorites)
        FeedTabItem.None -> friendsStore
    }

    // 动态构建主页 Tab 列表：左栏（若非隐藏） + 中间栏（不可隐藏） + 右栏（若非隐藏）
    val feedTabs: List<FeedTabSpec> = remember(leftTab, centerTab, rightTab, filter, guest) {
        buildList {
            if (leftTab != FeedTabItem.None) {
                if (leftTab == FeedTabItem.Follow) {
                    add(FeedTabSpec.Follow(filter, guest, "left"))
                } else {
                    add(FeedTabSpec.Custom(leftTab, "left", guest))
                }
            }
            if (centerTab == FeedTabItem.Follow) {
                add(FeedTabSpec.Follow(filter, guest, "center"))
            } else {
                add(FeedTabSpec.Custom(centerTab, "center", guest))
            }
            if (rightTab != FeedTabItem.None) {
                if (rightTab == FeedTabItem.Follow) {
                    add(FeedTabSpec.Follow(filter, guest, "right"))
                } else {
                    add(FeedTabSpec.Custom(rightTab, "right", guest))
                }
            }
        }
    }

    val feedStores = feedTabs.map { spec ->
        when (spec) {
            is FeedTabSpec.Follow -> followStore
            is FeedTabSpec.Custom -> storeFor(spec.item)
        }
    }
    val feedEmptyTexts = feedTabs.map { it.emptyText }

    val followIndex = feedTabs.indexOfFirst { it.isFollow }.coerceAtLeast(0)
    val initialPage = feedTabs.indexOfFirst { it.key == App.settings.feedTabKey }.takeIf { it >= 0 }
        ?: followIndex

    val feedPager = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (feedTabs.size - 1).coerceAtLeast(0)),
        pageCount = { feedTabs.size },
    )
    LaunchedEffect(feedPager.currentPage, feedTabs) {
        val currentTab = feedTabs.getOrNull(feedPager.currentPage)
        if (currentTab != null) {
            App.settings.applyFeedTabKey(currentTab.key)
            val legacyKind = when {
                currentTab.isFollow -> 1
                currentTab is FeedTabSpec.Custom && currentTab.item == FeedTabItem.Circle -> 0
                else -> 2
            }
            App.settings.applyFeedTab(legacyKind)
        }
    }

    LaunchedEffect(feedTabs.size) {
        if (feedPager.currentPage >= feedTabs.size) {
            feedPager.scrollToPage(followIndex)
        }
    }

    // The top bar floats over the lists; the first row starts below it and later rows scroll under.
    // 栏钉在自己的密度上，页面按系统密度排版，所以这里要按像素换算回当前密度。
    // 三个页签共用一行高：主页的标题行已删，行内是「搜索 | 分段 | 发布」三件套。
    val topBarHeight = pinnedHeight(FeedTitleRowHeight)
    // 「我」页的滚动量：标题随上滑折起隐藏。
    var profileCollapse by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        // The bar floats over the page, so the line it ends at is reported from the bar itself and
        // read by anything that has to be placed clear of it.
        CompositionLocalProvider(LocalTopBarBounds provides topBarBounds) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(pageContainerColor())
            ) {
                // The bottom bar only switches on tap, so the page carries the transition itself:
                // both tabs stay composed through the same state holder and cross-fade.
                Crossfade(
                    targetState = tab,
                    animationSpec = tween(MainTabFadeMillis),
                    modifier = Modifier.fillMaxSize(),
                    label = "mainTabPage",
                ) { page ->
                    holder.SaveableStateProvider(page) {
                        when (page) {
                            0 -> TimelineScreen(
                                nav = nav,
                                pagerState = feedPager,
                                stores = feedStores,
                                emptyTexts = feedEmptyTexts,
                                followStore = followStore,
                                recommendStore = recommendStore,
                                topInset = topBarHeight,
                            )
                            1 -> if (guest) {
                                LocalMessagesPlaceholder(nav, topBarHeight)
                            } else {
                                MessagesScreen(nav, topBarHeight)
                            }
                            else -> if (guest) LocalHome(
                                nav = nav,
                                topInset = topBarHeight,
                                extraBottom = FloatingBarContentGap,
                            ) else ProfileContent(
                                nav = nav,
                                uid = null,
                                modifier = Modifier.fillMaxSize(),
                                topInset = topBarHeight,
                                extraBottom = FloatingBarContentGap,
                                onScrollOffset = { profileCollapse = it },
                            )
                        }
                    }
                }
            }
        }

        // 顶栏材质要靠 LocalEdgeToEdge 才会画：COUI 只在内容真的铺到状态栏下面时才肯采样，
        // 否则把内容第一行抹成一条平色横带（顶栏「糊成一团」）。页面 contentPadding 把 topInset
        // 放在列表自己身上，滚动时内容确实从顶栏底下过，这里直接开。
        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalEdgeToEdge provides true,
        ) {
            PinnedBarDensity {
                MainTopBar(
                    tab = tab,
                    nav = nav,
                    feedPager = feedPager,
                    feedTabs = feedTabs,
                    guest = guest,
                    filter = filter,
                    collapsePx = profileCollapse,
                    bounds = topBarBounds,
                    onPublish = {
                        if (guest) App.toast("纯本地模式不能发微博，登录后可用") else nav.push(Route.Publish())
                    },
                    onFilterSelected = { selected ->
                        filterIndex = selected.ordinal
                        App.settings.applyFeedFilter(selected.ordinal)
                        // 这几条流按本机标记拼，标记是在主页改的，切过来时重拉一次才看得到。
                        if (selected == FeedFilter.Special || selected == FeedFilter.Secret ||
                            selected == FeedFilter.Local
                        ) {
                            scope.launch { filterStores[selected]?.refresh() }
                        }
                        val targetPage = feedTabs.indexOfFirst { it.isFollow }.coerceAtLeast(0)
                        scope.launch { feedPager.animateScrollToPage(targetPage) }
                    },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                )
            }
        }

        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            PinnedBarDensity {
                FloatingBottomBar(
                    tab = tab,
                    onTab = nav::setTab,
                    onFeedTop = { App.requestFeedTop() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * The "原创微博" filter keeps only posts that are not reposts. Pages with nothing but reposts are
 * skipped so the first screen is not an empty state with more pages behind it.
 */
private suspend fun originalFeed(cursor: String?, force: Boolean): FeedPage {
    var next = cursor
    var first = true
    repeat(5) {
        val page = App.repo.friends(next, force && first)
        val originals = page.items.filter { it.retweeted == null }
        if (originals.isNotEmpty()) return FeedPage(originals, page.sinceId)
        if (page.items.isEmpty() || page.sinceId.isNullOrBlank()) return FeedPage(emptyList(), null)
        next = page.sinceId
        first = false
    }
    return FeedPage(emptyList(), next)
}

private val FloatingBarContentGap = 64.dp

/** Cross-fade length for a bottom-bar tab switch. */
private const val MainTabFadeMillis = 220

/**
 * The floating bar over the three tabs.
 *
 * 消息 / 我 两页顶栏是标题行，底下铺一层 AppBarBlurOverlay 渐变玻璃；主页的顶栏只有一行
 * 「搜索 | 分段 | 发布」，三件各自带玻璃材质、悬浮在内容上，不再铺整条模糊带——上滑时
 * 顶部那一大块固定模糊随之消失。材质只有一层：切页时用透明度淡入淡出，不做两层互相采样。
 */
@Composable
private fun MainTopBar(
    tab: Int,
    nav: NavState,
    feedPager: PagerState,
    feedTabs: List<FeedTabSpec>,
    guest: Boolean,
    filter: FeedFilter,
    /** 「我」页列表的滚动量；非「我」页忽略。 */
    collapsePx: Float = 0f,
    bounds: TopBarBounds,
    onPublish: () -> Unit,
    onFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pageBarHeight = statusBarTop + FeedTitleRowHeight
    // 「我」页标题随滚动折起：0 = 完整，1 = 完全隐藏（只影响标题，动作钮不动）。
    val titleHeightPx = with(LocalDensity.current) { FeedTitleRowHeight.toPx() }
    val titleCollapse = if (tab == 2) (collapsePx / titleHeightPx).coerceIn(0f, 1f) else 0f
    // 只有消息页保留渐变玻璃带；主页和「我」都是悬浮件、不要模糊带。缓动只改透明度。
    val materialAlpha = animateFloatAsState(
        targetValue = if (tab == 1) 1f else 0f,
        animationSpec = tween(MainTabFadeMillis),
        label = "mainBarMaterialAlpha",
    )
    Box(modifier.reportTopBarBottom(bounds)) {
        Box(Modifier.graphicsLayer { alpha = materialAlpha.value }) {
            AppBarBlurOverlay(
                modifier = Modifier.fillMaxWidth(),
                containerColor = pageContainerColor(),
                scrollFraction = { 1f },
            ) {
                Box(Modifier.fillMaxWidth().height(pageBarHeight))
            }
        }
        Crossfade(
            targetState = tab,
            animationSpec = tween(MainTabFadeMillis),
            label = "mainBarContent",
        ) { page ->
            BarContent(
                tab = page,
                nav = nav,
                feedPager = feedPager,
                feedTabs = feedTabs,
                guest = guest,
                filter = filter,
                titleCollapse = titleCollapse,
                onPublish = onPublish,
                onFilterSelected = onFilterSelected,
            )
        }
    }
}

@Composable
private fun BarContent(
    tab: Int,
    nav: NavState,
    feedPager: PagerState,
    feedTabs: List<FeedTabSpec>,
    guest: Boolean,
    filter: FeedFilter,
    titleCollapse: Float = 0f,
    onPublish: () -> Unit,
    onFilterSelected: (FeedFilter) -> Unit,
) {
    when (tab) {
        0 -> FeedHeader(
            feedPager = feedPager,
            feedTabs = feedTabs,
            filterMenu = !guest,
            filter = filter,
            onFilterSelected = onFilterSelected,
            onSearch = { nav.push(Route.Search) },
            onPublish = onPublish,
        )

        1 -> PageTitleBar(
            title = "消息",
            actions = {
                if (!guest) {
                    SearchBarActionButton(
                        text = "通知",
                        onClick = { nav.push(Route.Web("https://m.weibo.cn/message/", "通知")) },
                        modifier = Modifier.testTag("top_notice"),
                    )
                }
            },
        )

        else -> PageTitleBar(
            title = "我",
            collapsed = titleCollapse,
            actions = {
                HighlightedIconButton(
                    onClick = { nav.push(Route.Settings) },
                    modifier = Modifier.couiTopAppBarActionItem().testTag("top_settings"),
                ) {
                    Icon(COUIIcons.Settings, "设置", Modifier.size(24.dp))
                }
            },
        )
    }
}

/**
 * Single-row page header for the tabs that have no segment row. It matches the feed's
 * 「搜索 | 分段 | 发布」row in height, so 搜索, 通知 and 设置 buttons all land on the same
 * pixel across the three tabs.
 */
@Composable
private fun PageTitleBar(
    title: String,
    /** 标题折叠量（0 = 展开，1 = 隐藏）：随列表上滑把标题推走。 */
    collapsed: Float = 0f,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val titleHeightPx = with(LocalDensity.current) { FeedTitleRowHeight.toPx() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Box(Modifier.fillMaxWidth().height(FeedTitleRowHeight)) {
            Text(
                title,
                color = COUITheme.colorScheme.onSurface.copy(alpha = 1f - collapsed),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 6.dp)
                    .graphicsLayer { translationY = -collapsed * titleHeightPx },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TopAppBarDefaults.ActionIconPadding),
                horizontalArrangement = Arrangement.spacedBy(
                    TopAppBarDefaults.ActionMenuItemSpacing,
                    Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

private const val CirclePage = 0
private const val FollowPage = 1
private const val RecommendPage = 2

private val FeedTabLabels = listOf("好友圈", "关注", "推荐")
private val FeedTabTags = listOf("feed_tab_circle", "feed_tab_follow", "feed_tab_recommend")

/** Height of the top bar's single row on all three tabs. */
internal val FeedTitleRowHeight = 44.dp

/**
 * Single-row feed header, floating glass pieces with no blur band behind them:
 * 搜索圆钮在左、分段胶囊居中、发布圆钮在右，三件同一行高。
 */
@Composable
private fun FeedHeader(
    feedPager: PagerState,
    feedTabs: List<FeedTabSpec>,
    filterMenu: Boolean,
    filter: FeedFilter,
    onFilterSelected: (FeedFilter) -> Unit,
    onSearch: () -> Unit,
    onPublish: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var filterMenuExpanded by remember { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .height(FeedTitleRowHeight),
    ) {
        // 分段胶囊的宽度上限 = 行宽 - 两侧圆钮各自的一段边距；超宽时胶囊收缩而不是把圆钮挤走。
        val sideZone = TopBarButtonZone
        val pillMaxWidth = maxWidth - sideZone * 2
        HighlightedIconButton(
            onClick = onSearch,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = TopBarButtonMargin)
                .testTag("top_search"),
        ) {
            Icon(COUIIcons.Basic.Search, "搜索", Modifier.size(24.dp))
        }
        if (feedTabs.size > 1) {
            FeedTabs(
                pager = feedPager,
                feedTabs = feedTabs,
                filterMenu = filterMenu,
                filter = filter,
                filterMenuExpanded = filterMenuExpanded,
                onFilterMenuDismiss = { filterMenuExpanded = false },
                onFilterSelected = {
                    filterMenuExpanded = false
                    onFilterSelected(it)
                },
                onSelect = { page -> scope.launch { feedPager.animateScrollToPage(page) } },
                onFilterClick = { filterMenuExpanded = true },
                maxWidth = pillMaxWidth,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HighlightedIconButton(
            onClick = onPublish,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = TopBarButtonMargin)
                .testTag("top_publish"),
        ) {
            Icon(COUIIcons.Add, "发微博", Modifier.size(24.dp))
        }
    }
}

/** Top-bar side buttons: 40dp 圆钮 + 边距 + 与胶囊之间的间隙。 */
private val TopBarButtonMargin = 12.dp
private val TopBarButtonZone = TopBarButtonMargin + 40.dp + 8.dp

// COUI's segment control opts out of LocalIndication — COUIIndication paints a rectangular state
// layer over the whole tab bounds, which reads as a square shadow — and scales the pressed
// segment instead (COUIPressFeedbackHelper).
private val FeedTabPressSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

@Composable
private fun FeedTabs(
    pager: PagerState,
    feedTabs: List<FeedTabSpec>,
    filterMenu: Boolean,
    filter: FeedFilter,
    filterMenuExpanded: Boolean,
    onFilterMenuDismiss: () -> Unit,
    onFilterSelected: (FeedFilter) -> Unit,
    onSelect: (Int) -> Unit,
    onFilterClick: () -> Unit,
    /** 分段胶囊可用的最大宽度（行宽减去两侧圆钮的占位），超出时格子收窄。 */
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = COUITheme.colorScheme
    // 玻璃胶囊 + 半透明白指示块，与底栏同一套液态玻璃材质。
    val dark = scheme.background.luminance() < 0.5f
    val indicatorColor = if (dark) Color(0x33FFFFFF) else Color(0xB2FFFFFF)
    val labels = feedTabs.map { it.label }
    val interactions = remember(feedTabs.size) { List(feedTabs.size) { MutableInteractionSource() } }
    val pressed = interactions.map { it.collectIsPressedAsState() }
    val indicatorScale by animateFloatAsState(
        targetValue = if (pressed.getOrNull(pager.currentPage)?.value == true) 0.96f else 1f,
        animationSpec = FeedTabPressSpring,
        label = "feedTabIndicatorScale",
    )
    // 三格同宽，按「4 个字 + 箭头」预留，字数是 2/3/4 时胶囊和位置都不动。
    // 用 sp 折算，系统字体放大时格子跟着变宽，不会被截断；两侧圆钮占位不够时格子才收窄。
    val density = LocalDensity.current
    val preferredSegment = with(density) { (4 * 13).sp.toDp() } + 2.dp + 10.dp + 18.dp
    val segmentWidth = preferredSegment.coerceAtMost((maxWidth - 8.dp) / feedTabs.size)
    val containerWidth = segmentWidth * feedTabs.size + 8.dp
    val segmentWidthPx = with(density) { segmentWidth.toPx() }
    val restHeightPx = with(density) { 32.dp.toPx() }
    val padPx = with(density) { 4.dp.toPx() }

    fun centerFor(index: Int): Float = padPx + index * segmentWidthPx + segmentWidthPx / 2f
    val firstCenter = centerFor(0)
    val lastCenter = centerFor(max(0, feedTabs.size - 1))

    val initialPage = pager.currentPage.coerceIn(0, max(0, feedTabs.size - 1))
    val motion = remember {
        CouiSegmentIndicatorMotion(
            center = centerFor(initialPage),
            width = segmentWidthPx,
            height = restHeightPx,
        )
    }
    LaunchedEffect(motion) { motion.animate() }

    var lastSettledPage by remember { mutableIntStateOf(initialPage) }
    LaunchedEffect(pager.currentPage, segmentWidthPx) {
        val target = pager.currentPage.coerceIn(0, max(0, feedTabs.size - 1))
        if (target != lastSettledPage) {
            lastSettledPage = target
            motion.select(
                targetCenter = centerFor(target),
                targetWidth = segmentWidthPx,
                restHeight = restHeightPx,
                firstCenter = firstCenter,
                lastCenter = lastCenter,
                maxExtraWidth = with(density) { 16.dp.toPx() },
            )
        }
    }

    LaunchedEffect(pager.currentPageOffsetFraction, pager.isScrollInProgress) {
        if (pager.isScrollInProgress && !motion.isRunning) {
            val pageFloat = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(
                0f,
                (feedTabs.size - 1).coerceAtLeast(0).toFloat()
            )
            val center = padPx + pageFloat * segmentWidthPx + segmentWidthPx / 2f
            val offsetAbs = abs(pager.currentPageOffsetFraction)
            val stretchFactor = sin(offsetAbs * PI.toFloat()).coerceIn(0f, 1f)
            val dynHeight = restHeightPx * (1f - 0.1f * stretchFactor)
            val dynWidth = segmentWidthPx + with(density) { 14.dp.toPx() } * stretchFactor
            motion.updateDrag(center, dynWidth, dynHeight)
        }
    }

    val handleSelect: (Int) -> Unit = { index ->
        lastSettledPage = index
        motion.select(
            targetCenter = centerFor(index),
            targetWidth = segmentWidthPx,
            restHeight = restHeightPx,
            firstCenter = firstCenter,
            lastCenter = lastCenter,
            maxExtraWidth = with(density) { 16.dp.toPx() },
        )
        onSelect(index)
    }

    val effectivePage = if (segmentWidthPx > 0f) {
        ((motion.centerX - padPx - segmentWidthPx / 2f) / segmentWidthPx).coerceIn(
            0f,
            (feedTabs.size - 1).coerceAtLeast(0).toFloat()
        )
    } else {
        0f
    }

    // couiGlassSurface 丢弃它前面的 modifier，align 之类的位置信息只能排在它后面。
    // 材质用液态玻璃，形状用上游的连续曲率胶囊（CouiSegmentCapsuleShape），动效沿用 CouiSegmentIndicatorMotion。
    BoxWithConstraints(
        Modifier
            .liquidGlass(CouiSegmentCapsuleShape, glassPlate(dark), glassPlateAlpha(dark))
            .then(modifier)
            // 40dp 胶囊，与两侧圆钮同高；clip 裁住指示块的圆角。
            .requiredHeight(40.dp)
            .width(containerWidth)
            .clip(CouiSegmentCapsuleShape),
    ) {
        val drawW = with(density) { (motion.width * indicatorScale).toDp() }
        val drawH = with(density) { (motion.height * indicatorScale).toDp() }
        val drawCenterXDp = with(density) { motion.centerX.toDp() }
        val drawLeftDp = drawCenterXDp - drawW / 2f
        val drawTopDp = (40.dp - drawH) / 2f

        Box(
            Modifier
                .offset(
                    x = drawLeftDp,
                    y = drawTopDp,
                )
                .size(width = drawW, height = drawH)
                .clip(CouiSegmentCapsuleShape)
                .background(indicatorColor),
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            feedTabs.forEachIndexed { index, spec ->
                val menu = filterMenu && spec.isFollow
                FeedTabSegment(
                    label = spec.label,
                    index = index,
                    tag = spec.tag,
                    page = effectivePage,
                    interactionSource = interactions[index],
                    pressed = pressed[index].value,
                    modifier = Modifier.weight(1f),
                    showArrow = menu,
                    onClick = {
                        // 两段式：没选中先选中，选中后再点才弹出筛选分类；切换走 handleSelect 以触发压扁动效。
                        if (index == pager.currentPage) {
                            if (menu) onFilterClick()
                        } else {
                            handleSelect(index)
                        }
                    },
                ) {
                    if (menu) {
                        OverlayListPopup(
                            show = filterMenuExpanded,
                            popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                            onDismissRequest = onFilterMenuDismiss,
                        ) {
                            val dismiss = LocalDismissState.current
                            ListPopupColumn {
                                FeedFilter.entries.forEachIndexed { filterIndex, item ->
                                    DropdownImpl(
                                        item = DropdownItem(text = item.label),
                                        optionSize = FeedFilter.entries.size,
                                        isSelected = item == filter,
                                        index = filterIndex,
                                        // One divider after "全部"; the filters below stay one group.
                                        groupKey = if (filterIndex == 0) "all" else "filters",
                                        onSelectedIndexChange = {
                                            onFilterSelected(FeedFilter.entries[it])
                                            dismiss?.invoke()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTabSegment(
    label: String,
    index: Int,
    tag: String,
    page: Float,
    interactionSource: MutableInteractionSource,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    showArrow: Boolean,
    onClick: () -> Unit,
    popup: @Composable () -> Unit = {},
) {
    val scheme = COUITheme.colorScheme
    val amount = (1f - abs(page - index)).coerceIn(0f, 1f)
    val labelScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = FeedTabPressSpring,
        label = "feedTabLabelScale",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = labelScale
                scaleY = labelScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // 排版按 Apple §15：小字用轻微正字距，层级靠字重（选中 Medium / 未选中 Regular）。
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (amount >= 0.5f) FontWeight.Medium else FontWeight.Normal,
                letterSpacing = 0.15.sp,
                color = lerp(scheme.onSurfaceSecondary, scheme.onSurface, amount),
                maxLines = 1,
                softWrap = false,
            )
            if (showArrow) {
                Icon(
                    COUIIcons.Basic.ExpandableIndicator,
                    contentDescription = null,
                    tint = lerp(scheme.onSurfaceSecondary, scheme.onSurface, amount),
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        popup()
    }
}

private val GlassBarHeight = 52.dp
private val GlassBarRadius = 26.dp
private val GlassBarBottomGap = 18.dp

/**
 * 悬浮玻璃底栏：三个页签一枚液态玻璃胶囊。刷新入口已移除（下拉刷新保留），
 * 发布「+」移到顶栏右侧。
 */
@Composable
private fun FloatingBottomBar(
    tab: Int,
    onTab: (Int) -> Unit,
    onFeedTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 双击「微博」= 回到顶部。用时间戳判定而不是 detectTapGestures：底栏 item 内部的
    // combinedClickable 会先消费 up 事件，外层再加 pointerInput 收不到点击。
    // 单击照常切 tab，不延迟；320ms 内的第二下追加一次回到顶部。
    var lastFeedTapAt by remember { mutableLongStateOf(0L) }
    val onFeedTap: () -> Unit = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastFeedTapAt <= 320L) {
            lastFeedTapAt = 0L
            onFeedTop()
        } else {
            lastFeedTapAt = now
        }
        onTab(0)
    }
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // 左右边缘对齐三图网格的左右边距（页面 12 + 卡片内衬 14 = 26dp）；
        // 大表面用更强的模糊读作更厚的材质（Apple Design §12）。
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = GlassBarBottomGap),
        ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // 选中指示块：玻璃胶囊随切换滑动（CouiSegmentIndicatorMotion 弹簧 + 光效描边）。
            val density = LocalDensity.current
            val barH = constraints.maxHeight
            val itemW = constraints.maxWidth / 3f
            val indicatorW = itemW - with(density) { 12.dp.toPx() }
            val indicatorH = with(density) { 40.dp.toPx() }
            val centerForIndex = { i: Int -> itemW / 2f + i * itemW }
            val motion = remember {
                CouiSegmentIndicatorMotion(
                    center = centerForIndex(when (tab) { 1 -> 0; 2 -> 2; else -> 1 }),
                    width = indicatorW,
                    height = indicatorH,
                )
            }
            LaunchedEffect(motion) { motion.animate() }
            LaunchedEffect(tab, itemW) {
                val index = when (tab) { 1 -> 0; 2 -> 2; else -> 1 }
                motion.select(
                    targetCenter = centerForIndex(index),
                    targetWidth = indicatorW,
                    restHeight = indicatorH,
                    firstCenter = centerForIndex(0),
                    lastCenter = centerForIndex(2),
                    maxExtraWidth = with(density) { 8.dp.toPx() },
                )
            }
            Box(
                Modifier
                    .liquidGlass(
                        RoundedCornerShape(GlassBarRadius),
                        glassPlate(dark),
                        glassPlateAlpha(dark),
                        blurRadius = 52f,
                    )
                    .fillMaxWidth()
                    .height(GlassBarHeight),
            ) {
                // 指示块：半透明胶囊 + 受光描边（光效），随弹簧滑动。
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (motion.centerX - motion.width / 2f).roundToInt(),
                                ((barH - motion.height) / 2f).roundToInt(),
                            )
                        }
                        .size(
                            width = with(density) { motion.width.toDp() },
                            height = with(density) { motion.height.toDp() },
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (dark) Color(0x33FFFFFF) else Color(0xB2FFFFFF))
                        .drawBehind {
                            val w = 1.dp.toPx()
                            drawRoundRect(
                                color = Color.White.copy(alpha = if (dark) 0.22f else 0.65f),
                                cornerRadius = CornerRadius(20.dp.toPx() - w / 2f),
                                style = Stroke(w),
                                topLeft = Offset(w / 2f, w / 2f),
                                size = Size(size.width - w, size.height - w),
                            )
                            // 顶部受光的高光带（光效）。
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (dark) 0.16f else 0.38f),
                                        Color.White.copy(alpha = 0f),
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.55f,
                                ),
                            )
                        },
                )
                Row(
                    Modifier.fillMaxWidth().height(GlassBarHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                // 位置按用户要求交换：左「消息」、中「微博」、右「我」。tab 值仍沿用
                // 0=微博 / 1=消息 / 2=我，只换视觉顺序，避免影响 nav.mainTab 的持久化语义。
                GlassBarItem(
                    selected = tab == 1,
                    onClick = { onTab(1) },
                    icon = CoboIcons.MessagesOutline,
                    selectedIcon = CoboIcons.MessagesFilled,
                    label = "消息",
                    modifier = Modifier.weight(1f).testTag("bottom_tab_messages"),
                )
                GlassBarItem(
                    selected = tab == 0,
                    onClick = onFeedTap,
                    icon = CoboIcons.FeedOutline,
                    selectedIcon = CoboIcons.FeedFilled,
                    label = "微博",
                    modifier = Modifier.weight(1f).testTag("bottom_tab_feed"),
                )
                GlassBarItem(
                    selected = tab == 2,
                    onClick = { onTab(2) },
                    icon = CoboIcons.ProfileOutline,
                    selectedIcon = CoboIcons.ProfileFilled,
                    label = "我",
                    modifier = Modifier.weight(1f).testTag("bottom_tab_profile"),
                )
                }
            }
        }
        }
    }
}

/** 底栏页签：图标 + 小字标签，选中面性图标、未选中线性图标（COUI 17 规范）。 */
@Composable
private fun GlassBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) {
        COUITheme.colorScheme.onSurface
    } else {
        COUITheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = FeedTabPressSpring,
        label = "glassBarItemScale",
    )
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = tween(180),
            label = "glassBarIcon",
        ) { sel ->
            Icon(
                if (sel) selectedIcon else icon,
                label,
                Modifier.size(24.dp),
                tint = tint,
            )
        }
        // 排版按 Apple §15：小字正字距、层级靠字重——选中 Medium、未选中 Regular。
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.2.sp,
            color = tint,
            maxLines = 1,
        )
    }
}
