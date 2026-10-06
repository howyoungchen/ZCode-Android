# 0001. 逆向复刻官方 Web 协议，协议栈全部独立实现

- 状态：已接受
- 日期：2026-10-07

## 背景（Context）

ZCode 官方只提供桌面端与 Web 远程控制页面，没有公开的移动端 SDK 或远程控制 API 文档。
要在 Android 上实现远程控制，只能从官方 Web 客户端的行为中还原通信协议。
同时项目承诺"全部代码为独立实现"（README），且明确不与官方有任何关系。

## 决策（Decision）

对官方 Web 远程控制页面抓包与逆向，独立重写整套协议栈
（`app/src/main/java/app/zemote/protocol/`：Relay 配对、IPC 编解码、rpc-frame 分片、
Channel RPC、Conversation V4），行为与官方 Web 客户端一致，代码不复制官方实现。

## 备选方案（Alternatives）

- **WebView 套壳加载官方 Web 页**：依赖浏览器、交互体验差、无法做前台保活与原生通知；
  且 README 明确目标是"不依赖浏览器"。
- **等待/请求官方开放 API**：不可控，且项目定位是非官方个人工具。
- **直接搬运官方前端 JS 逻辑**：与"独立实现"承诺冲突，许可风险大。

## 后果（Consequences）

- 好处：不依赖官方发布移动端；体积与行为完全自主可控。
- 代价：**协议没有任何稳定性承诺**——官方一更新就可能失效（README 免责声明已写明）；
  每次失效都需要重新抓包比对。仓库中保留了官方 JS 样本作对照材料
  （`analysis/official/`、`zcode_downloaded/`、根目录 `bundle.js` 等）。
- 推翻条件：官方发布正式远程 API / 移动端时，应迁移并废弃本协议栈。
