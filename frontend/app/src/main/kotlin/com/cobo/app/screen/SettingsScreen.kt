package com.cobo.app.screen

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import com.cobo.app.App
import com.cobo.app.data.Account
import com.cobo.app.data.FeedTabItem
import com.cobo.app.data.Updater
import com.cobo.app.ui.NavState
import com.cobo.app.ui.PinnedBarDensity
import com.cobo.app.ui.pageContainerColor
import com.cobo.app.ui.pinnedHeight
import com.cobo.app.ui.Route
import com.cobo.app.ui.components.BackIconButton
import com.cobo.app.ui.components.ConfirmDialog
import com.cobo.app.ui.components.PreferenceDivider
import com.cobo.app.ui.components.navBottomPadding
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val version = remember { Updater.currentVersion(context) }
    var darkMode by remember { mutableStateOf(App.settings.darkMode) }
    var fontScale by remember { mutableStateOf(App.settings.fontScale) }
    var autoCheckUpdate by remember { mutableStateOf(App.settings.autoCheckUpdate) }
    var acceptPrerelease by remember { mutableStateOf(App.settings.acceptPrerelease) }
    var inAppUpdate by remember { mutableStateOf(App.settings.inAppUpdate) }
    var predictiveBack by remember { mutableStateOf(App.settings.predictiveBack) }
    var glassAlpha by remember { mutableStateOf(App.settings.glassAlpha) }
    var message by remember { mutableStateOf("") }
    var about by remember { mutableStateOf(false) }
    var logout by remember { mutableStateOf(false) }
    var localFollowUid by remember { mutableStateOf(App.settings.localFollowUid) }
    val guest = App.repo.guest
    var pickLocalFollow by remember { mutableStateOf(false) }
    var feedLeftTab by remember { mutableStateOf(App.settings.feedLeftTab) }
    var feedCenterTab by remember { mutableStateOf(App.settings.feedCenterTab) }
    var feedRightTab by remember { mutableStateOf(App.settings.feedRightTab) }
    var pickFeedLeft by remember { mutableStateOf(false) }
    var pickFeedCenter by remember { mutableStateOf(false) }
    var pickFeedRight by remember { mutableStateOf(false) }

    // 只在选中的 uid 变化时读一次账号表，切到别的账号后摘要跟着换。
    val accounts = remember(localFollowUid) { App.store.accounts() }
    val localFollowAccount = accounts.firstOrNull { it.uid == localFollowUid }
    val localFollowSummary = if (localFollowAccount == null) {
        "跟随当前账号"
    } else {
        "用 ${localFollowAccount.name.ifBlank { "UID ${localFollowAccount.uid}" }} 的身份取数"
    }

    val pageColor = pageContainerColor()
    // 顶栏玻璃要有东西可采样：把列表录进 backdrop，顶栏浮在上面做渐变模糊。
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(pageColor)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 列表铺到屏幕最底，滚到末页时卡片从系统导航条下方穿过（沉浸），
            // 末项靠 contentPadding 抬到导航条之上，不会被挡住。
            contentPadding = PaddingValues(
                start = 12.dp,
                top = topBarHeight + 10.dp,
                end = 12.dp,
                bottom = 24.dp + navBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionTitle("显示与交互") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    AppearanceRow(
                        selected = darkMode,
                        onSelect = { mode ->
                            darkMode = mode
                            App.settings.applyDarkMode(mode)
                        },
                    )
                    PreferenceDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_font")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(96.dp)) {
                            Text(
                                "字号",
                                fontSize = 15.sp,
                                color = COUITheme.colorScheme.onSurface,
                            )
                            Text(
                                "当前 ${(fontScale * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = COUITheme.colorScheme.onSurfaceSecondary,
                            )
                        }
                        Slider(
                            value = fontScale * 100f,
                            onValueChange = {
                                val v = it / 100f
                                fontScale = v
                                App.settings.applyFontScale(v)
                            },
                            valueRange = 80f..140f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                    }
                    PreferenceDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_glass_alpha")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(96.dp)) {
                            Text(
                                "玻璃透明度",
                                fontSize = 15.sp,
                                color = COUITheme.colorScheme.onSurface,
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "轻透磨砂",
                                    fontSize = 11.sp,
                                    color = COUITheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "柔光玻璃",
                                    fontSize = 11.sp,
                                    color = COUITheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                )
                            }
                            Slider(
                                value = glassAlpha.toFloat(),
                                onValueChange = {
                                    glassAlpha = it.toInt()
                                    App.settings.applyGlassAlpha(it.toInt())
                                },
                                valueRange = 0f..100f,
                            )
                        }
                    }
                    PreferenceDivider()
                    SwitchPreference(
                        checked = predictiveBack,
                        onCheckedChange = {
                            predictiveBack = it
                            App.settings.applyPredictiveBack(it)
                        },
                        title = "预测性返回手势",
                        modifier = Modifier.testTag("settings_predictive_back"),
                        cardListPosition = CardListPosition.Tail,
                    )
                }
            }

            item { SectionTitle("微博") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    ArrowPreference(
                        title = "微博左侧栏",
                        modifier = Modifier.testTag("settings_feed_left_tab"),
                        summary = feedLeftTab.label,
                        cardListPosition = CardListPosition.Head,
                        onClick = { pickFeedLeft = true },
                    )
                    PreferenceDivider()
                    ArrowPreference(
                        title = "微博中间栏",
                        modifier = Modifier.testTag("settings_feed_center_tab"),
                        summary = feedCenterTab.label,
                        cardListPosition = CardListPosition.Middle,
                        onClick = { pickFeedCenter = true },
                    )
                    PreferenceDivider()
                    ArrowPreference(
                        title = "微博右侧栏",
                        modifier = Modifier.testTag("settings_feed_right_tab"),
                        summary = feedRightTab.label,
                        cardListPosition = CardListPosition.Tail,
                        onClick = { pickFeedRight = true },
                    )
                }
            }

            item { SectionTitle("账号与内容") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    ArrowPreference(
                        title = "账号管理",
                        modifier = Modifier.testTag("settings_accounts"),
                        cardListPosition = CardListPosition.Head,
                        onClick = { nav.push(Route.Accounts) },
                    )
                    // 纯本地模式一律用访客身份取数，没有账号可选。
                    if (!guest) {
                        PreferenceDivider()
                        ArrowPreference(
                            title = "本地关注使用账号",
                            modifier = Modifier.testTag("settings_local_follow"),
                            summary = localFollowSummary,
                            onClick = { pickLocalFollow = true },
                        )
                    }
                    PreferenceDivider()
                    ArrowPreference(
                        title = "草稿箱",
                        modifier = Modifier.testTag("settings_drafts"),
                        summary = "${App.store.drafts().size} 条草稿",
                        onClick = { nav.push(Route.Drafts) },
                    )
                    PreferenceDivider()
                    ArrowPreference(
                        title = "浏览历史",
                        modifier = Modifier.testTag("settings_history"),
                        summary = "${App.store.history().size} 条记录",
                        onClick = { nav.push(Route.History) },
                    )
                    // 收藏和附近挂在账号上，访客身份拿不到，纯本地模式不给入口。
                    if (!guest) {
                        PreferenceDivider()
                        ArrowPreference(
                            title = "收藏",
                            modifier = Modifier.testTag("settings_favorites"),
                            onClick = { nav.push(Route.Container("230267", "收藏")) },
                        )
                    }
                    PreferenceDivider()
                    ArrowPreference(
                        title = "话题 / 超话",
                        modifier = Modifier.testTag("settings_topic"),
                        cardListPosition = if (guest) CardListPosition.Tail else CardListPosition.None,
                        onClick = { nav.push(Route.Topic()) },
                    )
                    if (!guest) {
                        PreferenceDivider()
                        ArrowPreference(
                            title = "附近",
                            modifier = Modifier.testTag("settings_nearby"),
                            cardListPosition = CardListPosition.Tail,
                            onClick = { nav.push(Route.Nearby) },
                        )
                    }
                }
            }

            item { SectionTitle("更新") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    ArrowPreference(
                        title = "检查更新",
                        modifier = Modifier.testTag("settings_update"),
                        summary = if (App.checkingUpdate) "正在检查…" else "当前版本 $version",
                        cardListPosition = CardListPosition.Head,
                        onClick = { scope.launch { App.checkUpdate(manual = true) } },
                    )
                    PreferenceDivider()
                    ArrowPreference(
                        title = "更新日志",
                        modifier = Modifier.testTag("settings_changelog"),
                        cardListPosition = CardListPosition.Middle,
                        onClick = { nav.push(Route.Changelog) },
                    )
                    PreferenceDivider()
                    SwitchPreference(
                        checked = autoCheckUpdate,
                        onCheckedChange = {
                            autoCheckUpdate = it
                            App.settings.applyAutoCheckUpdate(it)
                        },
                        title = "自动检查更新",
                        modifier = Modifier.testTag("settings_auto_update"),
                        cardListPosition = CardListPosition.Middle,
                    )
                    PreferenceDivider()
                    SwitchPreference(
                        checked = acceptPrerelease,
                        onCheckedChange = {
                            acceptPrerelease = it
                            App.settings.applyAcceptPrerelease(it)
                        },
                        title = "接收预发布版本",
                        modifier = Modifier.testTag("settings_prerelease"),
                        cardListPosition = CardListPosition.Middle,
                    )
                    PreferenceDivider()
                    SwitchPreference(
                        checked = inAppUpdate,
                        onCheckedChange = {
                            inAppUpdate = it
                            App.settings.applyInAppUpdate(it)
                        },
                        title = "应用内更新",
                        modifier = Modifier.testTag("settings_in_app_update"),
                        cardListPosition = CardListPosition.Tail,
                    )
                }
            }

            item { SectionTitle("其他") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    ArrowPreference(
                        title = "清理缓存",
                        modifier = Modifier.testTag("settings_clear_cache"),
                        cardListPosition = CardListPosition.Head,
                        onClick = {
                            runCatching {
                                val loader = SingletonImageLoader.get(context)
                                loader.memoryCache?.clear()
                                loader.diskCache?.clear()
                            }
                            App.cache.clear()
                            message = "已清理图片与内容缓存"
                        },
                    )
                    PreferenceDivider()
                    ArrowPreference(
                        title = "关于",
                        modifier = Modifier.testTag("settings_about"),
                        summary = "Heibo · 第三方微博客户端",
                        cardListPosition = CardListPosition.Tail,
                        onClick = { about = true },
                    )
                }
            }

            // 退出是破坏性操作，单独一张卡，不和其他条目挤在一起。
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    if (guest) {
                        ArrowPreference(
                            title = "退出纯本地模式",
                            modifier = Modifier.testTag("settings_leave_local"),
                            summary = "回到登录页，本地关注名单保留",
                            cardListPosition = CardListPosition.Full,
                            onClick = {
                                App.settings.applyLocalMode(false)
                                nav.reset(Route.Login())
                            },
                        )
                    } else {
                        ArrowPreference(
                            title = "退出登录",
                            modifier = Modifier.testTag("settings_logout"),
                            summary = "清除本地会话",
                            cardListPosition = CardListPosition.Full,
                            onClick = { logout = true },
                        )
                    }
                }
            }

            if (message.isNotBlank()) {
                item {
                    Text(
                        message,
                        fontSize = 12.sp,
                        color = COUITheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            item {
                Text(
                    "Heibo · 基于 m.weibo.cn Web API 的第三方微博客户端。\n仅用于个人学习与研究。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = COUITheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                )
            }
        }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            // 内容铺到状态栏下面，顶栏玻璃才有东西可采样（COUI 17 规范要求 edgeToEdge）。
            LocalEdgeToEdge provides true,
        ) {
            PinnedBarDensity {
                SmallTopAppBar(
                    title = "设置",
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = pageColor,
                    showDivider = false,
                    gradientGlass = true,
                    navigationIcon = { BackIconButton { nav.pop() } },
                )
            }
        }
    }

    if (about) {
        ConfirmDialog(
            title = "关于 Heibo",
            message = "Heibo v$version\n\n基于 m.weibo.cn Web API 的第三方微博客户端，基于 COUI（ColorOS 17 设计语言）重做整套界面与交互。\n仅用于个人学习与研究。",
            confirmText = "知道了",
            onDismiss = { about = false },
            onConfirm = { about = false },
        )
    }

    if (logout) {
        ConfirmDialog(
            title = "退出登录",
            message = "会清除本地保存的 Cookie，下次需要重新登录。",
            confirmText = "退出",
            onDismiss = { logout = false },
            onConfirm = {
                logout = false
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                App.repo.logout()
                nav.reset(Route.Login())
            },
        )
    }

    if (pickLocalFollow) {
        LocalFollowPicker(
            accounts = accounts,
            selectedUid = localFollowUid,
            onDismiss = { pickLocalFollow = false },
            onPick = { uid ->
                App.settings.applyLocalFollowUid(uid)
                localFollowUid = uid
                pickLocalFollow = false
            },
        )
    }

    if (pickFeedLeft) {
        FeedTabPicker(
            title = "微博左侧栏内容",
            selected = feedLeftTab,
            onDismiss = { pickFeedLeft = false },
            onPick = { item ->
                App.settings.applyFeedLeftTab(item)
                feedLeftTab = item
                pickFeedLeft = false
            },
        )
    }

    if (pickFeedCenter) {
        FeedTabPicker(
            title = "微博中间栏内容",
            selected = feedCenterTab,
            options = FeedTabItem.entries.filter { it != FeedTabItem.None },
            onDismiss = { pickFeedCenter = false },
            onPick = { item ->
                App.settings.applyFeedCenterTab(item)
                feedCenterTab = item
                pickFeedCenter = false
            },
        )
    }

    if (pickFeedRight) {
        FeedTabPicker(
            title = "微博右侧栏内容",
            selected = feedRightTab,
            onDismiss = { pickFeedRight = false },
            onPick = { item ->
                App.settings.applyFeedRightTab(item)
                feedRightTab = item
                pickFeedRight = false
            },
        )
    }

}

@Composable
private fun LocalFollowPicker(
    accounts: List<Account>,
    selectedUid: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    OverlayDialog(
        show = show,
        title = "本地关注使用账号",
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                val uid = result
                if (uid != null) onPick(uid) else onDismiss()
            }
        },
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                propagateMinConstraints = true,
            ) {
                ListPopupColumn {
                    val optionCount = accounts.size + 1
                    DropdownImpl(
                        item = DropdownItem(text = "跟随当前账号"),
                        optionSize = optionCount,
                        isSelected = selectedUid.isBlank(),
                        index = 0,
                        modifier = Modifier.testTag("local_follow_follow_current"),
                        groupKey = "local-follow-accounts",
                        onSelectedIndexChange = {
                            result = ""
                            show = false
                        },
                    )
                    accounts.forEachIndexed { i, account ->
                        DropdownImpl(
                            item = DropdownItem(
                                text = account.name.ifBlank { "UID ${account.uid}" },
                                summary = if (account.name.isBlank()) null else "UID ${account.uid}",
                            ),
                            optionSize = optionCount,
                            isSelected = account.uid == selectedUid,
                            index = i + 1,
                            modifier = Modifier.testTag("local_follow_option_${account.uid}"),
                            groupKey = "local-follow-accounts",
                            onSelectedIndexChange = {
                                result = accounts[it - 1].uid
                                show = false
                            },
                        )
                    }
                }
            }
            DialogButtonBar(
                negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
                positive = null,
            )
        }
    }
}

@Composable
private fun FeedTabPicker(
    title: String,
    selected: FeedTabItem,
    options: List<FeedTabItem> = FeedTabItem.entries,
    onDismiss: () -> Unit,
    onPick: (FeedTabItem) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FeedTabItem?>(null) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    OverlayDialog(
        show = show,
        title = title,
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                val picked = result
                if (picked != null) onPick(picked) else onDismiss()
            }
        },
    ) {
        Column {
            // 和三点菜单同一套：ListPopupColumn 自己铺胶囊高亮和上下拖选的手势，
            // DropdownImpl 负责选中态的勾和蓝字。propagateMinConstraints 把对话框的宽度
            // 传给列，否则列会缩到弹层的最小宽度（约 168dp）。
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                propagateMinConstraints = true,
            ) {
                ListPopupColumn {
                    options.forEachIndexed { index, opt ->
                        DropdownImpl(
                            item = DropdownItem(text = opt.label),
                            optionSize = options.size,
                            isSelected = opt == selected,
                            index = index,
                            modifier = Modifier.testTag("feed_tab_option_${opt.key}"),
                            groupKey = "feed-tabs",
                            onSelectedIndexChange = {
                                result = options[it]
                                show = false
                            },
                        )
                    }
                }
            }
            DialogButtonBar(
                negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
                positive = null,
            )
        }
    }
}

/**
 * 外观模式：标题在左，右侧直接放「跟随系统 / 浅色模式 / 深色模式」三段按钮，
 * 点击即切换，不弹选择器。
 */
@Composable
private fun AppearanceRow(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val scheme = COUITheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dark")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "外观模式",
            fontSize = 15.sp,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) Color(0xFF353535) else Color(0xFFE2E3E4))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("跟随系统", "浅色模式", "深色模式").forEachIndexed { index, label ->
                val on = index == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            // 选中态用强调蓝，和同组滑杆 / 开关的强调色统一。
                            if (on) scheme.primary else Color.Transparent,
                        )
                        .clickable {
                            onSelect(index)
                        }
                        .testTag("appearance_option_$index")
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = if (on) scheme.onPrimary else scheme.onSurfaceSecondary,
                    )
                }
            }
        }
    }
}

/** 卡片组上面的小标题。左边对齐到卡片里的文字，而不是卡片边缘。 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = COUITheme.colorScheme.onSurfaceSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 2.dp),
    )
}
