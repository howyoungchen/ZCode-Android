# ADR 索引

| 编号 | 标题 | 状态 | 日期 |
|---|---|---|---|
| [0001](./0001-reverse-engineered-protocol.md) | 逆向复刻官方 Web 协议，协议栈全部独立实现 | 已接受 | 2026-10-07 |
| [0002](./0002-flutter-to-compose.md) | 由 Flutter 版本迁移到 Kotlin + Jetpack Compose 重写 | 已接受（补记） | 2026-10-07 |
| [0003](./0003-keystore-credential-cipher.md) | 设备凭据用 Android Keystore AES/GCM 加密存储 | 已接受 | 2026-10-07 |
| [0004](./0004-single-bridge-multi-subscribe.md) | 同一工作区单条 bridge 多订阅，配 LRU 上限 8 | 已接受 | 2026-10-07 |
| [0005](./0005-pure-jvm-codec-verification.md) | 协议编解码器保持纯 JVM，用独立脚本验证而非 app 内测试 | 已接受 | 2026-10-07 |
| [0006](./0006-release-debug-signing.md) | release 构建使用 debug 签名，不经应用商店分发 | 已接受 | 2026-10-07 |

## 维护规则

- ADR 只增不改：决策变更时新增一条，把旧的状态改为"被 NNNN 取代"。
- 新增 ADR 后在本表登记。
- 编号从 0001 递增，永不复用已删除的编号。
- 标注「补记」的条目系事后根据现状推断整理，理由待维护者确认。
