<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/banner-dark.svg">
  <img src="docs/assets/banner-light.svg" alt="Zemote" height="96">
</picture>

**桌面端的 ZCode，装进口袋。**

<a href="https://github.com/howyoungchen/ZCode-Android/releases">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/btn-download-zh-dark.svg">
    <img src="docs/assets/btn-download-zh-light.svg" alt="下载 APK" height="28">
  </picture>
</a>
&nbsp;
<a href="https://howyoungchen.github.io/ZCode-Android/">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/btn-preview-zh-dark.svg">
    <img src="docs/assets/btn-preview-zh-light.svg" alt="在线预览" height="28">
  </picture>
</a>

[English](README.md) · 简体中文

Kotlin · Jetpack Compose · Material 3 · MIT

</div>

## 为什么做 Zemote？

编码智能体不会因为你离开工位就停下来，但想在手机上盯一眼进度，过去只能开一个
浏览器标签页，让桌面布局在六寸屏幕上硬撑。Zemote 是一个非官方的独立客户端，
用 Kotlin 原生重讲了官方 Web 远控的那套「语言」：会话、工具调用、权限审批，
全部收进拇指够得到的距离。它只与官方 Relay 通信，也不收集任何数据。

> [!IMPORTANT]
> **非官方项目。** Zemote 是社区开发的第三方客户端，与 Z.ai / ZCode 官方没有
> 任何隶属关系，亦未获官方授权；ZCode 名称与相关商标归 Z.ai 所有。如本项目
> 内容涉及侵权，请[直接提交 Issue](https://github.com/howyoungchen/ZCode-Android/issues)
> 联系我们，会在第一时间处理。

<table>
  <tr>
    <td width="33%" align="center">
      <img src="screenshots/chat_light.png" width="260" alt="实时会话：思考行、工具调用与 Markdown 回复"><br>
      <b>实时会话</b><br>
      <sub>思考行、工具调用、Markdown 回复，边生成边显示</sub>
    </td>
    <td width="33%" align="center">
      <img src="screenshots/chat_dark.png" width="260" alt="同一会话的深色主题"><br>
      <b>深色会话</b><br>
      <sub>同一套黑白单色设计语言，熄灯形态</sub>
    </td>
    <td width="33%" align="center">
      <img src="screenshots/step1_home.png" width="260" alt="远控仪表盘：按工作区分组的任务"><br>
      <b>远控仪表盘</b><br>
      <sub>工作区与任务一览，状态胶囊实时更新</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" align="center">
      <img src="screenshots/step3_add_device.png" width="260" alt="扫桌面端二维码添加设备"><br>
      <b>快速配对</b><br>
      <sub>扫桌面端二维码或粘贴链接，多台设备切换</sub>
    </td>
    <td width="33%" align="center">
      <img src="screenshots/home_dark.png" width="260" alt="深色模式仪表盘"><br>
      <b>夜间仪表盘</b><br>
      <sub>深色下状态胶囊与路径依然清晰</sub>
    </td>
    <td width="33%" align="center">
      <img src="screenshots/step4_settings_page.png" width="260" alt="设置页"><br>
      <b>设置</b><br>
      <sub>主题、语言、缓存管理与调试日志</sub>
    </td>
  </tr>
</table>

## 为什么有这个 fork？

本仓库 fork 自 [Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android)，
协议栈与整体架构归功于原作者 [Damian2012](https://github.com/Damianjiang)。

fork 的理由只有一个：**让它看起来像 Z.ai 亲自发布的。** 黑白单色界面、sky 蓝
点缀，仪表盘与对话流逐块对照官方移动端远控页，而不只是「配色接近」。引擎盖
之下，协议可靠性也在持续加固：冷启动会话加载、流式卡顿、断线重连都已重做，
细节见[更新日志](CHANGELOG.md)。

**今后项目在本仓库独立演进，由 [howyoungchen](https://github.com/howyoungchen) 维护。**

## 你能得到什么

| | |
|---|---|
| **秒级配对** —— 扫桌面端二维码或粘贴链接，保存多台设备，一键切换 | **任务仪表盘** —— 工作区与任务一览，状态胶囊实时更新，一键新建任务 |
| **实时对话** —— 思考、回复、工具调用边生成边显示，完整 Markdown 与内联图片 | **消息排队** —— AI 忙时继续输入，排队消息发送前可编辑、排序、删除 |
| **随手审批** —— 文件访问、命令执行在手机上点一下，失控任务一键终止 | **子智能体** —— 打开任意子智能体的只读会话，返回时回到原来的位置 |
| **连接稳固** —— 网络切换后自动重连并恢复订阅，前台保活降低后台被杀 | **隐私优先** —— 凭据封存于 Android Keystore，零遥测，不连 Relay 以外的任何端点 |

## 快速上手

1. **桌面端** —— 打开 ZCode → *远程控制* → 生成配对二维码。
2. **手机** —— 安装 Zemote，扫码或粘贴链接。这条链接要当密码保管：
   谁拿到它，谁就控制了你的机器。
3. **开聊** —— 选择工作区，进入任务会话；追问排队、上传附件、审批操作，随手就来。

每个版本都会在 [Releases](https://github.com/howyoungchen/ZCode-Android/releases) 页
发布 APK，也可以直接用下面的源码构建。要求：Android 9+（API 28），arm64-v8a。
装之前想先看看界面？有一个
[在线预览](https://howyoungchen.github.io/ZCode-Android/)。

## 它是如何工作的

Zemote 是纯客户端——一条连到官方 Relay 的 WebSocket，上面叠着逐层逆向的协议栈。
每一层都与官方 Web 客户端行为一致，代码全部从零实现：

```
Conversation V4    快照与增量 · 流式 · 消息队列 · 附件 · 权限审批
Channel RPC        request/response + 事件订阅
rpc-frame          512KB 分片 · CRC32 校验 · ack · 断线重发
IPC codec          7-bit varint（String / Int / JSON / Bytes / Array）
Pairing            HMAC-SHA256 配对证明
Relay              wss 长连接 · 10s 心跳 · 指数退避重连
                              ⇅
                    官方 Relay ⇄ 桌面端 ZCode
```

线上格式的逐字段规范见[协议文档](docs/protocol.md)。

## 从源码构建

需要 JDK 17 与 Android SDK 35。

```bash
git clone https://github.com/howyoungchen/ZCode-Android.git
cd ZCode-Android
./gradlew assembleRelease   # Windows 用 gradlew.bat
```

产物在 `app/build/outputs/apk/release/`：R8 收缩、资源收缩、debug 签名，
下载即可直接安装。日常开发用 `assembleDebug`。

## 常见问题

**这是官方应用吗？**
不是。Zemote 非官方，与 ZCode、Z.ai 没有任何隶属关系。协议逆向自官方 Web
远控页，官方一更新就可能失效——真发生时会重新比对官方新版客户端并更新协议栈。

**配对链接能分享给别人吗？**
绝对不能。链接里的 `sid` / `hash` 等同于桌面端 ZCode 的完整控制权。一旦泄露，
在桌面端重新生成二维码，旧凭据即刻作废。

**它会收集什么数据？**
什么都不收集。没有遥测、没有第三方接口，只连官方 Relay。凭据经 Android
Keystore（AES/GCM）加密，永不离开手机。

**支持哪些设备？**
Android 9+（API 28），仅 arm64-v8a。深浅色主题，中文 / English 双语界面。

**遇到问题去哪反馈？**
[GitHub Issues](https://github.com/howyoungchen/ZCode-Android/issues)。
应用内的调试日志（设置 → 调试）记录了全部协议交互，一键复制即可随反馈附上。

## 项目布局

单 Gradle 模块，纯客户端：

```
app/src/main/java/app/zemote/
├── protocol/    # 逆向协议栈：relay → 配对 → IPC → rpc-frame → conversation
├── state/       # 设备、加密凭据、连接与会话状态
├── ui/          # Compose 页面、主题、通用组件
├── service/     # 前台保活服务
└── crash/       # 崩溃捕获与报告
```

## 文档

位于 `docs/` 目录：

- [architecture.md](docs/architecture.md) —— 应用是如何组织的
- [protocol.md](docs/protocol.md) —— 线上协议，逐字段
- [data-model.md](docs/data-model.md) —— 磁盘上都存了什么
- [runbook.md](docs/runbook.md) —— 构建、发布与排障
- [adr/](docs/adr/README.md) —— 每个设计为什么是现在这样
- [CHANGELOG.md](CHANGELOG.md) —— 每个版本的改动

## 社区

- [GitHub Issues](https://github.com/howyoungchen/ZCode-Android/issues) —— Bug 报告、功能建议与交流

## 致谢

- [Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android)
  （[Damian2012](https://github.com/Damianjiang)）—— 本 fork 站在其上的协议
  逆向与整体架构；对话协议同时参考了该项目更早的 Flutter 版本（线上行为一致）。
- 原项目开发中有 AI（大语言模型）辅助编写部分代码；整体架构与核心协议为
  原作者独立完成，AI 产出均经人工审查后纳入。

## 许可证

MIT。ZCode 与 Z.ai 名称归其权利人所有，本项目与其无隶属关系。
