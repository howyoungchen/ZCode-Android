package app.zemote.ui.theme

import androidx.compose.ui.graphics.Color

// ─── 官方 ZCode 远程控制页（zai 主题）设计令牌 ───
// 提取自官方 Web 客户端样式表（analysis/official/page.html + 官方 CSS）：
// 黑白单色 + 天蓝 brand。primary 浅色为近黑、深色为近白；
// brand=sky（浅 sky-400/深 sky-500），卡片走「边框 + 纯色底」而非色调叠加。

// 浅色（zai-light：底 #fafafa = neutral-50）
val NeutralBackgroundLight = Color(0xFFFAFAFA)
val NeutralOnBackgroundLight = Color(0xFF404040)
val NeutralVariantLight = Color(0xFFF5F5F5)
val NeutralOnVariantLight = Color(0xFF525252)
val OutlineLight = Color(0xFFD4D4D4)
val OutlineVariantLight = Color(0xFFE5E5E5)
val SurfaceDimLight = Color(0xFFDCDCDC)
val SurfaceBrightLight = Color(0xFFFAFAFA)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF5F5F5)
val SurfaceContainerLight = Color(0xFFF0F0F0)
val SurfaceContainerHighLight = Color(0xFFE5E5E5)
val SurfaceContainerHighestLight = Color(0xFFD4D4D4)
val InverseSurfaceLight = Color(0xFF262626)
val InverseOnSurfaceLight = Color(0xFFF5F5F5)

// 深色（zai-dark：底 #161616，官方 meta theme-color）
val NeutralBackgroundDark = Color(0xFF161616)
val NeutralOnBackgroundDark = Color(0xFFE5E5E5)
val NeutralVariantDark = Color(0xFF1F1F1F)
val NeutralOnVariantDark = Color(0xFFA3A3A3)
val OutlineDark = Color(0xFF4D4D4D)
val OutlineVariantDark = Color(0xFF2E2E2E)
val SurfaceDimDark = Color(0xFF111111)
val SurfaceBrightDark = Color(0xFF3A3A3A)
val SurfaceContainerLowestDark = Color(0xFF0A0A0A)
val SurfaceContainerLowDark = Color(0xFF1C1C1C)
val SurfaceContainerDark = Color(0xFF262626)
val SurfaceContainerHighDark = Color(0xFF2B2B2B)
val SurfaceContainerHighestDark = Color(0xFF383838)
val InverseSurfaceDark = Color(0xFFE5E5E5)
val InverseOnSurfaceDark = Color(0xFF262626)

// 错误色（官方 destructive：浅 red-600 / 深 red-500）
val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorDark = Color(0xFFEF4444)
val OnErrorDark = Color(0xFFFFFFFF)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val Scrim = Color(0xFF000000)

// 语义色（连接状态等业务用途；官方 success：浅 green-600 / 深 green-500）
val StatusSuccess = Color(0xFF16A34A)
val StatusSuccessDark = Color(0xFF22C55E)

// 官方 diff 绿（工具卡 +N 行数）
val DiffAdded = Color(0xFF16A34A)
val DiffAddedDark = Color(0xFF22C55E)
