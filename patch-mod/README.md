# VH3 Translation Patch

针对 Vault Hunters 3rd Edition 的独立方法级翻译补丁原型。modid 为 `vh3_translation_patch`，Java 包名前缀为 `com.dricetea.vh3patch`。

当前只接管结算界面的 `CombatStatsContainerElement.formatMobName(String)`。已有 VP 继续承担其他翻译。工程可构建并已通过离线测试；尚未在完整整合包内启动或验证加载顺序，不应当作已验收的发布版本。

## 固定基线

| 项目 | 版本 |
|---|---|
| Minecraft / Java | 1.18.2 / JDK 17 |
| Forge | 40.3.11 |
| 整合包 / 核心 mod | 3.21.7 / 1.18.2-3.21.6.6884 |
| 补丁原型 | 0.1.0 |
| Gradle / ForgeGradle | 8.8 / 6.0.54 |
| ModLauncher / ASM | 9.1.3 / 9.7.1 |

完整核心 JAR SHA-256 和目标方法摘要存于 `transformer/src/main/resources/vh3-patch.properties`。构建默认只读引用工作区的 `origin-3.21.7/the_vault-1.18.2-3.21.6.6884.jar`，不打包或改写该 JAR。

## 工程结构

| 目录 | 职责 |
|---|---|
| `transformer/` | ModLauncher 早期转换服务、版本检查、ASM 转换、离线检查及 VP 迁移工具 |
| `runtime/` | 普通 Forge mod、客户端注册表与语言系统辅助类、语言资源 |
| `translations/` | 旧译名导入记录和未解决的 ID 对应关系 |
| `gradle/wrapper/` | 来自 Forge 官方 MDK 的 Gradle Wrapper |

早期服务与普通 mod 分为两个 JAR，因为 Forge 40.3.11 会把发现的早期服务 JAR 排除在普通 mods 扫描之外。两侧不共享 Java 包；转换器只持有类名字符串，不加载 Minecraft 或 the_vault 类。运行侧类与语言资源由普通 Forge mod 加载。

## 构建

在本目录执行 PowerShell：

```powershell
.\build.ps1
```

脚本使用现有 JDK 17，临时设置当前进程环境，结束后恢复。若无法自动找到 JDK，可指定：

```powershell
.\build.ps1 -JavaHome '你的 JDK 17 目录'
```

也可自行把 `JAVA_HOME` 指向 JDK 17 后运行 `gradlew.bat build`。首次构建需要联网获取 Forge、Minecraft 与测试依赖。源码及 JSON 显式使用 UTF-8；不要给 Gradle 守护进程强设 `-Dfile.encoding=UTF-8`：在 GBK 系统的 Java 17 启动器上，含中文路径的参数文件会因此解析错误。

构建产物在 `build/distribution/`：

- `vh3_translation_patch-transformer-0.1.0.jar`
- `vh3_translation_patch-0.1.0.jar`
- `compat/config/vaultpatcher_asm/the_vault-asm_ulti.json`
- `compat/vp-migration-report.json`

`build` 包括测试、目标 JAR 哈希与方法摘要检查、转换后字节码分析和发布映射处理；不会启动游戏、安装文件或提交 Git。

可单独运行：

```powershell
.\build.ps1 -GradleArgs ':transformer:verifyTarget','--console=plain'
.\build.ps1 -GradleArgs ':transformer:prepareVpConfig','--console=plain'
.\build.ps1 -GradleArgs 'test','--console=plain'
```

目标 JAR 不在默认位置时，可添加 `-PvaultJar=<文件位置>`。该参数仅改变输入位置，不解除版本或摘要校验。`inspectTarget` 只报告候选摘要；不要未经审查就把新摘要写入清单。

## 补丁行为

保留原方法的全部算法，在两个返回位置插入 `MobNameResolver.translate(完整ID, 原结果)`。只改变返回的显示文字，不触碰统计、经验、业务 ID 或存档。保持原栈帧，仅在必要时提高最大栈深度。

解析顺序：检查实体真实注册 → 专用 ID 覆盖 → 实体自身语言键 → 原方法返回值。未知或不合法 ID 不读取注册表默认实体，也不套用猜测译名。每次查询当前语言资源，没有跨资源重载缓存。

已从 237 个既有 VP 词条导入 235 个 ID 覆盖，其中 213 个对应核心 mod 的实体语言键，22 个对应原版实体。保留“战斗牛”和“战斗牛首领”的差异。`en_us.json` 留空，使英文及其他语言使用实体语言键或原结果，不强制套用中文。

旧名称 `Black Widow Spider`（黑寡妇蜘蛛）、`Mummy`（木乃伊）尚无已确认的 ID；原词条与全部 237 条导入来源保存在 `translations/mob-name-import.json`，没有猜测注册名。如后续确认它们对应当前实体，再明确补入覆盖。

## VP 共存与首次联测

生成的兼容文件只删除由本补丁接管的一个 `formatMobName` 规则组；其他 JSON 规则语义保持一致。现有 `program/` 配置不被修改。

后续在独立测试副本中联测时：

1. 同时放入两个同版本 JAR。
2. 备份测试副本的 `the_vault-asm_ulti.json`，对照迁移报告，再使用生成的兼容文件。若测试副本配置与当前工程不同，应先合并差异，不直接覆盖。
3. 处理 VP 旧缓存。首次测试建议在测试副本的 `config/vaultpatcher_asm/config.json` 中设置 `debug_mode.use_cache=false`；工程和真实实例的配置不会由本构建脚本自动更改。
4. 检查启动日志的 preflight 消息和目标类加载时的 applied 消息，验证结算页、语言切换与资源重载。
5. 需要回退时，移除两个 JAR 并恢复旧 VP 配置，重新处理缓存。

按用户决定，发现不匹配时抛出明确错误，不静默禁用补丁。启动检查覆盖核心 JAR、运行侧配套版本与资源、目标方法原形和旧 VP 配置冲突；实际转换时再次检查摘要和重复注入。其他转换器造成的后续冲突可能到目标类加载时才显现，仍需整合包联测。

本轮支持生产客户端 `forgeclient`。专用服务端不注册此显示补丁，mod 入口不引用客户端类。开发启动目标尚未接入；当前未提供 `runClient` / `runServer` 配置，也没有声明已完成服务端启动验证。

## 验证与下一步

离线测试覆盖真实目标方法、调试信息归一化、非目标方法不变、错误哈希/签名/重复应用拒绝、实际方法执行、VP 规则迁移、配套 JAR 预检，以及译名优先级、未知 ID 回退和无缓存查询。

检查报告和转换后 class 在 `transformer/build/verification/`。测试报告在各子项目 `build/reports/tests/test/`。

下一步仍是完整整合包联测，确认服务层与游戏层加载、VP/其他转换器顺序以及缓存行为；通过后再继续经验提示和声音名称。后续任务不会仅因列在此处自动执行。

参考：[Forge 1.18 开发文档](https://docs.minecraftforge.net/en/1.18.x/gettingstarted/)、[Forge 40.3.11 官方 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.3.11/forge-1.18.2-40.3.11-mdk.zip)、[ModLauncher 9.1.3 源码包](https://maven.minecraftforge.net/cpw/mods/modlauncher/9.1.3/modlauncher-9.1.3-sources.jar)。
