package app.zemote.ui.component

import app.zemote.R

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.zemote.state.ConnectionState
import app.zemote.ui.theme.DiffAdded
import app.zemote.ui.theme.DiffAddedDark
import app.zemote.ui.theme.StatusSuccess
import app.zemote.ui.theme.StatusSuccessDark
import kotlin.math.abs

/** 呼吸脉冲状态点：连接中/已连接时持续呼吸，静止时恒定 */
@Composable
fun StatusDot(
    color: Color,
    pulsing: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 8,
) {
    if (!pulsing) {
        Box(modifier.size(size.dp).background(color, CircleShape))
        return
    }
    val transition = rememberInfiniteTransition(label = "statusPulse")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val halo by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseHalo",
    )
    Box(
        modifier = modifier.size((size * 2.4f).dp),
        contentAlignment = Alignment.Center,
    ) {
        // 向外扩散的光晕
        Box(
            modifier = Modifier
                .size((size * 2.4f).dp * halo.coerceAtLeast(0.1f))
                .background(color.copy(alpha = 0.35f * (1f - halo)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size((size.dp) * scale)
                .background(color, CircleShape)
        )
    }
}

private val DeviceGlyphs = listOf(
    Icons.Rounded.Devices,
    Icons.Rounded.Laptop,
    Icons.Rounded.Memory,
    Icons.Rounded.Router,
)

private fun deviceGlyph(id: String): ImageVector =
    DeviceGlyphs[abs(id.hashCode()) % DeviceGlyphs.size]

/**
 * 设备头像：对齐官方远控页的单色观感 —— 中性底 + 1px 边框 + 前景色图标
 * （替代旧的彩色渐变盘；官方界面没有彩色头像）。
 */
@Composable
fun DeviceAvatar(
    id: String,
    modifier: Modifier = Modifier,
    iconSize: Int = 24,
    corner: Int = 10,
) {
    Box(
        modifier = modifier
            .size((iconSize + 20).dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(corner.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(corner.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = deviceGlyph(id),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

/** 连接状态 → 文案、颜色、是否脉冲（颜色按官方语义色随主题切换） */
data class StatusPresentation(val label: String, val color: Color, val pulsing: Boolean)

@Composable
fun statusPresentation(state: ConnectionState, message: String?): StatusPresentation {
    val dark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    return when (state) {
        // 官方 success：浅 green-600 / 深 green-500
        ConnectionState.CONNECTED -> StatusPresentation(
            stringResource(R.string.status_connected),
            if (dark) StatusSuccessDark else StatusSuccess,
            true,
        )
        // 官方 warning：浅 yellow-700 / 深 yellow-500（映射到 tertiary）
        ConnectionState.CONNECTING -> StatusPresentation(
            message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.connecting),
            MaterialTheme.colorScheme.tertiary,
            true,
        )
        ConnectionState.ERROR -> StatusPresentation(
            message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.connect_failed),
            MaterialTheme.colorScheme.error,
            false,
        )
        ConnectionState.IDLE -> StatusPresentation(stringResource(R.string.not_connected), MaterialTheme.colorScheme.onSurfaceVariant, false)
    }
}

/** 工作区头像：与设备头像同款单色方块（按 key 稳定取图标） */
@Composable
fun WorkspaceAvatar(key: String, modifier: Modifier = Modifier, icon: ImageVector) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** 官方 diff 绿（+N 行数）：浅 green-600 / 深 green-500 */
@Composable
fun diffAddedColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) DiffAdded else DiffAddedDark
