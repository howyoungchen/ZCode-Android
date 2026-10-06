# 0004. 同一工作区单条 bridge 多订阅，配 LRU 上限 8

- 状态：已接受
- 日期：2026-10-07

## 背景（Context）

一个工作区下有多个会话（对话、任务、子智能体）。每个会话若各自向桌面端开一条
workspace bridge，桌面端会"后者顶掉前者"，两条桥互相踢，表现为数据时有时无。
同时，会话仓库（`ConversationV4Session`）持有 bridge 与订阅，浏览的工作区多了会
无限增长、耗尽资源。

（来源：state/AppSessionViewModel.kt 内注释，属显式记录的设计依据。）

## 决策（Decision）

- 会话仓库按 `accountId|workspaceKey` 维度**共享**：同一工作区只有一条 bridge，
  该工作区下所有会话的订阅都复用这条桥——对齐官方 Web 的"单桥多订阅"架构
  （AppSessionViewModel.conversationFor）。
- 仓库缓存为访问序 LRU，单设备上限 `MAX_CONVERSATIONS_PER_ACCOUNT = 8`
  （曾为 4，因"点开 5 个工作区后最早那个被淘汰、当前聊天页收不到推送"而调大）。
  淘汰即 dispose（关闭 bridge 与订阅），页面重新进入时自动重建。
- 恢复场景由 `BridgeSession.swapBridge` 支持换桥重建 transport/channel 栈。

## 备选方案（Alternatives）

- **每会话一条 bridge**：多桥互踢（前述根因），不可行。
- **无上限缓存**：内存与桌面端资源随浏览增长，失控。
- **全局单 bridge（跨工作区共享）**：bridge 与 workspaceKey 绑定（workspace-bridge-open），
  协议上不支持。

## 后果（Consequences）

- 好处：与官方行为一致、资源有界；上限调整有实证依据（4→8）。
- 代价：用户快速浏览超过 8 个工作区时，最早的会被断开重连（有重建兜底）；
  上限是硬编码常量，调整需改代码发版。
- 推翻条件：若官方协议支持跨会话的多路复用或 bridge 池，可重访此结构。
