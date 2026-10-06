# 0005. 协议编解码器保持纯 JVM，用独立脚本验证而非 app 内测试

- 状态：已接受
- 日期：2026-10-07

## 背景（Context）

IPC 编解码（IpcCodec）与 CRC32 是每一帧双向必经的热路径，正确性要求极高；
但 Android 项目的单元测试通常要配 androidTest / 模拟器 / Robolectric，
为几十个断言引入整套测试依赖不划算。仓库的 `app/src` 下只有 `main` 源集，
没有任何 test source set。

## 决策（Decision）

- `IpcCodec.kt` 刻意写成**纯 JVM**（只依赖 kotlin-stdlib + gson，无任何 Android API，
  文件头注释声明了这一约束与性能约定）。
- 正确性用 `tools/verify-ipc-codec/run.sh` 验证：脚本直接用 Gradle 缓存里的
  kotlin-compiler-embeddable 把 IpcCodec.kt 编译成 class 运行断言，
  不需要模拟器、不需要给 app 模块引测试依赖、不需要联网。
- 另有 `tools/check_16kb_alignment.py`：自包含检查 APK/AAR 原生库 16KB 页对齐
  （解析 ELF，不依赖 NDK）。
- 常规改动验证以 `./gradlew :app:compileDebugKotlin` 为标准（CHANGELOG 中亦如此记载）。

## 备选方案（Alternatives）

- **标准 JUnit 单元测试模块**：能力等价但引入测试框架与配置；且历史从未有过 test 源集。
- **仅靠真机手测**：编解码回归风险高，热路径 bug（分片、varint、大帧）手测难覆盖。

## 后果（Consequences）

- 好处：零依赖快速验证（一条 bash 命令），CI/本地都能跑；代码约束本身还带来了
  性能红利（无锁缓冲、避免逐字节分配，见 IpcCodec.kt 注释）。
- 代价：只有 IpcCodec 享受这种验证；RelayClient / ConversationV4 等含 Android/协程
  依赖的层**没有自动化回归**，改动只能靠编译 + 真机。
- 推翻条件：一旦给仓库引入正经的 test 源集，本脚本可并入单元测试。
