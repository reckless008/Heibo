package com.cobo.app.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobo.app.App
import com.cobo.app.data.Comment
import com.cobo.app.data.CommentSort
import com.cobo.app.data.HistoryItem
import com.cobo.app.data.Membership
import com.cobo.app.data.Status
import com.cobo.app.data.formatWeiboTime
import com.cobo.app.ui.NavState
import com.cobo.app.ui.PinnedBarDensity
import com.cobo.app.ui.pinnedHeight
import com.cobo.app.ui.pageContainerColor
import com.cobo.app.ui.Route
import com.cobo.app.ui.TopBarBounds
import com.cobo.app.ui.attachToRow
import com.cobo.app.ui.reportTopBarBottom
import com.cobo.app.ui.CoboIcons
import com.cobo.app.data.User
import com.cobo.app.ui.components.ActionIcon
import com.cobo.app.ui.components.GlassActionPill
import com.cobo.app.ui.components.Avatar
import com.cobo.app.ui.components.BackIconButton
import com.cobo.app.ui.components.ErrorState
import com.cobo.app.ui.components.InputDialog
import com.cobo.app.ui.components.LoadingState
import com.cobo.app.ui.components.MemberBadge
import com.cobo.app.ui.components.ShareSheet
import com.cobo.app.ui.components.StatusCard
import com.cobo.app.ui.components.WeiboText
import com.cobo.app.ui.components.fmtCount
import com.cobo.app.ui.components.navBottomPadding
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Edit
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(nav: NavState, id: String, mid: String, readOnly: Boolean = false) {
    val scope = rememberCoroutineScope()
    var status by remember(id) { mutableStateOf<Status?>(null) }
    var detailError by remember(id) { mutableStateOf<String?>(null) }
    val comments = remember(id) { mutableStateListOf<Comment>() }
    var maxId by remember(id) { mutableStateOf<String?>(null) }
    var maxIdType by remember(id) { mutableStateOf(0) }
    var sort by remember(id) { mutableStateOf(CommentSort.Hot) }
    var nextCommentPage by remember(id) { mutableStateOf(1) }
    var loadingComments by remember(id) { mutableStateOf(false) }
    var commentsEnd by remember(id) { mutableStateOf(false) }
    var commentError by remember(id) { mutableStateOf<String?>(null) }
    var switchingSort by remember(id) { mutableStateOf(false) }
    var showSwitchProgress by remember(id) { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var repostDialog by remember { mutableStateOf(false) }
    var commentDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // The bar floats over this list too, so the quoted post inside the card below has to be able to
    // read where the bar ends when a page returns into it.
    val topBarBounds = remember { TopBarBounds() }
    val backdrop = rememberLayerBackdrop()

    // A link only carries the numeric status id; the mblogid comes with the loaded status.
    fun effectiveMid(): String = status?.mid?.takeIf { it.isNotBlank() } ?: mid

    suspend fun loadComments(reset: Boolean, force: Boolean = false) {
        if (loadingComments) return
        if (!reset && switchingSort) return
        loadingComments = true
        commentError = null
        val requestedSort = sort
        try {
            val page = App.repo.comments(
                id = id,
                mid = effectiveMid(),
                sort = requestedSort,
                maxId = if (reset) null else maxId,
                maxIdType = if (reset) 0 else maxIdType,
                page = if (reset) 1 else nextCommentPage,
                force = force,
            )
            // Another order took over while this page was on the wire; its rows are not ours.
            if (sort != requestedSort) return
            if (reset) comments.clear()
            val known = comments.mapTo(HashSet()) { it.id }
            page.items.forEach { if (known.add(it.id)) comments.add(it) }
            maxId = page.maxId
            maxIdType = page.maxIdType
            nextCommentPage = page.nextPage
            if (page.items.isEmpty() ||
                (sort == CommentSort.Time && page.nextPage == 0)
            ) {
                commentsEnd = true
            }
        } catch (t: Throwable) {
            commentError = t.message ?: "评论加载失败"
        } finally {
            loadingComments = false
        }
    }

    fun switchSort(next: CommentSort) {
        if (sort == next) return
        val previous = sort
        val previousMaxId = maxId
        val previousMaxIdType = maxIdType
        val previousPage = nextCommentPage
        val previousEnd = commentsEnd
        sort = next
        switchingSort = true
        maxId = null
        maxIdType = 0
        nextCommentPage = 1
        commentsEnd = false
        commentError = null
        // Keep the current rows on screen until the other order arrives; the header shows a
        // slim progress line only if the wait is long enough to notice.
        scope.launch {
            // Let a page already on the wire for the old order land and be dropped first.
            while (loadingComments) delay(30)
            try {
                loadComments(true, force = true)
            } finally {
                switchingSort = false
            }
            // The old rows are still on screen, so a failed switch must also put the control back
            // instead of labelling hot comments as the newest ones.
            val failure = commentError
            if (failure != null && sort == next) {
                sort = previous
                maxId = previousMaxId
                maxIdType = previousMaxIdType
                nextCommentPage = previousPage
                commentsEnd = previousEnd
                commentError = null
                App.toast(failure)
            }
        }
    }

    LaunchedEffect(switchingSort) {
        if (!switchingSort) {
            showSwitchProgress = false
        } else {
            delay(180)
            showSwitchProgress = true
        }
    }

    LaunchedEffect(id) {
        detailError = null
        try {
            status = App.repo.detail(id, force = false)
        } catch (t: Throwable) {
            detailError = t.message ?: "正文加载失败"
        }
        status?.let { s ->
            App.store.addHistory(
                HistoryItem(
                    s.id,
                    s.mid,
                    s.plainText.take(120),
                    s.user?.name.orEmpty(),
                    System.currentTimeMillis(),
                    // 列表里要按原样还原 emoji 与链接，纯文本留作兜底与裁剪后的短标题。
                    s.textHtml.take(1000),
                )
            )
        }
        loadComments(true, force = false)
    }

    LaunchedEffect(listState, comments.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (!switchingSort && last >= comments.size + 2 &&
                    comments.isNotEmpty() && !commentsEnd
                ) {
                    loadComments(false, force = false)
                }
            }
    }

    val scheme = COUITheme.colorScheme
    // The bar floats over the list the same way it does on the feed: the first card starts below it
    // and everything scrolled past runs underneath the glass.
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val bottomBarHeight = 56.dp + navBottomPadding()
    // Rows dim only when the wait is long enough to show the header progress line; a cached or
    // quick response swaps the list with no visible flash at all.
    val commentsAlpha by animateFloatAsState(
        targetValue = if (showSwitchProgress) 0.62f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "commentsAlpha",
    )

    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(pageContainerColor())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = topBarHeight + 8.dp,
                    end = 12.dp,
                    bottom = bottomBarHeight + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    status != null -> item(key = "status") {
                        StatusCard(
                            status = status!!,
                            onOpen = {},
                            onUser = { nav.push(Route.Profile(it.id)) },
                            onImage = { urls, i -> nav.push(Route.Images(urls, i)) },
                            onVideo = { nav.push(Route.Video(it.url, it.title)) },
                            onLike = {
                                val s = status ?: return@StatusCard
                                scope.launch {
                                    runCatching { App.repo.like(s.id, s.liked) }.onSuccess {
                                        status = s.copy(
                                            liked = !s.liked,
                                            attitudes = (s.attitudes + if (s.liked) -1 else 1).coerceAtLeast(0),
                                        )
                                    }.onFailure { App.toast(it.message ?: "操作失败") }
                                }
                            },
                            showActions = false,
                            actionsEnabled = !readOnly,
                            onOpenQuoted = { quoted, quotedOrigin ->
                                quotedOrigin.attachToRow(listState, "status", topBarBounds)
                                nav.push(Route.Detail(quoted.id, quoted.mid, readOnly), quotedOrigin)
                            },
                        )
                    }

                    detailError != null -> item {
                        ErrorState(detailError!!) {
                            scope.launch {
                                detailError = null
                                status = runCatching { App.repo.detail(id, force = true) }.getOrNull()
                            }
                        }
                    }

                    else -> item { LoadingState() }
                }

                if ((status?.editCount ?: 0) > 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_edit_history"),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            onClick = { nav.push(Route.EditHistory(effectiveMid())) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    COUIIcons.Edit,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = scheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "已编辑 ${status!!.editCount} 次",
                                    fontSize = 13.sp,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    " · 查看编辑记录",
                                    fontSize = 13.sp,
                                    color = scheme.primary,
                                )
                            }
                        }
                    }
                }

                item {
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (sort == CommentSort.Hot) "热门评论" else "最新评论",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            // COUI's ChipTab is a 32dp, 14sp control that dwarfs a 13sp section label,
                            // so the two orders share one compact track instead.
                            SortSwitch(
                                selected = sort,
                                onSelect = ::switchSort,
                            )
                        }
                        // A hairline progress bar sits in the header's bottom padding, so a slow
                        // order switch reads as "refreshing in place" instead of a spinner flash.
                        SortProgressLine(
                            visible = showSwitchProgress,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                // Drop into the list's 8dp item gap so the line is not glued to
                                // the sort control's bottom edge.
                                .offset(y = 2.dp)
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .height(2.dp),
                        )
                    }
                }

                itemsIndexed(comments, key = { index, comment -> "${comment.id}#$index" }) { index, comment ->
                    CommentCard(
                        comment = comment,
                        statusId = id,
                        mid = effectiveMid(),
                        modifier = Modifier.graphicsLayer { alpha = commentsAlpha },
                        likeTag = Modifier.testTag("comment_like_$index"),
                        replyTag = Modifier.testTag("comment_reply_$index"),
                        readOnly = readOnly,
                        onUser = { nav.push(Route.Profile(it.id)) },
                    ) {
                        val at = comments.indexOfFirst { it.id == comment.id }
                        if (at < 0) return@CommentCard
                        val current = comments[at]
                        comments[at] = current.copy(
                            liked = !current.liked,
                            likes = (current.likes + if (current.liked) -1 else 1).coerceAtLeast(0),
                        )
                        scope.launch {
                            // Comment likes have their own endpoints; api/attitudes/* is statuses only.
                            runCatching { App.repo.likeComment(current.id, current.liked) }
                                .onFailure {
                                    val back = comments.indexOfFirst { it.id == current.id }
                                    if (back >= 0) comments[back] = current
                                    App.toast(it.message ?: "操作失败")
                                }
                        }
                    }
                }

                item {
                    when {
                        // An order switch keeps the old rows visible and reports progress in the
                        // header, so it must not also drop a spinner under the list.
                        switchingSort -> Unit
                        loadingComments -> LoadingState()
                        commentError != null -> ErrorState(commentError!!) {
                            scope.launch { loadComments(true, force = true) }
                        }
                        commentsEnd -> Text(
                            "没有更多评论",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceSecondary,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            // 内容铺到状态栏下面，顶栏玻璃才有东西可采样（COUI 17 规范要求 edgeToEdge）。
            LocalEdgeToEdge provides true,
        ) {
            PinnedBarDensity {
                // 玻璃比顶栏往下多画 24dp 做渐隐，而正文卡片只在顶栏下留 8dp：常亮的话静止时卡片顶端
                // 就被糊住。改成 COUI 原生的随滚动出现 —— 在顶部时透明，内容滚进顶栏底下 30dp 内淡入。
                val density = LocalDensity.current
                AppBarBlurOverlay(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageContainerColor(),
                    scrollFraction = {
                        val scrolled = if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
                        else listState.firstVisibleItemScrollOffset.toFloat()
                        AppBarBlurOverlayDefaults.scrollAlpha(scrolled, density)
                    },
                ) {
                    SmallTopAppBar(
                        title = "微博正文",
                        modifier = Modifier.reportTopBarBottom(topBarBounds),
                        color = Color.Transparent,
                        showDivider = false,
                        gradientGlass = false,
                        navigationIcon = { BackIconButton { nav.pop() } },
                        actions = {
                            HighlightedIconButton(
                                onClick = { showShare = true },
                                modifier = Modifier.couiTopAppBarActionItem().testTag("detail_share"),
                            ) {
                                Icon(COUIIcons.Share, "分享", Modifier.size(24.dp))
                            }
                        },
                    )
                }
            }
        }

        // 转发、评论、点赞：与信息流卡片同款的玻璃小胶囊，悬浮在评论之上。
        // 左右内缩 26dp（页面 12 + 卡片内衬 14），与主页卡片里的胶囊位置、间距完全一致。
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val current = status
            GlassActionPill(
                icon = CoboIcons.Repost,
                text = fmtCount(current?.reposts ?: 0),
                onClick = { repostDialog = true },
                modifier = Modifier.testTag("detail_repost"),
                enabled = !readOnly,
            )
            GlassActionPill(
                icon = CoboIcons.Comment,
                text = fmtCount(current?.comments ?: 0),
                onClick = { commentDialog = true },
                modifier = Modifier.testTag("detail_comment"),
                enabled = !readOnly,
            )
            GlassActionPill(
                icon = if (current?.liked == true) CoboIcons.HeartFill else CoboIcons.Heart,
                text = fmtCount(current?.attitudes ?: 0),
                onClick = {
                    val s = status ?: return@GlassActionPill
                    scope.launch {
                        runCatching { App.repo.like(s.id, s.liked) }.onSuccess {
                            status = s.copy(
                                liked = !s.liked,
                                attitudes = (s.attitudes + if (s.liked) -1 else 1).coerceAtLeast(0),
                            )
                        }.onFailure { App.toast(it.message ?: "操作失败") }
                    }
                },
                tint = if (current?.liked == true) scheme.primary
                else scheme.onSurfaceSecondary,
                modifier = Modifier.testTag("detail_like"),
                enabled = !readOnly,
            )
        }
    }

    if (showShare) {
        status?.let { current ->
            ShareSheet(
                status = current,
                onDismiss = { showShare = false },
            )
        }
    }

    if (repostDialog) {
        InputDialog(
            title = "转发",
            hint = "说点什么",
            onDismiss = { repostDialog = false },
            onConfirm = { text ->
                repostDialog = false
                scope.launch {
                    runCatching { App.repo.repost(id, effectiveMid(), text) }
                        .onSuccess { App.toast("已转发") }
                        .onFailure { App.toast(it.message ?: "转发失败") }
                }
            },
        )
    }
    if (commentDialog) {
        InputDialog(
            title = "评论",
            hint = "写评论",
            onDismiss = { commentDialog = false },
            onConfirm = { text ->
                commentDialog = false
                scope.launch {
                    runCatching { App.repo.comment(id, effectiveMid(), text) }.onSuccess {
                        App.toast("评论成功")
                        loadComments(true, force = true)
                    }.onFailure { App.toast(it.message ?: "评论失败") }
                }
            },
        )
    }
}

/** How many replies a comment shows before the "共 N 条回复" link. */
private const val PreviewReplies = 1

@Composable
private fun CommentCard(
    comment: Comment,
    statusId: String,
    mid: String,
    modifier: Modifier = Modifier,
    likeTag: Modifier = Modifier,
    replyTag: Modifier = Modifier,
    readOnly: Boolean = false,
    onUser: (User) -> Unit = {},
    onLike: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()
    val replies = remember(comment.id) {
        mutableStateListOf<Comment>().apply { addAll(comment.replies) }
    }
    var replyTotal by remember(comment.id) {
        mutableStateOf(maxOf(comment.replyCount, comment.replies.size))
    }
    var expanded by remember(comment.id) { mutableStateOf(false) }
    var loadingReplies by remember(comment.id) { mutableStateOf(false) }
    var cursor by remember(comment.id) { mutableStateOf<String?>(null) }
    var cursorType by remember(comment.id) { mutableStateOf(0) }
    var repliesDone by remember(comment.id) { mutableStateOf(false) }
    // The comment or nested reply being answered; it owns the dialog so a finished reply can
    // reload this thread instead of the whole comment list.
    var replyTarget by remember(comment.id) { mutableStateOf<Comment?>(null) }

    fun loadMoreReplies() {
        if (loadingReplies || repliesDone) return
        loadingReplies = true
        scope.launch {
            runCatching { App.repo.commentReplies(comment.id, cursor, cursorType) }
                .onSuccess { page ->
                    val before = replies.size
                    val known = replies.mapTo(HashSet()) { it.id }
                    page.items.forEach { if (known.add(it.id)) replies.add(it) }
                    cursor = page.maxId
                    cursorType = page.maxIdType
                    // `max_id` 0 means the thread is exhausted; a page that adds nothing means the
                    // same, and stops a looping "load more" when the count and reality disagree.
                    if (page.items.isEmpty() || replies.size == before ||
                        page.maxId.isBlank() || page.maxId == "0"
                    ) {
                        repliesDone = true
                    }
                    if (page.nextPage > 0) replyTotal = maxOf(replyTotal, page.nextPage)
                }
                .onFailure { App.toast(it.message ?: "回复加载失败") }
            loadingReplies = false
        }
    }

    fun showNewReply(reply: Comment) {
        expanded = true
        replyTotal += 1
        val at = replies.indexOfFirst { it.id == reply.id }
        if (at >= 0) replies[at] = reply else replies.add(0, reply)
    }

    fun likeReply(reply: Comment) {
        val at = replies.indexOfFirst { it.id == reply.id }
        if (at < 0) return
        val current = replies[at]
        replies[at] = current.copy(
            liked = !current.liked,
            likes = (current.likes + if (current.liked) -1 else 1).coerceAtLeast(0),
        )
        scope.launch {
            runCatching { App.repo.likeComment(current.id, current.liked) }
                .onFailure {
                    val back = replies.indexOfFirst { it.id == current.id }
                    if (back >= 0) replies[back] = current
                    App.toast(it.message ?: "操作失败")
                }
        }
    }

    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Avatar(
                comment.user?.avatar,
                34.dp,
                modifier = Modifier.clickable { comment.user?.let(onUser) },
            )
            Spacer(Modifier.width(10.dp))
            // 点评论任意位置（链接除外）= 回复该评论作者。
            Column(
                Modifier
                    .weight(1f)
                    .then(
                        if (readOnly) Modifier
                        else Modifier.clickable { replyTarget = comment },
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.user?.name.orEmpty(),
                        fontSize = 13.sp,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { comment.user?.let(onUser) },
                    )
                    val membership = comment.user?.membership ?: Membership.None
                    if (membership != Membership.None) {
                        Spacer(Modifier.width(4.dp))
                        MemberBadge(membership, compact = true)
                    }
                }
                Spacer(Modifier.height(3.dp))
                WeiboText(
                    html = comment.textHtml,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                comment.reply?.let { reply ->
                    Spacer(Modifier.height(6.dp))
                    // The quoted reply is the page tone inside a card, so the app keeps exactly one
                    // background colour and one card colour instead of inventing a third grey.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pageContainerColor())
                            .padding(8.dp),
                    ) {
                        Text(
                            "@${reply.user?.name.orEmpty()}：",
                            fontSize = 12.sp,
                            color = scheme.primary,
                        )
                        WeiboText(
                            html = reply.textHtml,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                val total = maxOf(replyTotal, replies.size)
                if (total > 0) {
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pageContainerColor())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        val visible = if (expanded) replies else replies.take(PreviewReplies)
                        visible.forEach { reply ->
                            ReplyRow(
                                reply = reply,
                                onLike = { likeReply(reply) },
                                onReply = { replyTarget = reply },
                                enabled = !readOnly,
                                onUser = onUser,
                            )
                        }
                        when {
                            loadingReplies -> Text(
                                "加载中…",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceSecondary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                            expanded && !repliesDone && replies.size < total -> Text(
                                "展开更多回复",
                                fontSize = 12.sp,
                                color = scheme.primary,
                                modifier = Modifier
                                    .clickable { loadMoreReplies() }
                                    .padding(vertical = 4.dp),
                            )
                            expanded -> Text(
                                "收起",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceSecondary,
                                modifier = Modifier
                                    .clickable { expanded = false }
                                    .padding(vertical = 4.dp),
                            )
                            total > visible.size -> Text(
                                "共 $total 条回复",
                                fontSize = 12.sp,
                                color = scheme.primary,
                                modifier = Modifier
                                    .testTag("comment_replies")
                                    .clickable {
                                        expanded = true
                                        if (replies.size < total) loadMoreReplies()
                                    }
                                    .padding(vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    // 右缘与楼中楼的点赞按钮对齐（引用块 8dp 内衬），点赞按钮列成一条线。
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatWeiboTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = scheme.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "回复",
                        fontSize = 12.sp,
                        color = scheme.onSurfaceSecondary,
                        modifier = replyTag
                            .clip(RoundedCornerShape(11.dp))
                            .then(
                                if (readOnly) Modifier.alpha(0.38f)
                                else Modifier.clickable { replyTarget = comment },
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    // scheme.background on a card is only ~1.1:1, too faint to read as a badge;
                    // a tinted ink chip keeps the two-colour palette and still stands out. The
                    // badge stays visible at zero so the count is never missing.
                    Row(
                        modifier = likeTag
                            // 点赞徽章固定尺寸：计数长短不再改变按钮宽度。
                            .width(45.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (comment.liked) scheme.primary.copy(alpha = 0.16f)
                                else scheme.onSurface.copy(
                                    alpha = if (scheme.background.luminance() < 0.5f) 0.20f else 0.12f
                                )
                            )
                            .then(
                                if (readOnly) Modifier.alpha(0.38f)
                                else Modifier.clickable(onClick = onLike),
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (comment.liked) CoboIcons.HeartFill else CoboIcons.Heart,
                            contentDescription = if (comment.liked) "取消点赞" else "点赞",
                            modifier = Modifier.size(13.dp),
                            tint = if (comment.liked) scheme.primary else scheme.onSurface,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = fmtCount(comment.likes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (comment.liked) scheme.primary else scheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    replyTarget?.let { target ->
        InputDialog(
            title = "回复 @${target.user?.name.orEmpty()}",
            hint = "回复",
            onDismiss = { replyTarget = null },
            onConfirm = { text ->
                replyTarget = null
                scope.launch {
                    runCatching { App.repo.replyComment(statusId, mid, target.id, text) }
                        .onSuccess {
                            App.toast("回复成功")
                            showNewReply(it)
                        }
                        .onFailure { App.toast(it.message ?: "回复失败") }
                }
            },
        )
    }
}

@Composable
private fun ReplyRow(
    reply: Comment,
    onLike: () -> Unit,
    onReply: () -> Unit,
    enabled: Boolean = true,
    onUser: (User) -> Unit = {},
) {
    val scheme = COUITheme.colorScheme
    // 整条回复可点：回复该层作者。楼中楼不再放缩小的「回复」按钮。
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onReply)
                else Modifier,
            )
            .padding(vertical = 3.dp),
    ) {
        Avatar(
            reply.user?.avatar,
            22.dp,
            modifier = Modifier.clickable { reply.user?.let(onUser) },
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reply.user?.name.orEmpty(),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { reply.user?.let(onUser) },
                )
                val membership = reply.user?.membership ?: Membership.None
                if (membership != Membership.None) {
                    Spacer(Modifier.width(4.dp))
                    MemberBadge(membership, compact = true)
                }
            }
            Spacer(Modifier.height(2.dp))
            WeiboText(
                html = reply.textHtml,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatWeiboTime(reply.createdAt),
                    fontSize = 11.sp,
                    color = scheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                Row(
                    modifier = Modifier
                        .testTag("reply_like")
                        // 点赞徽章固定尺寸；这条的右缘就是全评论区点赞列的对齐基准。
                        .width(45.dp)
                        .clip(RoundedCornerShape(11.dp))
                        // 楼中楼是次级操作：底色与墨色都比主评论淡一档（DESIGN.md 的
                        // 「UI recedes / 弱化墨色」层级原则），点赞态仍用主色表达状态。
                        .background(
                            if (reply.liked) scheme.primary.copy(alpha = 0.12f)
                            else scheme.onSurface.copy(alpha = 0.07f)
                        )
                        .then(
                            if (enabled) Modifier.clickable(onClick = onLike)
                            else Modifier.alpha(0.38f),
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (reply.liked) CoboIcons.HeartFill else CoboIcons.Heart,
                        contentDescription = if (reply.liked) "取消点赞" else "点赞",
                        modifier = Modifier.size(13.dp),
                        tint = if (reply.liked) scheme.primary
                        else scheme.onSurface.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = fmtCount(reply.likes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (reply.liked) scheme.primary
                        else scheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

private val SortEntries = listOf("热度" to CommentSort.Hot, "时间" to CommentSort.Time)
private val SortSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 420f)
private val SortPressSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

@Composable
private fun SortSwitch(
    selected: CommentSort,
    onSelect: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = COUITheme.colorScheme
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = remember { TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium) }
    // Both labels are measured at the heavier weight so the pill never clips a label as the
    // selected side swaps in, and so the control grows with the user's font scale.
    val labelSize = remember(measurer, labelStyle) {
        val sizes = SortEntries.map { measurer.measure(it.first, labelStyle).size }
        sizes.maxOf { it.width } to sizes.maxOf { it.height }
    }
    val segmentWidth = with(density) { labelSize.first.toDp() } + 20.dp
    val segmentHeight = with(density) { labelSize.second.toDp() } + 8.dp
    val segmentWidthPx = with(density) { segmentWidth.toPx() }
    val selectedIndex = SortEntries.indexOfFirst { it.second == selected }.coerceAtLeast(0)
    val indicator = remember { Animatable(selectedIndex.toFloat()) }
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()
    val interactions = remember { List(SortEntries.size) { MutableInteractionSource() } }
    val pressed = interactions.map { it.collectIsPressedAsState() }
    val dark = scheme.background.luminance() < 0.5f

    LaunchedEffect(selectedIndex) {
        if (dragValue == null) indicator.animateTo(selectedIndex.toFloat(), SortSpring)
    }

    val value = dragValue ?: indicator.value

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(scheme.onSurface.copy(alpha = if (dark) 0.16f else 0.11f))
            .selectableGroup()
            .padding(2.dp)
            .pointerInput(segmentWidthPx, selectedIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { dragValue = indicator.value },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        dragValue = ((dragValue ?: indicator.value) + delta / segmentWidthPx)
                            .coerceIn(0f, (SortEntries.size - 1).toFloat())
                    },
                    onDragEnd = {
                        val start = dragValue ?: indicator.value
                        val target = start.roundToInt().coerceIn(0, SortEntries.size - 1)
                        scope.launch {
                            indicator.snapTo(start)
                            dragValue = null
                            if (target != selectedIndex) onSelect(SortEntries[target].second)
                            else indicator.animateTo(target.toFloat(), SortSpring)
                        }
                    },
                    onDragCancel = {
                        val start = dragValue ?: indicator.value
                        scope.launch {
                            indicator.snapTo(start)
                            dragValue = null
                            indicator.animateTo(selectedIndex.toFloat(), SortSpring)
                        }
                    },
                )
            },
    ) {
        Box(
            Modifier
                .offset(x = segmentWidth * value)
                .width(segmentWidth)
                .height(segmentHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(if (dark) scheme.surfaceContainerHigh else scheme.surfaceContainer),
        )
        Row(Modifier.height(segmentHeight)) {
            SortEntries.forEachIndexed { index, (label, entrySort) ->
                val proximity = (1f - abs(value - index)).coerceIn(0f, 1f)
                val labelScale by animateFloatAsState(
                    targetValue = if (pressed[index].value) 0.94f else 1f,
                    animationSpec = SortPressSpring,
                    label = "sortLabelScale",
                )
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = labelScale
                            scaleY = labelScale
                        }
                        .selectable(
                            selected = selected == entrySort,
                            interactionSource = interactions[index],
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onSelect(entrySort) },
                        )
                        .testTag(
                            if (entrySort == CommentSort.Hot) "comment_sort_hot" else "comment_sort_time"
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (proximity > 0.5f) FontWeight.Medium else FontWeight.Normal,
                        color = lerp(scheme.onSurfaceSecondary, scheme.onSurface, proximity),
                    )
                }
            }
        }
    }
}

@Composable
private fun SortProgressLine(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "sortProgressAlpha",
    )
    if (alpha <= 0.01f) return
    val transition = rememberInfiniteTransition(label = "sortProgress")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sortProgressPhase",
    )
    val scheme = COUITheme.colorScheme
    val trackAlpha = if (scheme.background.luminance() < 0.5f) 0.10f else 0.045f
    Canvas(modifier.graphicsLayer { this.alpha = alpha }) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = scheme.onSurface.copy(alpha = trackAlpha),
            cornerRadius = radius,
        )
        val bar = size.width * 0.32f
        drawRoundRect(
            color = scheme.primary.copy(alpha = 0.48f),
            topLeft = Offset((size.width + bar) * phase - bar, 0f),
            size = Size(bar, size.height),
            cornerRadius = radius,
        )
    }
}
