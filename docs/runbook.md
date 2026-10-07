# Runbook 运维手册

> 最后更新：2026-10-07 ｜ 维护规则：每次真实故障处理完后补一条排查条目

本项目是 Android 客户端，**没有自管服务端**，"运维"对象是：构建发布流程、GitHub Pages 落地页、以及用户侧协议故障的定位。

## 0. 紧急速查卡

| 症状 | 一行处理 |
|---|---|
| 官方协议更新导致连不上 / 功能失效 | 抓官方 Web 新版 JS 重新比对协议（参考 `analysis/`、`zcode_downloaded/` 的做法），改 `app/src/main/java/app/zemote/protocol/`，用 `bash tools/verify-ipc-codec/run.sh` 验证编解码 |
| 构建失败（本机） | 确认 JDK 17 + Android SDK 35 已装、`local.properties` 指向正确 SDK 路径，再 `./gradlew :app:compileDebugKotlin` 看具体报错 |
| 用户报崩溃 | 让用户在崩溃报告页复制内容（含版本/机型/堆栈），或 `adb logcat` 复现；崩溃文件在应用 `filesDir/crash_report.txt` |

## 1. 环境清单

| 环境 | 地址 | 部署方式 | 配置差异 |
|---|---|---|---|
| 开发机 | 本地 | `./gradlew assembleDebug` 直装 | `local.properties` 含本机 SDK 路径（勿提交） |
| 发布分发 | GitHub Releases（⚠️ 待确认：仓库内无发布自动化） | 手动上传 APK | release 构建启用 R8 + 资源收缩，**debug 签名** |
| 落地页 | https://howyoungchen.github.io/ZCode-Android | push main 自动部署（两个 workflow 均部署 `docs/`，见 §8） | 仅静态页 |

## 2. 配置项

仓库内没有 `.env` 类配置；环境相关文件：

| 项 | 位置 | 说明 |
|---|---|---|
| SDK 路径 | `local.properties`（gitignored） | `sdk.dir=...`，含本机路径勿提交 |
| Gradle 参数 | `gradle.properties` | JVM 参数等 |
| 版本号 | `app/build.gradle.kts` | `versionCode`（24）/ `versionName`（1.9.3）；⚠️ 与 CHANGELOG 顶部（v1.9.2）、落地页标注（v1.9.6）口径不一致，发版前先对齐 |

## 3. 构建与部署

前置：JDK 17、Android SDK 35（README「构建」节）。

```bash
./gradlew assembleDebug       # 日常调试 → app/build/outputs/apk/debug/
./gradlew assembleRelease     # 分发版 → app/build/outputs/apk/release/（R8+资源收缩，debug 签名）
./gradlew :app:compileDebugKotlin   # 快速验证编译（CHANGELOG 中作为标准验证命令）
```

验证工具（不依赖模拟器）：

```bash
bash tools/verify-ipc-codec/run.sh          # IPC 编解码器纯 JVM 断言验证，退出码 0=通过
python tools/check_16kb_alignment.py apk <app.apk>   # 检查原生库 16KB 页对齐
```

发布步骤：

1. 改 `app/build.gradle.kts` 版本号，`CHANGELOG.md` 补条目，`ui/screens/ChangelogScreen.kt` 的硬编码日志同步更新；
2. `./gradlew assembleRelease`；
3. 上传 APK 到 GitHub Releases（⚠️ 待确认具体流程，仓库内无脚本/CI）；
4. 落地页版本号如需展示，改 `docs/index.html`（push main 后 CI 自动部署）。

回滚：APK 无增量迁移，直接安装上一个 Release 的 APK 即可（本地数据向前兼容由代码保证，如 `enc:` 前缀兼容历史明文）。落地页回滚 = revert 对应 commit 再 push。

## 4. 日志

三层日志，按排障深度递进：

| 层 | 位置 | 查看方式 |
|---|---|---|
| 应用内调试日志 | 内存（ZemoteLogger 环形缓冲，1000 条） | App「设置 → 日志」，支持一键复制；可整体关闭 |
| 崩溃报告 | `filesDir/crash_report.txt` | 崩溃后下次启动自动展示；`adb shell run-as app.zemote cat files/crash_report.txt`（debug 包） |
| 系统日志 | logcat | `adb logcat --pid=$(adb shell pidof -s app.zemote)` |

调试日志自 2026-10-07 起**同步镜像到 logcat**（tag 前缀 `Zemote/`），实机排障直接：

```bash
adb logcat -s Zemote/protocol:* Zemote/v4:* Zemote/ipc:*
```

调试日志的关键 tag / 关键字（grep 什么判断什么）：

| 关键字 | 含义 |
|---|---|
| `[relay]` | WSS 连接层：连接、配对、心跳、重连（`reconnect in`、`kicked by another client`） |
| `[ipc]` | Channel RPC 层：`initialized`、invalid frame |
| `[bridge]` | 工作区桥开闭与恢复 |
| `[v4]` | 对话协议：握手、订阅、`command ... stale`、resync |
| `action` | 用户操作行为（打开会话、发消息等） |

## 5. 健康检查与监控

- 仓库内**没有**任何监控 / 告警配置（客户端项目，不适用服务端探活）。落地页在线状态即 GitHub Pages 默认可用性。
- CI 检查项：`.github/workflows/deploy-docs.yml` 与 `pages.yml` 仅部署 `docs/`，**不构建 APK**——APK 构建质量目前靠本地执行 §3 命令保证（仓库无 test source set）。

## 6. 故障排查表

| 症状 | 可能原因 | 诊断步骤 | 处理 |
|---|---|---|---|
| 添加设备报「Cannot parse pairing URL」 | URL 缺 `sid`/`hash`/`t` 或非 https/wss | 检查粘贴的链接完整性 | 重新生成配对二维码 |
| 卡在连接中，日志反复 `reconnect in` | 桌面端不在线 / Relay 不可达 / 协议变更 | 看日志 `[relay]` 具体阶段；官方 Web 远程页能否正常连 | 桌面端重开远程控制；若官方页正常而 App 失败 → 协议变更，按 §0 第一条处理 |
| `kicked by another client` 反复出现 | 同一设备被多个客户端（如浏览器页 + App）争抢 | 日志 grep `kicked` | 关掉其它客户端；App 自带 1s→8s 退避抢回（RelayClient） |
| 会话一直转圈加载不出来 | 历史加载软失败 / 订阅后快照未到 / bridge 被淘汰 | 日志 grep `[v4]`、`[bridge]` | v1.9.2 已修三处根因（CHANGELOG）；仍复现则收集日志开 Issue |
| 冷启动首开会话必现「无法获取会话」 | 桌面端对上一进程遗留桥报 `rpc-transport-fault` → 桥重建后本端握手标志残留，新连接上的调用被桌面端以 `fault.connection.handshakeRequired` 拒绝 | `adb logcat -s Zemote/protocol` 抓 `handshakeRequired` / `re-handshaking` | 2026-10-07 已修：`call()` 收到 handshakeRequired 自动重握手重试；`swapBridge` 触发 `recovered` 重建订阅 |
| 「加载更早消息」永远失败 | `conversationRowsRangeV4` 的 `beforeRowId` 发了字符串，桌面端 Zod 校验要 number | 日志 grep `expected number, received string` | 2026-10-07 已修：游标改 Long；日志若再现即为回归 |
| 流式期间每隔几十秒卡一下、日志反复 `resync (gap)` | 快照/resync 后迟到的旧帧（toSeq≤本地 seq）被误判为断档，触发全量快照循环 | 日志 grep `resync (gap)` 看频率 | 2026-10-07 已修：对齐官方 v4-store 语义，迟到帧直接跳过 |
| 断网（WiFi↔4G 切换）后 App 再也收不到数据 | `onFailure` 只置 ERROR 不重连，`poke()` 无调用方，永久卡死 | 日志看 `[relay] connect failed` 后是否有 `reconnect in` | 2026-10-07 已修：已配对过的连接 onFailure 走退避自动重连，实测 2 秒内全链路自愈 |
| 仪表盘「新建任务」发首条消息必失败，日志 `createSession rejected: proto.invalidPayload`（sessionId expected string, received undefined） | 命令信封缺 sessionId 键：无会话命令省略了必填可空字段，或编码层丢掉了 null 值 | `adb logcat -s Zemote/protocol` grep `createSession rejected` | 2026-10-08 已修：信封恒带 sessionId（无会话传 null），IPC Gson 启用 serializeNulls；若回归先查这两处 |
| 聊天页卡顿 | 高频状态订阅重组 / 时间线 lambda 不稳定 | 复现机型 +日志 | v1.9.2 已做重组域拆分（CHANGELOG），新案例开 Issue |
| App 启动即崩溃页 | 上次崩溃残留报告 | 读崩溃报告内容 | 页面「重启」即清除；按堆栈定位 |
| 构建报 SDK/版本错误 | JDK 或 SDK 版本不对 | `java -version`、检查 `local.properties` | 装 JDK 17 / SDK 35 |

## 7. 数据操作

无 SQL / 无 ORM。涉及用户数据的操作：

- **设备列表备份**：`AccountStore.exportJson()`（⚠️ 无 UI 入口，如需人工备份只能临时调用或 adb 备份 DataStore 文件）。
- **用户侧清数据**：App「设置 → 缓存清理」只清 cacheDir / 调试日志 / 崩溃报告；**不会**碰 DataStore（设备列表）——这是代码注释里的显式设计约束（CacheCleanScreen.kt）。
- 危险操作红名单见 [data-model.md §6](./data-model.md)。

## 8. 已知坑与升级路径

- 两个 Pages workflow（`deploy-docs.yml`：paths 过滤；`pages.yml`：所有 push）功能重复且并发组相同，会互相排队（⚠️ 待确认是否合并）。
- 搞不定时：GitHub Issues（github.com/howyoungchen/ZCode-Android/issues）。⚠️ 值班 / oncall 途径待确认。
