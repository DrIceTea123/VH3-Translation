# 开发与验证

## VTP 构建

在 `patch-mod/` 使用 JDK 17：

```powershell
./build.ps1                   # 当前版本完整构建与导出
./build.ps1 -BumpPatch        # 普通 mod 修改：修订号 +1
./build.ps1 -BumpCore         # 核心适配：小版本和修订号都 +1
```

例如 `1.0.19 → 1.1.20`，修订号不重置。同轮失败重试不重复递增。仅配置/文档修改不递增 VTP；修改 mod 后须完整验证并导出。

`build` 包括 Java 测试、固定核心 SHA-256、方法指纹/命中数、ASM 分析、单包结构与哈希验证。导出到 `translate-packs/mods/`，逐字节检查后只删除旧 VTP。`check` / `distribution` 不导出；`exportToProgram` 是保留的任务名，实际目标已迁到 translate-packs。

测试依赖 VP 1.5.3-hotfix，可从仓库根运行 `tools/vp/fetch-reference.ps1` 获取并验哈希到忽略的 `local-deps/`。原始核心默认读取父工作区 `origin-3.21.7/the_vault-1.18.2-3.21.6.6884.jar`；`-PvaultJar=<文件>` 可改变输入位置，不能绕过哈希。

已有依赖缓存时可离线构建：

```powershell
./build.ps1 -GradleArgs 'build','--offline','-Dnet.minecraftforge.gradle.check.certs=false','--console=plain'
```

不要将证书检查参数持久化，不对 Windows JDK 17 Gradle 启动器强设 UTF-8 参数文件编码；源码及 JSON 本身使用 UTF-8。

## 源码与复核入口

- [模块架构](../patch-mod/docs/modules.md)：runtime/transformer 分工、新增目标、VP 接管。
- [单 JAR 加载](../patch-mod/docs/single-jar.md)：SERVICE/GAME 分层、专用定位器、内嵌缓存。
- [模块索引](vtp.md)：每个显示领域的原始输入、调用边界和配置说明。
- `:transformer:inspectTarget` / `:transformer:verifyTarget`：检查固定真实核心的目标方法。
- `python tools/source-audit.py [模块]`（在 patch-mod 运行）：比对 9 个后增模块的原始文本及 class 摘要；只有审阅后才用 `--record` 更新来源基线。
- `python tools/assessment/verify-bundle.py`：使用本机固定 Forge/ASM 缓存执行真实加载层探针，先完成 build。

构建报告在 runtime/transformer 的 `build/reports/tests/test/`；目标清单结果在 `transformer/build/verification/report.json`；单包探针结果在 `build/bundle-verification/report.json`。安装器独立构建，见 [安装器维护](installer.md)。

## 验收边界

离线方法执行验证逻辑，加载层探针验证 SPI、Forge 元数据和隔离 GAME 类加载；两者都不执行完整 Minecraft 生命周期。实际游戏仍须确认图鉴详情返回/直接打开、词缀、缓存与 F3+T、专用服务端、VP 和其他转换器共存。

新的目标摘要必须来自审阅后的真实方法，不能为了通过构建改摘要。新增测试应验证业务行为或失败边界，不固定用户可编辑译文。未确认的新名称保留英文，不自行扩展到其他模块。Git 提交仅限用户明确要求。
