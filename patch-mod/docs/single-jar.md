# VTP 1.0.16 单 JAR 交付

2026-10-04，用户批准合并评估的推荐方案并授权实现、版本递增和 Git 提交。1.0.16 只分发 `vh3_translation_patch-1.0.16.jar`，源码仍分 runtime 与 transformer；14 个翻译模块、外部配置、105 个目标方法和 PRE_CLASS 策略保持不变。前轮 1.0.15 的 VP 启用文件范围修复包含在本版。

## 实现

- 外层是 SERVICE 层的转换器，同时注册 `ITransformationService` 和 `IModLocator`。`EmbeddedRuntimeLocator` 继承固定 Forge 40.3.11 的 `AbstractJarFileModLocator`，把已校验运行模块交给 Forge。
- `runtime:reobfJar` 完成后，构建将其逐字节嵌入 `META-INF/vh3_translation_patch/runtime.jar`，并生成版本与 SHA-256 元数据。外层不含普通 mod 的 `mods.toml` 或运行侧类；内层保留完整 Forge mod 结构。
- `EmbeddedRuntime.resolve` 是两入口共同的提取实现，不依赖转换服务和定位器调用先后。安装目录必须恰好有一个合并包，不能残留旧独立运行侧/转换侧 JAR；按内容识别，重命名也不能绕过预检。重复服务模块也可能先被加载器拒绝。
- 每次读取内嵌原件并校验哈希，缓存放在游戏目录 `.vh3_translation_patch/runtime/<版本>-<完整 SHA-256>/runtime.jar`。缺失或损坏时，在同目录写临时文件、校验元数据后原子替换；有效缓存不重写。不能创建缓存、不支持原子移动或文件被占用时报错并显示缓存路径。
- 旧版本缓存不自动删除，避免影响其他进程，也便于回退。它不是安装输入；关闭游戏后可清理整个 `.vh3_translation_patch`，下次启动重建。不要将缓存 JAR 复制到 mods。
- 原有核心/方法指纹、VP 冲突检查和运行侧 ready/version 握手保留。preflight 改为校验内嵌运行侧版本、mod 元数据与各模块 helper，不再要求 mods 中存在第二个 JAR。

## 安装、升级与回退

客户端和专用服务器使用相同的单 JAR；外部配置与 VP 配置要求见 [README](../README.md)。升级前关闭游戏，删除 mods 中旧的 `vh3_translation_patch-<版本>.jar`、`vh3_translation_patch-transformer-<版本>.jar`，再放入新包。保留自定义翻译配置。

工程 `build` / `exportToProgram` 在验证通过后复制单个成品，逐字节检查后仅清理本补丁旧 JAR。`.gitignore` 只放行新单 JAR 安装输入。`program/package.iss` 新增 basic 组件的两条 VTP 专属清理规则，原脚本编码保留；安装时先清理旧命名文件，再安装当前包。[Inno Setup 的 InstallDelete](https://jrsoftware.org/ishelp/topic_installdeletesection.htm) 在文件安装之前执行。手动覆盖版仍须按上面的步骤移除旧 JAR；被自行改名的旧文件需人工删除。

回退到 1.0.15：移除新单 JAR，放回同版本的旧 runtime 和 transformer 两个文件，不混装。缓存无需参与回退。更早版本还需匹配当时配置及 VP 接管范围。

## 验证

完整离线 build：211 项 Java 测试（runtime 59、transformer 152），零失败、错误或跳过；固定核心 105 个目标方法的哈希、指纹和字节码校验通过。新增 12 项缓存/安装布局测试覆盖缺失、损坏恢复、重复包、重命名旧包、内外版本和哈希不符、元数据缺失、缓存创建失败、升级及回退。

构建新增 `verifyBundle`，检查两个 SPI 入口、内外版本、内层 SHA-256、与 reobf 成品逐字节一致、运行侧类隔离，以及没有内置 JSON 译表、历史 translations 或测试类。工程 mods 成品与 distribution 一致。

`tools/assessment/verify-bundle.py` 对正式成品使用实际 Forge 扫描器、`JarModuleFinder` 和 `ModuleClassLoader`：

1. 外层被早期扫描识别，两个生产 SPI 提供者通过 SERVICE 层的 ServiceLoader 实例化。
2. 先调用定位器、后初始化转换服务，Forge 正确识别内层 modId/version。
3. 对固定真实核心和当前 VP 配置执行客户端、服务端初始化：分别注册 14/8 模块、85/41 类转换目标，ready 版本匹配。
4. 内层 `TranslationPatchMod` 类在独立 GAME 层加载，SERVICE 层不能加载该运行类。

探针依赖 JDK 17 和脚本中列出的本机固定依赖缓存，输出在 `build/bundle-verification`；固定结果见 [证据](evidence/single-jar-1.0.16.json)。复现：在 patch-mod 运行 `python tools/assessment/verify-bundle.py`（先完成 build）。历史双 JAR 评估脚本改为显式接收旧文件，不误把新包当作旧运行侧。

**验证边界：没有启动 Minecraft，没有实例化 @Mod，没有执行事件生命周期或游戏内 F3+T，也没有启动真实专用服务器。** 探针补充了 FML 版本与环境参数夹具，不能替代整个 ModLauncher/FML 启动过程。不可写场景测试使用阻挡缓存目录创建的文件，没有修改系统 ACL。安装器脚本只做静态范围和编码检查，未构建或运行安装器。此前 3 项来源读取测试沿用原结果，本次不涉及译表变更。完整整合包联测仍待完成。
