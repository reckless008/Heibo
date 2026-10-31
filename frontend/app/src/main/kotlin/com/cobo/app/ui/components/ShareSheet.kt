// 分享面板：复制全部文本 / 复制链接 / 生成图片 / 私信好友 / 更多分享（系统面板）。
package com.cobo.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cobo.app.App
import com.cobo.app.data.MessageItem
import com.cobo.app.data.User
import com.cobo.app.data.Status
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Image as CopyImageIcon
import io.github.suqi8.coui.kmp.icon.extended.Link
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.Send
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.theme.COUITheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShareStage { Menu, Direct }

/**
 * 微博分享面板。文案 = 正文 + 链接，四个主动作 + 系统分享兜底。
 */
@Composable
fun ShareSheet(
    status: Status,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = COUITheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    var stage by remember { mutableStateOf(ShareStage.Menu) }
    var conversations by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }
    var loadingDm by remember { mutableStateOf(false) }
    var sendingName by remember { mutableStateOf<String?>(null) }

    val url = "https://m.weibo.cn/detail/${status.id}"
    val shareText = if (status.plainText.isBlank()) url else "${status.plainText}\n$url"

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        App.toast("已复制")
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                when (stage) {
                    ShareStage.Menu -> {
                        Text(
                            "分享微博",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurface,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShareTile(
                                icon = COUIIcons.Copy,
                                label = "复制全文",
                                modifier = Modifier.weight(1f),
                            ) { copy("微博", shareText) }
                            ShareTile(
                                icon = COUIIcons.Link,
                                label = "复制链接",
                                modifier = Modifier.weight(1f),
                            ) { copy("链接", url) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShareTile(
                                icon = COUIIcons.CopyImageIcon,
                                label = "生成图片",
                                modifier = Modifier.weight(1f),
                            ) {
                                scope.launch {
                                    App.toast("正在生成图片…")
                                    val bitmap = renderShareCard(context, status, url)
                                    val bytes = bitmap?.let { pngBytes(it) }
                                    val uri = bytes?.let { saveShareImage(context, it) }
                                    if (uri == null) {
                                        App.toast("生成图片失败")
                                        return@launch
                                    }
                                    App.toast("已保存到相册")
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(send, "分享图片"))
                                    onDismiss()
                                }
                            }
                            ShareTile(
                                icon = COUIIcons.Messages,
                                label = "私信好友",
                                modifier = Modifier.weight(1f),
                            ) {
                                stage = ShareStage.Direct
                                if (conversations.isEmpty() && following.isEmpty() && !loadingDm) {
                                    loadingDm = true
                                    scope.launch {
                                        conversations = runCatching { App.repo.messages(0) }
                                            .getOrDefault(emptyList())
                                        val me = runCatching { App.repo.ownUid() }.getOrNull()
                                        following = if (me.isNullOrBlank()) emptyList()
                                        else runCatching { App.repo.followingList(me) }
                                            .getOrDefault(emptyList())
                                        loadingDm = false
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ShareTile(
                            icon = COUIIcons.Share,
                            label = "更多分享",
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(send, "分享微博"))
                            onDismiss()
                        }
                    }

                    ShareStage.Direct -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "私信好友",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "返回",
                                fontSize = 13.sp,
                                color = scheme.primary,
                                modifier = Modifier
                                    .clickable { stage = ShareStage.Menu }
                                    .padding(6.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (loadingDm) {
                            Text(
                                "加载好友中…",
                                fontSize = 13.sp,
                                color = scheme.onSurfaceSecondary,
                                modifier = Modifier.padding(vertical = 18.dp),
                            )
                        } else {
                            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                                fun sendTo(user: User?) {
                                    if (user == null || sendingName != null) return
                                    sendingName = user.name
                                    scope.launch {
                                        val ok = runCatching {
                                            App.repo.sendChat(user.id, shareText)
                                        }.getOrNull() != null
                                        App.toast(
                                            if (ok) "已私信 ${user.name}"
                                            else "发送失败，请稍后再试",
                                        )
                                        sendingName = null
                                        onDismiss()
                                    }
                                }

                                // 最近会话（有的话排在前面）
                                conversations.take(6).mapNotNull { it.user }.let { recent ->
                                    if (recent.isNotEmpty()) {
                                        Text(
                                            "最近会话",
                                            fontSize = 12.sp,
                                            color = scheme.onSurfaceSecondary,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                        )
                                        recent.forEach { user ->
                                            ShareDmRow(
                                                user = user,
                                                sending = sendingName == user.name,
                                                onClick = { sendTo(user) },
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }

                                // 我的关注：按头像选人，没有会话也能直接私信
                                if (following.isNotEmpty()) {
                                    Text(
                                        "我的关注",
                                        fontSize = 12.sp,
                                        color = scheme.onSurfaceSecondary,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                    following.take(20).forEach { user ->
                                        ShareDmRow(
                                            user = user,
                                            sending = sendingName == user.name,
                                            onClick = { sendTo(user) },
                                        )
                                    }
                                }

                                if (conversations.isEmpty() && following.isEmpty()) {
                                    Text(
                                        "还没有可私信的好友",
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceSecondary,
                                        modifier = Modifier.padding(vertical = 18.dp),
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
private fun ShareDmRow(
    user: User,
    sending: Boolean,
    onClick: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(url = user.avatar, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            user.name,
            fontSize = 14.sp,
            color = scheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (sending) {
            Text(
                "发送中…",
                fontSize = 12.sp,
                color = scheme.onSurfaceSecondary,
            )
        }
    }
}

@Composable
private fun ShareTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (scheme.background.luminance() < 0.5f) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.045f),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, label, Modifier.size(22.dp), tint = scheme.onSurface)
        Text(label, fontSize = 12.sp, color = scheme.onSurface)
    }
}

/** 把位图压成 PNG 字节。 */
private suspend fun pngBytes(bitmap: Bitmap): ByteArray = withContext(Dispatchers.IO) {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.toByteArray()
}

/** 拉取头像位图（纯网络流，避免图库组件的异步时序）。 */
private suspend fun loadBitmap(url: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isNullOrBlank()) return@withContext null
    runCatching {
        val conn = URL(url).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        conn.getInputStream().use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}

/**
 * 微博文案出图：白卡 + 头像/昵称/时间 + 正文 + 链接 + 落款。
 * 纯 Canvas 绘制，尺寸 1080 宽，高度按正文自适应。
 */
private suspend fun renderShareCard(
    context: Context,
    status: Status,
    url: String,
): Bitmap? = withContext(Dispatchers.Default) {
    val avatar = loadBitmap(status.user?.avatar)
    val width = 1080
    val pad = 72f
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1D1D1F")
        textSize = 42f
        typeface = Typeface.DEFAULT
    }
    val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#6E7683")
        textSize = 30f
    }
    val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1D1D1F")
        textSize = 46f
        typeface = Typeface.DEFAULT_BOLD
    }
    val bodyText = status.plainText
        .replace("\uFFFC", "")
        .replace("[obj]", "")
        .ifBlank { "（微博正文）" }
    val bodyLayout = StaticLayout.Builder
        .obtain(bodyText, 0, bodyText.length, textPaint, (width - pad * 2).toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.35f)
        .build()

    val headerH = 150f
    val bodyH = bodyLayout.height.toFloat()
    val footerH = 300f
    val height = (pad + headerH + 24f + bodyH + 40f + 56f + footerH + pad).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    // 头像
    val avatarSize = 116f
    if (avatar != null) {
        val circle = Path().apply {
            addOval(RectF(pad, pad, pad + avatarSize, pad + avatarSize), Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(circle)
        canvas.drawBitmap(
            avatar,
            null,
            RectF(pad, pad, pad + avatarSize, pad + avatarSize),
            Paint(Paint.ANTI_ALIAS_FLAG),
        )
        canvas.restore()
    }

    // 昵称 + 时间
    val name = status.user?.name.orEmpty()
    canvas.drawText(name, pad + avatarSize + 24f, pad + 52f, namePaint)
    val meta = buildString {
        append(formatShareTime(status.createdAt))
        if (status.source.isNotBlank()) append("  来自 ").append(status.source)
    }
    canvas.drawText(meta, pad + avatarSize + 24f, pad + 104f, metaPaint)

    // 正文
    canvas.save()
    canvas.translate(pad, pad + headerH + 24f)
    bodyLayout.draw(canvas)
    canvas.restore()

    // 链接
    val linkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4E6E96")
        textSize = 32f
    }
    canvas.drawText(url, pad, pad + headerH + 24f + bodyH + 44f, linkPaint)

    // 落款 + 二维码（扫码直达原文）
    val footY = height - pad
    val qrSize = 240
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E5E5EA")
        strokeWidth = 2f
    }
    canvas.drawLine(pad, footY - footerH + 40f, width - pad, footY - footerH + 40f, dividerPaint)
    val qr = qrBitmap(url, qrSize)
    if (qr != null) {
        canvas.drawBitmap(
            qr,
            null,
            RectF(
                width - pad - qrSize,
                footY - qrSize,
                width - pad,
                footY,
            ),
            Paint(Paint.ANTI_ALIAS_FLAG),
        )
    }
    val footTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1D1D1F")
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText("来自 Heibo", pad, footY - qrSize + 72f, footTitle)
    canvas.drawText("扫码查看微博原文", pad, footY - qrSize + 136f, metaPaint)

    bitmap
}

/** 把链接编码成二维码位图。 */
private fun qrBitmap(url: String, size: Int): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bmp
}.getOrNull()

private fun formatShareTime(raw: String): String {
    // 出图上的时间直接用展示层同款规则。
    return com.cobo.app.data.formatWeiboTime(raw)
}
