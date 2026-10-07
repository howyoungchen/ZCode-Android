<div align="center">

# Zemote

**在手机上远程控制桌面端 ZCode**

[![Release](release-shield)][release-url]
[![Downloads](downloads-shield)][downloads-url]
![License](license-shield)
![Platform](platform-shield)

**[下载 APK](https://github.com/howyoungchen/ZCode-Android/releases/latest)** ·
**[在线预览](https://howyoungchen.github.io/ZCode-Android/)** ·
**[更新日志](CHANGELOG.md)**

简体中文 · [English](README_EN.md)

原生 Android 客户端 · Kotlin + Jetpack Compose（Material 3）· 独立实现官方远控协议

</div>

Zemote 逆向了 ZCode 官方 Web 远程控制页的通信协议，把整个远控体验原生搬到 Android：
流式对话、任务面板、权限审批、附件上传，全部不依赖浏览器。纯客户端设计，
只与官方 Relay 通信，不收集任何数据。

<p align="center">
  <img src="screenshots/step1_home.png" width="30%" alt="远控仪表盘：按工作区分组的任务列表" />
  <img src="screenshots/step3_add_device.png" width="30%" alt="扫码或粘贴链接添加设备" />
  <img src="screenshots/home_dark.png" width="30%" alt="深色模式" />
</p>

## 关于本 Fork

本仓库 fork 自 [Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android)，
感谢原作者 [Damian2012](https://github.com/Damianjiang) 完成协议逆向与整体架构。

这个 fork 的使命，是**把界面美学打磨得更贴近 Z.ai**：以官方设计语言为基准——
黑白单色为底、sky 蓝点缀——远控仪表盘、会话文档流与发送栏逐块对齐官方移动端远控页，
而不只是「配色接近」。同时持续修复协议层可靠性问题
（冷启动会话加载、流式卡顿、断线重连等，见[更新日志](CHANGELOG.md)）。

**今后本项目在本仓库独立开发与维护**，由 [howyoungchen](https://github.com/howyoungchen) 负责。

## 主要功能

**连接与控制**

- 扫码或粘贴配对链接添加设备，多设备保存、一键切换
- 远控仪表盘：按工作区分组的任务列表、运行状态胶囊、一键新建任务
- 任务面板：查看后台运行任务与待审批交互，一键终止
- 权限审批：在手机上批准文件访问与命令执行

**对话体验**

- 流式输出：思考、回复、工具调用边生成边显示，Markdown 渲染、图片内联
- 每条工具调用一行摘要（终端 / 读取 / 搜索 / MCP / 子智能体），点击展开原始输出
- 消息排队：AI 回复期间发送的消息支持立即发送、编辑、删除、拖动排序
- 模型与推理强度切换，上下文用量实时显示；图片和文件分片上传
- 子智能体只读会话，返回时自动恢复父会话

**可靠与隐私**

- 断线自动重连、桥重建与订阅恢复，网络切换后无需重启 App
- 前台保活服务，降低后台被杀导致的掉线
- 凭据经 Android Keystore（AES/GCM）加密，仅存手机本地
- 零遥测、不请求 Relay 之外的任何接口；中文 / English 双语，深浅色主题

## 快速上手

1. 桌面端 ZCode 打开「远程控制」，生成配对二维码
2. 手机打开 Zemote，扫码或粘贴配对链接
3. 选择工作区，进入任务会话，开始对话

## 下载安装

- 前往 [GitHub Releases](https://github.com/howyoungchen/ZCode-Android/releases) 下载最新 APK
- 系统要求：Android 9+（API 28），仅 arm64-v8a 架构
- 安装前想先看看界面：[在线预览页](https://howyoungchen.github.io/ZCode-Android/)

## 从源码构建

需要 JDK 17 与 Android SDK 35。

```bash
git clone https://github.com/howyoungchen/ZCode-Android.git
cd ZCode-Android
./gradlew assembleRelease   # Windows 用 gradlew.bat
```

产物在 `app/build/outputs/apk/release/`。release 包开启 R8 与资源收缩，
使用 debug 签名，可直接安装；日常开发用 `assembleDebug`。

## 协议实现

协议栈逐层逆向自官方 Web 客户端，行为保持一致，代码全部独立实现：

| 层级 | 说明 |
|---|---|
| Relay | wss 长连接，10s 心跳，指数退避重连 |
| 配对 | HMAC-SHA256 配对证明 |
| IPC | 7-bit varint 编解码（String / Int / JSON / Bytes / Array） |
| RpcFrame | 512KB 分片、CRC32 校验、ack 应答、断线重传 |
| Channel RPC | request/response + 事件订阅 |
| Conversation V4 | 快照与增量订阅、流式重组、消息队列、附件上传、权限审批 |

逐层的消息、字段与时序规范见[协议文档](docs/protocol.md)。

## 项目结构

纯客户端单模块 `:app`：

```
app/src/main/java/app/zemote/
├── protocol/    # 协议栈：Relay、配对、IPC 编解码、rpc-frame、Conversation V4
├── state/       # 状态层：设备持久化、凭据加密、连接与会话管理
├── ui/          # Compose 界面：主题、通用组件、各页面
├── service/     # 前台保活服务
└── crash/       # 崩溃捕获与报告
```

模块分层与关键流程时序见[架构文档](docs/architecture.md)。

## 文档

| 文档 | 内容 |
|---|---|
| [架构文档](docs/architecture.md) | 模块划分、协议栈分层、关键流程时序 |
| [协议规范](docs/protocol.md) | 逆向协议逐层规范：消息、字段、时序常量 |
| [数据模型](docs/data-model.md) | 本地存储（DataStore / Keystore）与运行时数据 |
| [Runbook](docs/runbook.md) | 构建、发布、日志与故障排查 |
| [ADR](docs/adr/README.md) | 关键技术决策及其取舍 |
| [更新日志](CHANGELOG.md) | 每个版本的改动明细 |

## 社区

- QQ 群 [1090759263](https://qm.qq.com/q/1090759263)：使用交流与问题反馈
- [GitHub Issues](https://github.com/howyoungchen/ZCode-Android/issues)：Bug 报告与功能建议

## ⚠️ 免责声明

- Zemote **不是官方客户端**，与 ZCode / Z.ai 无任何隶属关系。协议来自对官方 Web 页面的
  抓包与逆向，官方更新可能导致失效。
- 仅供连接你自己的设备使用，请遵守服务条款与当地法律，风险自负。
- 配对 URL 中的 `sid` / `hash` 等同于设备控制权，**不要分享给任何人**；
  若泄露，在桌面端重新生成二维码即可作废旧凭据。
- 本项目不收集任何数据，凭据经 Keystore 加密后仅存于手机本地。

## 致谢与许可证

- 原项目 [Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android)（Damian2012）——
  协议逆向与整体架构；对话协议实现参考了同项目原 Flutter 版本（协议行为一致）。
- 原项目开发中有 AI（大语言模型）辅助编写部分代码；整体架构与核心协议由原作者独立完成并负责，
  AI 产出经人工审查、修正后纳入。

以 MIT 许可证发布。ZCode 名称及相关商标归其权利人所有，本项目与其无任何隶属关系。

[release-shield]: https://img.shields.io/github/v/release/howyoungchen/ZCode-Android?style=flat-square&color=0ea5e9
[release-url]: https://github.com/howyoungchen/ZCode-Android/releases/latest
[downloads-shield]: https://img.shields.io/github/downloads/howyoungchen/ZCode-Android/total?style=flat-square&color=0ea5e9
[downloads-url]: https://github.com/howyoungchen/ZCode-Android/releases
[license-shield]: https://img.shields.io/badge/license-MIT-111827?style=flat-square
[platform-shield]: https://img.shields.io/badge/Android-9%2B-111827?style=flat-square&logo=android&logoColor=white
