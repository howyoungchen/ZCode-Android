package app.zemote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ─── 品牌色盘 ───

/**
 * 一套品牌色的三色组（主 / 辅 / 点缀，各自含 on 与 container 色）。
 * 结构保留以兼容个性化页；内容对齐官方 ZCode 远程控制页（zai 主题）。
 */
data class BrandTrio(
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val tertiaryContainer: Color, val onTertiaryContainer: Color,
)

data class PaletteSpec(
    val key: String,
    val label: String,
    val swatch: Color,
    val light: BrandTrio,
    val dark: BrandTrio,
)

/**
 * 唯一色盘「zai」：官方 ZCode 远程控制页的黑白单色 + sky brand。
 * - primary：浅色近黑（neutral-950）/ 深色近白（neutral-50），主按钮即官方样式
 * - secondary：sky 色系（浅 sky-600 / 深 sky-400），官方 --color-brand
 * - tertiary：官方 warning 黄（浅 yellow-700 / 深 yellow-500），连接中态使用
 */
val Palettes: List<PaletteSpec> = listOf(
    PaletteSpec(
        "zai", "ZCode", Color(0xFF0A0A0A),
        BrandTrio(
            Color(0xFF0A0A0A), Color(0xFFFAFAFA), Color(0xFFF0F9FF), Color(0xFF0C4A6E),
            Color(0xFF0284C7), Color(0xFFFFFFFF), Color(0xFFE0F2FE), Color(0xFF075985),
            Color(0xFFA16207), Color(0xFFFFFFFF), Color(0xFFFEF9C3), Color(0xFF422006),
        ),
        BrandTrio(
            Color(0xFFFAFAFA), Color(0xFF0A0A0A), Color(0xFF0A3346), Color(0xFF7DD3FC),
            Color(0xFF38BDF8), Color(0xFF082F49), Color(0xFF0A3346), Color(0xFF7DD3FC),
            Color(0xFFEAB308), Color(0xFF1C1917), Color(0xFF422006), Color(0xFFFDE047),
        ),
    ),
)

fun paletteSpec(key: String): PaletteSpec = Palettes.firstOrNull { it.key == key } ?: Palettes.first()

// ─── 中性色（各色盘共享） ───

private val LightNeutrals = lightColorScheme(
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = NeutralBackgroundLight,
    onBackground = NeutralOnBackgroundLight,
    surface = NeutralBackgroundLight,
    onSurface = NeutralOnBackgroundLight,
    surfaceVariant = NeutralVariantLight,
    onSurfaceVariant = NeutralOnVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Scrim,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkNeutrals = darkColorScheme(
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralBackgroundDark,
    onSurface = NeutralOnBackgroundDark,
    surfaceVariant = NeutralVariantDark,
    onSurfaceVariant = NeutralOnVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Scrim,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

private fun BrandTrio.lightScheme(): androidx.compose.material3.ColorScheme = LightNeutrals.copy(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
)

private fun BrandTrio.darkScheme(): androidx.compose.material3.ColorScheme = DarkNeutrals.copy(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
)

@Composable
fun ZemoteTheme(
    themeManager: ThemeManager,
    content: @Composable () -> Unit,
) {
    val themeState by themeManager.state.collectAsState()
    val darkTheme = when (themeState.mode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    val spec = paletteSpec(themeState.palette)
    // 动态取色（Material You）会让整体观感偏离官方远控页配色，已移除；
    // 仅保留浅色 / 深色 / 跟随系统三种模式。
    val colorScheme = if (darkTheme) spec.dark.darkScheme() else spec.light.lightScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZemoteTypography,
        content = content,
    )
}

/**
 * 官方卡片底色：浅色主题为白卡（neutral-50 底上的 --color-card），
 * 深色主题为 neutral-800。官方远控页的卡片靠 1px 边框而非色调叠加区分，
 * 配合 [androidx.compose.foundation.border] 使用。
 */
@Composable
fun cardContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) scheme.surfaceContainerLowest
    else scheme.surfaceContainer
}

/** 官方 selected 底（10% 前景色叠加）：列表选中行 / 运行中会话的高亮 */
@Composable
fun selectedContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) Color(0xFFE5E5E5) else Color(0xFF2E2E2E)
}

/** 以下辅助色对齐官方移动端远控页令牌（zai-light / zai-dark 两套），见 Color.kt 注释 */

/** 顶栏底色：官方 --color-header */
@Composable
fun headerColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) HeaderBgLight else HeaderBgDark

/** 用户消息气泡底：官方 --color-secondary */
@Composable
fun userBubbleColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) UserBubbleLight else UserBubbleDark

/** 最浅前景：官方 --color-foreground-subtlest，用于工具行图标与摘要 */
@Composable
fun subtlestColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) OnSurfaceSubtlestLight else OnSurfaceSubtlestDark

/** 卡片内图标方块底：官方 --color-surface（前景色低比例叠加） */
@Composable
fun surfaceTintColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SurfaceTintLight else SurfaceTintDark

/** 运行中状态胶囊底：官方 --color-accent */
@Composable
fun accentColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) AccentLight else AccentDark

/** 工具行来源徽章底：官方 --color-background-alt */
@Composable
fun backgroundAltColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) BackgroundAltLight else BackgroundAltDark
