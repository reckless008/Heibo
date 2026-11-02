package com.cobo.app.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.cobo.app.App
import com.cobo.app.ui.NavState
import com.cobo.app.ui.PinnedBarDensity
import com.cobo.app.ui.pageContainerColor
import com.cobo.app.ui.CoboIcons
import com.cobo.app.ui.components.ActionIcon
import com.cobo.app.ui.components.BackIconButton
import com.cobo.app.ui.components.LoadingState
import com.cobo.app.ui.components.glassEdgeLight
import com.cobo.app.ui.components.glassPlate
import com.cobo.app.ui.components.glassPlateAlpha
import com.cobo.app.ui.components.liquidGlass
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Close
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

private val EMOJIS = listOf(
    "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊",
    "😋", "😎", "😍", "😘", "😗", "😙", "😚", "🙂", "🤗", "🤩",
    "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮",
    "🤐", "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜", "😝",
    "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "🙁", "😖",
    "😞", "😟", "😤", "😢", "😭", "😦", "😧", "😨", "😩", "🤯",
    "😬", "😰", "😱", "🥵", "🥶", "😳", "🤪", "😵", "🥴", "😠",
    "😡", "🤬", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "😇", "🥳",
    "🥺", "🤠", "🤡", "🤥", "🤫", "🤭", "🧐", "🤓", "😈", "👿",
    "💀", "👻", "👽", "🤖", "💩", "😺", "😸", "😹", "😻", "😼",
)

@Composable
fun PublishScreen(nav: NavState, draft: String) {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf(draft) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var publishing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var emojiOpen by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            images = (images + uris).distinct().take(9)
        }
    }

    fun leave() {
        if (text.isNotBlank()) App.store.addDraft(text)
        nav.pop()
    }

    BackHandler {
        if (!publishing) leave()
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            PinnedBarDensity {
                SmallTopAppBar(
                    color = pageContainerColor(),
                    title = "发微博",
                    navigationIcon = { BackIconButton { leave() } },
                    actions = {
                        TextButton(
                            text = if (publishing) "发布中…" else "发布",
                            modifier = Modifier.testTag("publish_send"),
                            enabled = !publishing && (text.isNotBlank() || images.isNotEmpty()),
                            onClick = {
                                scope.launch {
                                    publishing = true
                                    error = null
                                    try {
                                        val ids = ArrayList<String>()
                                        images.forEachIndexed { index, uri ->
                                            progress = "上传图片 ${index + 1}/${images.size}"
                                            ids.add(App.repo.uploadImage(uri))
                                        }
                                        progress = "正在发布…"
                                        App.repo.publish(text.trim(), ids)
                                        App.notifyPublished()
                                        progress = ""
                                        nav.pop()
                                    } catch (t: Throwable) {
                                        error = t.message ?: "发布失败"
                                    } finally {
                                        publishing = false
                                    }
                                }
                            },
                        )
                    },
                )
            }
        },
    ) { padding ->
        val dark = COUITheme.colorScheme.background.luminance() < 0.5f
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // 输入区把「工具栏 + 媒体区」之外的剩余高度吃满：键盘收起时不再留一大块空白，
            // 键盘弹起时按剩余高度收缩；内容超长/多图由外层滚动承接。
            val fieldHeight = (maxHeight - 194.dp).coerceAtLeast(160.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    // 底部工具条悬浮在内容之上，留出它的高度，滚到底时最后一行才不被压住。
                    .padding(bottom = 76.dp),
            ) {
                ComposerField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight)
                        .testTag("publish_input"),
                )
                Spacer(Modifier.height(10.dp))
                if (images.isNotEmpty()) {
                    ImagePickerGrid(images, onRemove = { uri -> images = images - uri }) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(COUITheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(COUIIcons.Image, "添加图片", Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "最多 9 张图片",
                            fontSize = 12.sp,
                            color = COUITheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                }
                if (progress.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(progress, fontSize = 13.sp, color = COUITheme.colorScheme.primary)
                }
                if (publishing) {
                    LoadingState()
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, fontSize = 13.sp, color = COUITheme.colorScheme.error)
                }
            }

            // 图片 / 表情 / @ / # 和字数贴底，键盘弹起时随 imePadding 一起上移。
            // 外层 Box 只负责定位与外边距：liquidGlass 会丢弃排在它前面的 modifier，
            // 尺寸和位置必须接在它后面，否则整条会退回 Box 左上角。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            tint = glassPlate(dark),
                            alpha = glassPlateAlpha(dark),
                        )
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .glassEdgeLight(dark)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(COUIIcons.Image, "", onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    ActionIcon(CoboIcons.Emoji, "", onClick = { emojiOpen = true })
                    ActionIcon(CoboIcons.At, "", onClick = { text += "@" })
                    ActionIcon(CoboIcons.Topic, "", onClick = { text += "##" })
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${text.length}",
                        fontSize = 12.sp,
                        color = COUITheme.colorScheme.onSurfaceSecondary,
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
    }

    if (emojiOpen) {
        EmojiDialog(
            onDismiss = { emojiOpen = false },
            onPick = { emoji ->
                text += emoji
                emojiOpen = false
            },
        )
    }
}

@Composable
private fun ImagePickerGrid(
    images: List<Uri>,
    onRemove: (Uri) -> Unit,
    onAdd: () -> Unit,
) {
    val rows = (images.size + 2) / 3
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    if (index < images.size) {
                        val uri = images[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(COUITheme.colorScheme.surfaceContainerHigh),
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(COUITheme.colorScheme.background.copy(alpha = 0.85f))
                                    .clickable { onRemove(uri) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(COUIIcons.Close, "移除", Modifier.size(14.dp))
                            }
                        }
                    } else if (index == images.size && images.size < 9) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(COUITheme.colorScheme.surfaceContainerHigh)
                                .clickable(onClick = onAdd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(COUIIcons.Add, "添加图片", Modifier.size(26.dp))
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
        ) {
            Text(
                "表情",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .height(320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EMOJIS.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPick(emoji) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                        repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(text = "关闭", onClick = onDismiss)
            }
        }
    }
}

/**
 * 发布输入区：基础 BasicTextField 默认顶对齐，避免 COUI TextField 在固定高度里把文字/占位符
 * 垂直居中；底线固定在输入区底部，占位符贴左上，和下方媒体区/工具条形成完整编排。
 */
@Composable
private fun ComposerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = COUITheme.colorScheme
    Box(modifier) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.onSurface.copy(alpha = 0.10f)),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 10.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = scheme.onSurface,
            ),
            cursorBrush = SolidColor(scheme.primary),
            maxLines = 14,
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (value.isEmpty()) {
                        Text(
                            "分享新鲜事…",
                            fontSize = 15.sp,
                            color = scheme.onSurfaceSecondary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
