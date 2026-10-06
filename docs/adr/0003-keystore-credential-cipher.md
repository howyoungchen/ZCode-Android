# 0003. 设备凭据用 Android Keystore AES/GCM 加密存储

- 状态：已接受
- 日期：2026-10-07

## 背景（Context）

配对 URL 中的 `sid` / `hash` 等同于设备凭据，泄露即等于交出桌面端控制权
（README 免责声明）。App 又承诺"不收集任何数据，凭据只存在手机本地"。
设备列表需要持久化（DataStore），不能明文落盘。

## 决策（Decision）

`CredentialCipher`（state/CredentialCipher.kt）用 Android Keystore 生成不可导出的
AES-256 密钥（别名 `zemote_key`，GCM/NoPadding），加密后的 URL 以
`enc:` + base64url(IV‖密文) 形式存入 DataStore；`enc:` 前缀用于区分历史明文数据，
读取时兼容两种格式（AccountStore.kt）。

## 备选方案（Alternatives）

- **明文存 DataStore**：root / 备份场景下直接暴露凭据，违反安全承诺。
- **EncryptedSharedPreferences（jetpack security）**：引一个新依赖，且本质同样是
  Keystore+AES/GCM，自己实现只需 ~80 行并完全可控。
- **不保存设备，每次重新扫码**：多设备切换场景体验差（README 把"保存多台设备"列为功能）。

## 后果（Consequences）

- 好处：密钥不出安全硬件，导出 DataStore 文件拿不到明文凭据。
- 代价与已知边界（代码注释明确设计）：
  - Keystore 不可用的设备上，`encrypt()` 返回 null，**降级为明文存储**（不崩溃优先）；
  - 密钥丢失（如恢复备份到新机）时解密失败，对应账号条目被**静默跳过**；
  - 云备份规则只包含 sharedpref 域，DataStore 不上云（见 data-model.md §5）。
- 推翻条件：若未来引入端到端账号同步，需重新设计凭据派生与迁移方案。
