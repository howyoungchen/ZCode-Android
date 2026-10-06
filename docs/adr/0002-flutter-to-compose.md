# 0002. 由 Flutter 版本迁移到 Kotlin + Jetpack Compose 重写

- 状态：已接受（补记）
- 日期：2026-10-07
- **本 ADR 为事后补记**：理由系根据现状推断（README 与 git 历史），待维护者确认。

## 背景（Context）

README 记载"对话协议的实现细节参考了同项目原 Flutter 版本（协议行为一致）"，
即作者此前有一个 Flutter 实现的同类客户端。本仓库首个提交（8aafbdb，
"Zemote v1.1.0：ZCode Android 远程控制客户端（非官方·协议逆向复刻）"）
就已经是 Kotlin + Compose 版本，仓库内没有 Flutter 残留代码。

## 决策（Decision）

放弃继续维护 Flutter 版本，用 Kotlin + Jetpack Compose（Material 3）从零重写 Android 客户端，
协议行为对齐旧 Flutter 版本以降低验证成本。

## 备选方案（Alternatives）

- **继续 Flutter**：（推断）放弃原因可能包括包体积、平台通道与后台保活的原生能力限制、
  以及对纯原生 Compose 的偏好——⚠️ 真实原因待维护者补充。
- **React Native / 其它跨端框架**：同样有原生能力与体积问题，且无既有资产。

## 后果（Consequences）

- 好处：单语言（Kotlin）覆盖 UI 与协议层；前台服务、Keystore、扫码等系统能力直用平台 API；
  依赖面极简（无 DI 框架、无本地数据库，见 build.gradle.kts）。
- 代价：放弃 Flutter 的多端潜力（当前仅 Android）；重写成本已付。
- 推翻条件：需要 iOS 等其它移动端时，需重新评估跨端方案或各端原生分仓。
