# 0006. release 构建使用 debug 签名，不经应用商店分发

- 状态：已接受
- 日期：2026-10-07

## 背景（Context）

项目是非官方逆向客户端，不可能上架 Google Play 等商店（协议与商标均有风险，
README 免责声明已明确）。分发方式是 GitHub 下载页 + 社区自取 APK，
受众是愿意侧载（sideload）的用户。

## 决策（Decision）

`app/build.gradle.kts` 的 release 构建类型：`isMinifyEnabled = true` +
`isShrinkResources = true`（R8 全量优化与资源收缩），但
`signingConfig = signingConfigs.getByName("debug")`——用 debug 签名，
产物可直接安装，无需用户配置任何签名信任。
体积策略一并固化：只打包 arm64-v8a、资源只保留 zh/en。

## 备选方案（Alternatives）

- **正式自签（release keystore）**：需要保管密钥、用户侧升级时签名必须一致；
  对侧载分发没有实际收益（不提供 Play 保护性校验）。
- **Play / 其它商店分发**：非官方逆向客户端无法过审，直接排除。

## 后果（Consequences）

- 好处：任何人 clone 后一条 `./gradlew assembleRelease` 即得到可分发 APK；
  R8 保证体积与性能。
- 代价：
  - debug 签名意味着无发布者身份背书，用户需自行信任来源（与项目定位一致）；
  - 升级覆盖安装要求所有版本统一用 debug 签名，**中途更换签名会导致用户必须卸载重装**
    （本地 DataStore 数据随之丢失）。
  - `REQUEST_INSTALL_PACKAGES` 权限保留在 Manifest 中，而应用内"检查更新"功能
    已在 v1.1.1 移除（git 2ca044f）——权限现状与功能现状不一致（⚠️ 待确认）。
- 推翻条件：若转为 F-Droid 等合规渠道分发，需要正式签名与 reproducible build。
