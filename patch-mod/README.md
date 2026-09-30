# VH3 Translation Patch

针对 Vault Hunters 3rd Edition 的独立方法级翻译补丁原型。modid 为 `vh3_translation_patch`，Java 包名前缀为 `com.dricetea.vh3patch`。

当前接管结算怪物名称 `CombatStatsContainerElement.formatMobName(String)` 和声音设置名称 `VaultSoundOptionsScreen.formatSoundName(String)`，分别由 `combat_stats`、`sound_names` 模块负责。已有 VP 继续承担其他翻译。工程可构建并已通过离线测试；尚未在完整整合包内启动或验证加载顺序，不应当作已验收的发布版本。

## 固定基线

| 项目 | 版本 |
|---|---|
| Minecraft / Java | 1.18.2 / JDK 17 |
| Forge | 40.3.11 |
| 整合包 / 核心 mod | 3.21.7 / 1.18.2-3.21.6.6884 |
| 补丁版本 | 1.0.3 |
| Gradle / ForgeGradle | 8.8 / 6.0.54 |
| ModLauncher / ASM | 9.1.3 / 9.7.1 |

完整核心 JAR SHA-256 和目标方法摘要按模块存于 `transformer/src/main/resources/patches/<模块名>.properties`。构建默认只读引用工作区的 `origin-3.21.7/the_vault-1.18.2-3.21.6.6884.jar`，不打包或改写该 JAR。

## 工程结构

| 目录 | 职责 |
|---|---|
| `transformer/` | ModLauncher 早期转换服务、版本检查、ASM 转换、离线检查及 VP 迁移工具 |
| `runtime/` | 普通 Forge mod、独立功能模块、外部配置读取与重载 |
| `translations/` | 旧译名导入记录和未解决的 ID 对应关系 |
| `gradle/wrapper/` | 来自 Forge 官方 MDK 的 Gradle Wrapper |

早期服务与普通 mod 分为两个 JAR，因为 Forge 40.3.11 会把发现的早期服务 JAR 排除在普通 mods 扫描之外。两侧不共享 Java 包；转换器只持有类名字符串，不加载 Minecraft 或 the_vault 类。运行侧模块由普通 Forge mod 加载。每一侧均将具体功能放入独立的 `modules` 包，一个类代表一个模块，公共层统一注册、校验和加载配置。

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

- `vh3_translation_patch-transformer-1.0.3.jar`
- `vh3_translation_patch-1.0.3.jar`
- `compat/config/vaultpatcher_asm/the_vault-asm_complex.json`
- `compat/vp-migration-report.json`

`build` 包括测试、目标 JAR 哈希与方法摘要检查、转换后字节码分析和发布映射处理；不会启动游戏、修改游戏实例或提交 Git。

完整 `build` 通过后会执行 `exportToProgram`，将两个同版本 JAR 复制到 `../program/基础+硬编码汉化/mods/`，逐字节验证复制结果，并仅删除此目录中本补丁的旧版 JAR。其他模组文件不受影响。JAR 继续按仓库现有规则忽略，不自动纳入 Git。

版本号在 `gradle.properties` 的 `mod_version` 中维护，采用 `1.x.x`，起始版本为 `1.0.1`。普通修改为 `1.0.1 → 1.0.2 → 1.0.3`；依赖的 the_vault 核心版本更新时小版本号和修订号均加一，例如 `1.0.3 → 1.1.4`，**修订号始终不重置**。编译重试或对同一份内容重复打包不重复递增。`check`、`test` 和 `distribution` 本身不复制到工程，完整 `build` 或单独 `exportToProgram` 才会执行验证后的复制。

普通修改完成后执行 `.\build.ps1 -BumpPatch`，将修订号加一并构建、导出。核心版本适配需先审查并更新目标清单和依赖，再执行 `.\build.ps1 -BumpCore`，同时将小版本号和修订号加一。脚本不自动猜测上游版本变化。同一轮失败重试用 `.\build.ps1`，不要再次带递增参数；重建当前已编号版本时直接构建即可。

可单独运行：

```powershell
.\build.ps1 -GradleArgs ':transformer:verifyTarget','--console=plain'
.\build.ps1 -GradleArgs ':transformer:prepareVpConfig','--console=plain'
.\build.ps1 -GradleArgs 'test','--console=plain'
```

目标 JAR 不在默认位置时，可添加 `-PvaultJar=<文件位置>`。该参数仅改变输入位置，不解除版本或摘要校验。`inspectTarget` 只报告候选摘要；不要未经审查就把新摘要写入清单。

## 结算怪物名称

保留原方法的全部算法，在两个返回位置插入运行侧 `CombatStatsModule.translate(完整ID, 原结果)`。只改变返回的显示文字，不触碰统计、经验、业务 ID 或存档。保持原栈帧，仅在必要时提高最大栈深度。

解析顺序：取输入 ID 的路径 → 本模块配置 → 已注册实体自身语言键 → 原方法返回值。配置命中时直接使用译文，因此也支持手动添加的 ID；未配置的未知 ID 不读取注册表默认实体。只有后备的实体译名跟随游戏当前语言，模块配置不区分语言、不会全局覆盖语言键。

已从 237 个既有 VP 词条导入 235 个映射，其中 213 个来源 ID 由核心 mod 的实体语言文件核对，22 个对应原版实体。保留“战斗牛”和“战斗牛首领”的差异。唯一的运行时配置源为 `../program/基础+硬编码汉化/config/vh3_translation_patch/combat_stats.json`；源码资源与 JAR 不保留配置副本。

旧名称 `Black Widow Spider`（黑寡妇蜘蛛）、`Mummy`（木乃伊）尚无已确认的 ID；原词条与全部 237 条导入来源保存在 `translations/mob-name-import.json`，没有猜测注册名。如后续确认它们对应当前实体，再明确补入覆盖。

## 声音设置名称

`sound_names` 接管 `VaultSoundOptionsScreen.formatSoundName(String)` 的唯一返回点，保留原方法算法，把原始参数与原显示名交给 `SoundNamesModule.translate`。原始参数来自 `ModSounds` 的 Java 字段名，例如 `GRASSHOPPER_BRRR`、`RAFFLE_SFX`、`VAULT_AMBIENT_LOOP`；它不是声音资源 ID，也不是格式化后的英文名称。

初始迁移的 203 条译文全部与真实字段唯一匹配。唯一的运行时配置源为 `../program/基础+硬编码汉化/config/vh3_translation_patch/sound_names.json`，安装时一并复制到游戏的同名配置位置：

```json
{
  "GRASSHOPPER_BRRR": "蚱蜢：咕咕",
  "RAFFLE_SFX": "速通音效",
  "VAULT_AMBIENT_LOOP": "宝库环境循环"
}
```

查找保留大小写和下划线，未配置时使用原方法结果。只改名称，不修改声音 ID、音量、播放或持久化逻辑。原界面依照显示名搜索和排序，因此沿用中文显示名搜索和排序。

**修改后按 F3+T，再关闭并重新打开声音设置。** 名称列表在界面构造时缓存，本轮不额外接管已打开界面的即时刷新。导入依据及原英文词条见 `translations/sound-name-import.json`。

## 修改模块配置

两个模块的条目都只需编辑外部 JSON，无需修改源码、重编译或更换 JAR。正式配置统一维护在 `../program/基础+硬编码汉化/config/vh3_translation_patch/`，安装时随汉化工程复制到游戏的 `config/vh3_translation_patch/`。运行中的游戏读取游戏目录里的文件，修改工程文件不会自动同步到已安装的实例。VTP 不生成、不补齐、不覆盖配置，也不在源码资源或导出的 JAR 中保留内置译文。文件使用 UTF-8 标准 JSON，例如结算模块：

```json
{
  "aggressive_cow": "战斗牛",
  "aggressive_cow_boss": "战斗牛首领"
}
```

编辑后按 **F3+T** 重载资源即可重新读取，无需重新编译 JAR。配置不按语言拆分，切换游戏语言不会关闭这些映射。这个模块取冒号后的路径查表，例如 `the_vault:aggressive_cow` 与其他命名空间的同名路径都会使用 `aggressive_cow`；它不是全局 `entity.…` 语言资源。

重载时若文件格式错误、键重复或值不是字符串，日志会标出具体文件和错误，并保留该模块上次有效的配置；首次读取时缺失或损坏配置会抛错并阻止启动，错误消息包含需要修复的文件路径。其他模块仍可正常重载。修正文件后再次按 F3+T。解析完全成功才整体替换快照，不会出现半份配置生效的情况。

删除一条映射会启用该条目的后备译名；`{}` 表示没有专用映射；空字符串是显式的空译文。删除整个文件后，F3+T 会报错并保留旧快照；下一次启动则会失败，必须先恢复文件。JSON 不支持注释。

后续模块默认直接使用英文原文作为键，例如 `"Aggressive Cow": "战斗牛"`。结算怪物名使用原始 ID 路径，声音名称使用原始 Java 字段名；它们是按各自输入形式查表的模块。模块文件名由模块类中的 ID 决定。给现有模块增加词条只改 JSON；增加新的接管方法或新的模块仍需编写源码，见 [模块开发说明](docs/modules.md)。原始输入、上游处理过程和字节码插入位置见 [文本获取分析](docs/text-capture.md)。

## VP 共存与首次联测

`program/` 中原有的 `formatMobName` 规则已经替换为 `_comment`，原位置说明结算怪物名称由 VTP 的 `combat_stats` 模块接管。其他规则保留。旧规则的 237 对词条另存于 `translations/vp/combat_stats.json`。声音名称的 203 对旧规则也已替换为原位置的 VTP 接管注释，原规则另存于 `translations/vp/sound_names.json`；同类 6 条普通界面文案仍由 VP 处理。这些旧规则仅供导入和冲突回归测试，不发布到 VP 配置目录。

兼容配置生成工具现在支持已迁移输入：没有接管规则时原样复制（含接管注释），有一组时移除，存在重复组仍报错。构建不再反复改写 `program/`；当前迁移报告的 `removedGroups` 为 0 是正常情况。

后续在独立测试副本中联测时：

1. 同时放入两个同版本 JAR，并复制工程 `config/vh3_translation_patch/` 下的两个 JSON。升级旧安装前保留并合并已有的自定义配置。
2. 使用当前工程已移除接管规则的 main/long/complex 三份配置及对应 config.json，或使用构建生成的兼容文件。备份测试副本中的旧配置后再合并更新。旧 ulti 布局应先按 `docs/maintenance/vp-asm-layout.md` 完成重组；若测试副本配置与当前工程不同，应先合并差异，不直接覆盖。
3. 处理 VP 旧缓存。首次测试建议在测试副本的 `config/vaultpatcher_asm/config.json` 中设置 `debug_mode.use_cache=false`；工程和真实实例的配置不会由本构建脚本自动更改。
4. 检查启动日志的 preflight 消息和目标类加载时的 applied 消息，验证结算页，以及编辑配置后 F3+T 重载、错误配置保留旧值的行为。
5. 需要回退时，移除两个 JAR 并恢复旧 VP 配置，重新处理缓存。

按用户决定，发现不匹配时抛出明确错误，不静默禁用补丁。启动检查覆盖核心 JAR、运行侧配套版本与 mod 元数据、目标方法原形和旧 VP 配置冲突；实际转换时再次检查摘要和重复注入。其他转换器造成的后续冲突可能到目标类加载时才显现，仍需整合包联测。

本轮支持生产客户端 `forgeclient`。专用服务端不注册此显示补丁，mod 入口不引用客户端类。开发启动目标尚未接入；当前未提供 `runClient` / `runServer` 配置，也没有声明已完成服务端启动验证。

## 验证与下一步

离线测试覆盖真实目标方法、调试信息归一化、非目标方法不变、错误哈希/签名/重复应用拒绝、实际方法执行、VP 规则迁移、配套 JAR 预检，以及模块映射、外部配置加载、任意新增条目、首次缺失/损坏拒绝启动、编辑重载、错误保留旧值、严格 JSON 校验和模块隔离。真实游戏的 F3+T 事件、界面显示和加载顺序仍需联测。

检查报告和转换后 class 在 `transformer/build/verification/`。测试报告在各子项目 `build/reports/tests/test/`。

下一步仍是完整整合包联测，确认服务层与游戏层加载、VP/其他转换器顺序以及缓存行为；后续再按用户要求推进经验提示等模块。后续任务不会仅因列在此处自动执行。

参考：[Forge 1.18 开发文档](https://docs.minecraftforge.net/en/1.18.x/gettingstarted/)、[Forge 40.3.11 官方 MDK](https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.3.11/forge-1.18.2-40.3.11-mdk.zip)、[ModLauncher 9.1.3 源码包](https://maven.minecraftforge.net/cpw/mods/modlauncher/9.1.3/modlauncher-9.1.3-sources.jar)。
