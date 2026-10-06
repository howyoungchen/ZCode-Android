# AGENTS.md

Zemote（仓库 ZCode-Android）是非官方的 Android 端 ZCode 远程控制客户端：
逆向官方 Web 远控页的通信协议，让用户在手机上操控自己桌面端的 ZCode。
纯客户端、单模块 `:app`，Kotlin + Jetpack Compose（Material 3），唯一网络对端是官方 Relay。

## 怎样写代码和文档

始终用中文沟通。代码注释、说明文档和界面提示都用自然的中文。
写清楚对象、动作和结果。标题直接说明要检查什么或怎样操作，避免抽象名词堆叠。

动手前先读对应文档：
- 改代码前先读 [docs/architecture.md](docs/architecture.md)
- 改协议栈（protocol/ 目录）前先读 [docs/protocol.md](docs/protocol.md)
- 动数据 / 持久化逻辑前先读 [docs/data-model.md](docs/data-model.md)
- 构建、发布、排障：[docs/runbook.md](docs/runbook.md)
- 不明白某设计为什么存在：查 [docs/adr/README.md](docs/adr/README.md)

文档随代码一起改：模块增删、依赖变化时更新 architecture.md；
改 protocol/ 下任何文件时更新 protocol.md；存储结构变化时更新 data-model.md；
每次真实故障处理完在 runbook.md 补一条排查条目。
ADR 只增不改：决策变更时新增一条，把旧的状态改为「被 NNNN 取代」。

优先选择简单、可运行、容易修改的代码，不增加与当前功能无关的防御代码或复杂抽象。
只在业务规则、平台限制和不直观的处理处添加注释，不逐行翻译代码。
协议栈分层保持现状：上层依赖下层，protocol/ 不依赖 UI 类（ZemoteLogger 除外）。

## 改协议时要保留什么

协议逆向自官方 Web 客户端，官方没有稳定性承诺。事实来源是 protocol/ 目录的代码；
官方 JS 对照材料在 analysis/official/、zcode_downloaded/ 和根目录 bundle.js，只读参考，不进编译。
配对握手、10s 心跳、指数退避重连、512KB 分片加 CRC32、baseRevision CAS 与 stale 重试，
这些消息名、字段和时序常量以 docs/protocol.md 为准，不凭感觉改。
官方协议更新导致连不上时，先抓官方 Web 新版 JS 重新比对，再动 protocol/，
改完必须跑 `bash tools/verify-ipc-codec/run.sh`，退出码 0 才算通过。

`IpcCodec.kt` 是每帧双向必经的热路径：写入端不用 ByteArrayOutputStream，读取端不做多余复制。
改动前先读文件头的性能约定，改完用上面的脚本验证。
对话行按 rowId 乱序重放时退化为 upsert，不产生重复行。
同一工作区共用一条 bridge，会话仓库每账号 LRU 上限 8，淘汰时关闭对应 bridge。

## 改数据和凭据时要保留什么

配对 URL 里的 sid / hash 等同设备控制权，不能写进源码、日志、文档或测试报告。
凭据只在本地存放：Keystore（别名 zemote_key）AES-256/GCM 加密后落盘，
密文带 `enc:` 前缀，历史明文数据兼容读取。
`AccountStore.exportJson()` 的导出内容含明文凭据，不能外发或提交。
禁止清空 filesDir——DataStore 设备列表在 filesDir/datastore/ 下，清掉即丢失全部已配对设备。
「缓存清理」页只清 cacheDir、调试日志和崩溃报告，永远不碰 DataStore。
App 不上报遥测，不请求 Relay 之外的第三方接口。

## 怎样保持界面一致

色盘对齐官方 ZCode 远控页：zai 黑白单色为底，sky 蓝作点缀。
颜色一律从 ui/theme/Color.kt 取，组件复用 ui/component/，不新开硬编码色值。
深浅色两种模式都要检查，改颜色时 Light / Dark 两套同步改。
全部界面文本中英双语：改 values/strings.xml 时同步改 values-en/strings.xml，不留下只有中文的键。
界面只说用户能理解的内容，不把协议字段、存储键名等实现细节放进普通页面。

## 怎样保持聊天页流畅

聊天页对重组敏感，v1.9.2 拆好的重组域不能退化。
高频状态（usage、queueItems、modelOptions 等流式期间频繁变化的状态）
只在发送区等局部订阅，不放聊天页顶层。
传给 MessageTimeline 等列表的 lambda 用 remember / rememberUpdatedState 固定实例，
避免每次重组都是新参数导致整列表重排。
新增订阅前先想清楚变化频率：变化越频繁，订阅越往下沉。

## 怎样管理分支和版本

main 始终保持可构建、可发布；不直接在 main 上改代码，所有改动先开分支。
每个功能或修复从最新的 main 创建分支，命名用 feature/、fix/、docs/ 前缀加一两个词，
例如 feature/任务面板终止、fix/心跳超时。
提交信息用中文短句说明改了什么，可加 feat:、fix:、docs: 前缀；
一次提交只做一件事，凭据、密钥和签名文件不进入提交。
分支合并回 main 前，先 `git fetch origin` 并在分支上 `git rebase main`、解决冲突，
回 main 执行 `git merge --ff-only <分支名>` 快进合并，推送后删除已合并分支，保持 main 历史线性。
合并前在分支上完成「交付前检查什么」的全部检查；被设备或凭据阻止的项目写明原因，再决定是否合并。

每次代码修改合并回 main 后，紧跟一次版本提升：在 main 上单独提交，
只改 app/build.gradle.kts 的 versionCode 和 versionName，versionCode 每次 +1；
新功能或明显改版提次版本号（如 1.10.0），修复和小调整提末位（如 1.9.7）。
纯文档修改不提版本。版本提交写成 `chore: bump version to vX.Y.Z`。
发版时对齐四处口径：app/build.gradle.kts、CHANGELOG.md 顶部条目、
ChangelogScreen.kt 的硬编码日志、docs/index.html 落地页版本号，不一致先对齐再发。
release 构建使用 debug 签名（ADR 0006），APK 只通过 GitHub Releases 分发，构建产物不入库。

## 交付前检查什么

执行 `./gradlew :app:compileDebugKotlin assembleDebug`；涉及发布再跑 `assembleRelease`。
改了 protocol/ 的编解码逻辑，执行 `bash tools/verify-ipc-codec/run.sh`，退出码必须是 0。
打正式包后可用 `python tools/check_16kb_alignment.py apk <app.apk>` 检查原生库 16KB 页对齐。
仓库当前没有单元测试源集，CI 只部署 docs 不构建 APK，编译验证全靠本地执行，
不能因为页面能打开就跳过上面的命令。
有设备时手工核对主链路：扫码或粘贴配对 → bootstrap 出工作区列表 → 打开会话看到流式输出
→ 发消息与排队 → 断网后自动重连恢复。用真实桌面端 ZCode 配对测试，不把 mock 协议层当成通过。
设备、官方协议或网络原因跑不通的项目明确写原因，不能写成已通过。
修改行为后更新 README.md 和 docs/ 下对应文档。
